package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Signed-upgrade photo and release audit tests use ONLY synthetic member data. */
@RunWith(AndroidJUnit4.class)
public final class V28PhotosAndReadinessTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String DB="badminton_room_v18.db";

 @Test public void verifyPhotoAndRecoveryAuditAfterSignedUpgrade()throws Exception {
  Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
  LedgerDatabase db=Room.databaseBuilder(ctx,LedgerDatabase.class,DB)
   .allowMainThreadQueries().build();
  File source=null;
  boolean syntheticVerified=false;
  try{
   assertNotNull("Upgrade fixture ledger exists",db.dao().meta());
   assertTrue("Never run photograph mutations against a real club ledger",
    db.dao().meta().club.startsWith("CI Synthetic Badminton Club"));
   boolean found=false;
   for(Member m:db.dao().members())if(MEMBER.equals(m.id)){found=true;break;}
   assertTrue("Photo must be bound to existing original member ID",found);
   syntheticVerified=true;
   String baseline=MigrationCodec.canonical(
    MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
   assertTrue("SHA member key uses stable ID",
    PremiumV28MemberPhotos.key(MEMBER).matches("member-[0-9a-f]{64}\\.jpg"));
   assertNotEquals("Other member cannot collide",
    PremiumV28MemberPhotos.key(MEMBER),PremiumV28MemberPhotos.key(MEMBER+"-different"));
   File dir=new File(ctx.getCacheDir(),"pdfs");
   assertTrue(dir.isDirectory()||dir.mkdirs());
   source=new File(dir,"ci-member-photo-"+UUID.randomUUID()+".jpg");
   Bitmap bitmap=Bitmap.createBitmap(640,450,Bitmap.Config.ARGB_8888);
   bitmap.eraseColor(Color.rgb(25,100,65));
   try(FileOutputStream output=new FileOutputStream(source)){
    assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,88,output));
   }finally{bitmap.recycle();}
   Uri uri=FileProvider.getUriForFile(ctx,ctx.getPackageName()+".files",source);
   PremiumV28MemberPhotos.remove(ctx,MEMBER);
   PremiumV28MemberPhotos.addOrReplace(ctx,uri,MEMBER);
   assertTrue("Member photo exists locally",PremiumV28MemberPhotos.has(ctx,MEMBER));
   Bitmap thumb=PremiumV28MemberPhotos.thumbnail(ctx,MEMBER);
   assertNotNull("Thumbnail decodes",thumb);
   assertEquals("Local photo width",384,thumb.getWidth());
   assertEquals("Local photo height",384,thumb.getHeight());
   thumb.recycle();
   File photo=PremiumV28MemberPhotos.file(ctx,MEMBER);
   assertTrue(photo.getCanonicalPath().startsWith(
    ctx.getFilesDir().getCanonicalPath()+File.separator));
   assertTrue("Portrait re-encoding must limit bytes",photo.length()<1_500_000);
   String release=PremiumV28ReleaseAudit.review(ctx,db);
   assertTrue("Read-only audit must report restore rehearsal",
    release.contains("LOCAL RESTORE REHEARSAL")&&release.contains("PASS"));
   assertTrue("Audit must disclose photo transfer limitation",
    release.contains("not restored")&&release.contains("FINANCIAL backups"));
   String afterAdd=MigrationCodec.canonical(
    MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
   assertEquals("Photo cannot modify financial JSON fields",baseline,afterAdd);
   verifyExifOrientations(ctx,dir);
   verifyInvalidReplacementPreservesPhoto(ctx,dir);
   assertEquals("Orientation and invalid-input checks preserve every ledger field",baseline,
    MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger));
   // Verify overwrite is independent of ledger and reversible.
   PremiumV28MemberPhotos.addOrReplace(ctx,uri,MEMBER);
   PremiumV28MemberPhotos.remove(ctx,MEMBER);
   assertFalse("Removing member portrait must remove only local image",
    PremiumV28MemberPhotos.has(ctx,MEMBER));
   String afterRemoval=MigrationCodec.canonical(
    MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
   assertEquals("Removing photograph leaves all original records intact",baseline,afterRemoval);
   assertEquals("Synthetic original roster preserved",15,db.dao().members().size());
   System.out.println("V28_PHOTO_AND_FINANCIAL_AUDIT_PASS");
  }finally{
   if(syntheticVerified)PremiumV28MemberPhotos.remove(ctx,MEMBER);
   if(source!=null)source.delete();
   db.close();
  }
 }

 private static void verifyExifOrientations(Context ctx,File directory)throws Exception {
  // Independent expected corner mapping, ordered top-left, top-right,
  // bottom-left, bottom-right. This checks final decoded JPEG pixels rather
  // than reusing the production Matrix operations to predict the answer.
  int[] colors={Color.rgb(224,48,48),Color.rgb(35,175,75),
   Color.rgb(45,80,210),Color.rgb(235,200,35)};
  int[] orientations={ExifInterface.ORIENTATION_NORMAL,
   ExifInterface.ORIENTATION_FLIP_HORIZONTAL,ExifInterface.ORIENTATION_ROTATE_180,
   ExifInterface.ORIENTATION_FLIP_VERTICAL,ExifInterface.ORIENTATION_TRANSPOSE,
   ExifInterface.ORIENTATION_ROTATE_90,ExifInterface.ORIENTATION_TRANSVERSE,
   ExifInterface.ORIENTATION_ROTATE_270};
  int[][] expected={{0,1,2,3},{1,0,3,2},{3,2,1,0},{2,3,0,1},
   {0,2,1,3},{2,0,3,1},{3,1,2,0},{1,3,0,2}};
  for(int i=0;i<orientations.length;i++){
   File fixture=File.createTempFile("ci-exif-photo-",".jpg",directory);
   Bitmap bitmap=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);
   try{
    Canvas canvas=new Canvas(bitmap);Paint paint=new Paint();
    for(int corner=0;corner<4;corner++){
     paint.setColor(colors[corner]);
     int left=(corner%2)*320,top=(corner/2)*240;
     canvas.drawRect(left,top,left+320,top+240,paint);
    }
    try(FileOutputStream output=new FileOutputStream(fixture)){
     assertTrue("Synthetic orientation fixture encodes",
      bitmap.compress(Bitmap.CompressFormat.JPEG,96,output));
    }
    ExifInterface exif=new ExifInterface(fixture.getAbsolutePath());
    exif.setAttribute(ExifInterface.TAG_ORIENTATION,Integer.toString(orientations[i]));
    exif.setAttribute(ExifInterface.TAG_MAKE,"CI Synthetic Camera");
    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,"22/1,0/1,0/1");
    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,"N");
    exif.saveAttributes();
    assertEquals("Fixture must carry its intended EXIF orientation",orientations[i],
     new ExifInterface(fixture.getAbsolutePath()).getAttributeInt(
      ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_UNDEFINED));
    Uri uri=FileProvider.getUriForFile(ctx,ctx.getPackageName()+".files",fixture);
    PremiumV28MemberPhotos.addOrReplace(ctx,uri,MEMBER);
    Bitmap normalized=PremiumV28MemberPhotos.thumbnail(ctx,MEMBER);
    assertNotNull("Normalized orientation "+orientations[i]+" decodes",normalized);
    try{
     assertEquals(384,normalized.getWidth());assertEquals(384,normalized.getHeight());
     for(int corner=0;corner<4;corner++){
      int x=corner%2==0?96:288,y=corner/2==0?96:288;
      assertColorNear("EXIF "+orientations[i]+" corner "+corner,
       colors[expected[i][corner]],normalized.getPixel(x,y));
     }
    }finally{normalized.recycle();}
    ExifInterface saved=new ExifInterface(PremiumV28MemberPhotos.file(ctx,MEMBER).getAbsolutePath());
    int savedOrientation=saved.getAttributeInt(ExifInterface.TAG_ORIENTATION,
     ExifInterface.ORIENTATION_UNDEFINED);
    assertTrue("Orientation is baked into pixels, not left for the viewer",
     savedOrientation==ExifInterface.ORIENTATION_UNDEFINED||savedOrientation==ExifInterface.ORIENTATION_NORMAL);
    assertNull("Private camera metadata is removed",saved.getAttribute(ExifInterface.TAG_MAKE));
    assertNull("Private location metadata is removed",saved.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
   }finally{bitmap.recycle();fixture.delete();}
  }
  System.out.println("V28_PHOTO_ALL_EXIF_ORIENTATIONS_PASS");
 }

 private static void assertColorNear(String description,int expected,int actual){
  // JPEG compression and RGB_565 thumbnails can change channel values slightly.
  int tolerance=24;
  assertTrue(description+" red",Math.abs(Color.red(expected)-Color.red(actual))<=tolerance);
  assertTrue(description+" green",Math.abs(Color.green(expected)-Color.green(actual))<=tolerance);
  assertTrue(description+" blue",Math.abs(Color.blue(expected)-Color.blue(actual))<=tolerance);
 }

 private static void verifyInvalidReplacementPreservesPhoto(Context ctx,File directory)throws Exception {
  byte[] before=Files.readAllBytes(PremiumV28MemberPhotos.file(ctx,MEMBER).toPath());
  byte[][] invalidInputs={new byte[0],new byte[512]};
  for(byte[] invalid:invalidInputs){
   File fixture=File.createTempFile("ci-invalid-photo-",".jpg",directory);
   try{
    try(FileOutputStream output=new FileOutputStream(fixture)){output.write(invalid);}
    Uri uri=FileProvider.getUriForFile(ctx,ctx.getPackageName()+".files",fixture);
    try{
     PremiumV28MemberPhotos.addOrReplace(ctx,uri,MEMBER);
     fail("Invalid image must be rejected before replacing the member photograph");
    }catch(IOException expected){
     assertNotNull("Image rejection explains the problem",expected.getMessage());
    }
    assertArrayEquals("Invalid input preserves the complete original photograph",before,
     Files.readAllBytes(PremiumV28MemberPhotos.file(ctx,MEMBER).toPath()));
   }finally{fixture.delete();}
  }
  System.out.println("V28_PHOTO_INVALID_REPLACEMENT_PRESERVED_PASS");
 }
}

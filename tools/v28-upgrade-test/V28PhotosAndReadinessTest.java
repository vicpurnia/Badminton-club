package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
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
  try{
   assertNotNull("Upgrade fixture ledger exists",db.dao().meta());
   boolean found=false;
   for(Member m:db.dao().members())if(MEMBER.equals(m.id)){found=true;break;}
   assertTrue("Photo must be bound to existing original member ID",found);
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
   PremiumV28MemberPhotos.remove(ctx,MEMBER);
   if(source!=null)source.delete();
   db.close();
  }
 }
}

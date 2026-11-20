package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/**
 * Member portraits are optional, offline, and keyed to an immutable member ID.
 * No financial Room schema/ledger/verified JSON mutation or online upload.
 *
 * Source images from Android's scoped Documents picker; decode at reduced
 * resolution, center-crop and strip EXIF/location metadata during re-encoding.
 * The resulting 384px JPEG is app-private (not part of financial JSON backup).
 * A separately authenticated/photo-backup workflow is required for transfer.
 */
final class PremiumV28MemberPhotos {
 static final int SIDE=384;
 static final int MAX_SOURCE_BYTES=12_000_000;
 private PremiumV28MemberPhotos(){}

 static String key(String memberId) throws IOException {
  if(memberId==null||memberId.trim().isEmpty()||memberId.length()>256)
   throw new IOException("A valid unchanged member ID is required");
  try {
   byte[] digest=MessageDigest.getInstance("SHA-256").digest(
    memberId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder out=new StringBuilder("member-");
   for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
   return out.append(".jpg").toString();
  }catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
 }

 static File directory(Context ctx)throws IOException {
  File dir=new File(ctx.getFilesDir(),"member-photos");
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create private member photo folder");
  return dir;
 }

 static File file(Context context,String memberId)throws IOException{
  return new File(directory(context),key(memberId));
 }

 static boolean has(Context context,String id)throws IOException {
  File photo=file(context,id);
  return photo.isFile()&&photo.length()>100&&photo.length()<1_500_000;
 }

 static int count(Context ctx)throws IOException{
  String[] entries=directory(ctx).list((dir,name)->name.matches("member-[a-f0-9]{64}\\.jpg"));
  return entries==null?0:entries.length;
 }

 static Bitmap thumbnail(Context ctx,String id)throws IOException{
  File src=file(ctx,id);
  if(!src.isFile())return null;
  BitmapFactory.Options opts=new BitmapFactory.Options();
  opts.inSampleSize=1;
  opts.inPreferredConfig=Bitmap.Config.RGB_565;
  Bitmap photo=BitmapFactory.decodeFile(src.getAbsolutePath(),opts);
  if(photo==null||photo.getWidth()!=SIDE||photo.getHeight()!=SIDE){
   if(photo!=null)photo.recycle();
   return null;
  }
  return photo;
 }

 static void remove(Context ctx,String memberId)throws IOException{
  File target=file(ctx,memberId);
  if(target.exists()&&!target.delete())
   throw new IOException("Could not remove selected local member photograph");
 }

 static void addOrReplace(Context ctx,Uri selected,String memberId)throws Exception {
  if(selected==null)throw new IOException("No member photograph was selected");
  File target=file(ctx,memberId);
  byte[] raw=bounded(ctx.getContentResolver().openInputStream(selected));
  Bitmap crop=null,source=null,oriented=null;
  try {
   BitmapFactory.Options bounds=new BitmapFactory.Options();
   bounds.inJustDecodeBounds=true;
   BitmapFactory.decodeByteArray(raw,0,raw.length,bounds);
   if(bounds.outWidth<32||bounds.outHeight<32||
      bounds.outWidth>16000||bounds.outHeight>16000||
      (long)bounds.outWidth*bounds.outHeight>100_000_000L)
    throw new IOException("Choose a normal JPEG/PNG portrait (minimum 32 pixels, max 100 MP)");
   int sample=1;
   while(bounds.outWidth/sample>1024||bounds.outHeight/sample>1024)
    sample*=2;
   BitmapFactory.Options decode=new BitmapFactory.Options();
   decode.inSampleSize=sample;
   decode.inPreferredConfig=Bitmap.Config.ARGB_8888;
   source=BitmapFactory.decodeByteArray(raw,0,raw.length,decode);
   if(source==null)throw new IOException("Selected member image could not be decoded");
   int orientation=ExifInterface.ORIENTATION_NORMAL;
   try{
    ExifInterface exif=new ExifInterface(new ByteArrayInputStream(raw));
    orientation=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
   }catch(Exception ignore){ /* non-EXIF files still supported */ }
   Matrix rotate=new Matrix();
   switch(orientation){
    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:rotate.postScale(-1,1);break;
    case ExifInterface.ORIENTATION_ROTATE_180:rotate.postRotate(180);break;
    case ExifInterface.ORIENTATION_FLIP_VERTICAL:rotate.postScale(1,-1);break;
    case ExifInterface.ORIENTATION_TRANSPOSE:
     // Rotate, then mirror: reflection about the top-left/bottom-right diagonal.
     rotate.postRotate(90);rotate.postScale(-1,1);break;
    case ExifInterface.ORIENTATION_ROTATE_90:rotate.postRotate(90);break;
    case ExifInterface.ORIENTATION_TRANSVERSE:
     // Reflection about the top-right/bottom-left diagonal.
     rotate.postRotate(270);rotate.postScale(-1,1);break;
    case ExifInterface.ORIENTATION_ROTATE_270:rotate.postRotate(270);break;
    case ExifInterface.ORIENTATION_NORMAL:
    case ExifInterface.ORIENTATION_UNDEFINED:
    default:break;
   }
   // The app stores a normalized image, so private EXIF metadata is dropped.
   oriented=Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),rotate,true);
   int edge=Math.min(oriented.getWidth(),oriented.getHeight());
   if(edge<16)throw new IOException("Selected photograph is too small");
   crop=Bitmap.createBitmap(oriented,(oriented.getWidth()-edge)/2,
    (oriented.getHeight()-edge)/2,edge,edge);
   Bitmap resized=Bitmap.createScaledBitmap(crop,SIDE,SIDE,true);
   File stage=File.createTempFile("photo-stage-", ".jpg",directory(ctx));
   boolean succeeded=false;
   try{
    try(FileOutputStream out=new FileOutputStream(stage)){
     if(!resized.compress(Bitmap.CompressFormat.JPEG,86,out))
      throw new IOException("Unable to encode member photograph");
     out.flush();out.getFD().sync();
    }
    if(stage.length()<100||stage.length()>1_500_000)
     throw new IOException("Normalized member photograph size is not valid");
    try{
     Files.move(stage.toPath(),target.toPath(),
      StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }catch(AtomicMoveNotSupportedException e){
     throw new IOException("Atomic member photo replacement is unsupported; original photo retained",e);
    }
    succeeded=true;
   }finally{
    if(!succeeded)stage.delete();
    if(resized!=crop)resized.recycle();
   }
  }finally{
   Arrays.fill(raw,(byte)0);
   if(crop!=null&&!crop.isRecycled())crop.recycle();
   if(oriented!=null&&oriented!=crop&&oriented!=source&&!oriented.isRecycled())oriented.recycle();
   if(source!=null&&source!=oriented&&source!=crop&&!source.isRecycled())source.recycle();
  }
 }

 private static byte[] bounded(InputStream input)throws IOException{
  if(input==null)throw new IOException("Selected image could not be reopened");
  try(InputStream source=input;ByteArrayOutputStream data=new ByteArrayOutputStream()){
   byte[] buf=new byte[16384];int n;
   while((n=source.read(buf))!=-1){
    if(n==0)continue;
    if(data.size()+n>MAX_SOURCE_BYTES)
     throw new IOException("Member photo input exceeds 12 MB; choose a smaller picture");
    data.write(buf,0,n);
   }
   if(data.size()<100)throw new IOException("Selected image is empty or incomplete");
   return data.toByteArray();
  }
 }
}

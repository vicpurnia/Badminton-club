package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * Each read-only CSV share gets its own stable, independently verified file.
 * Never overwrite a file already handed to Android FileProvider / share sheet.
 */
final class PremiumV2797CsvShare {
 static final int MAX_CSV_BYTES=4_000_000;
 private PremiumV2797CsvShare(){}

 static File create(File dir,String prefix,String csv)throws IOException {
  if(dir==null||!dir.isDirectory())throw new IOException("Private share folder unavailable");
  if(prefix==null||!prefix.matches("[A-Za-z0-9_-]{3,96}"))
   throw new IOException("Unsafe CSV export filename prefix");
  if(csv==null||csv.length()==0||!csv.contains("\r\n")||csv.charAt(0)=='=')
   throw new IOException("CSV report contents missing or invalid");
  byte[] bytes=csv.getBytes(StandardCharsets.UTF_8);
  if(bytes.length==0||bytes.length>MAX_CSV_BYTES)
   throw new IOException("CSV exceeds bounded share limit");
  File file=File.createTempFile(prefix+"_",".csv",dir);
  boolean verified=false;
  try {
   try(FileOutputStream stream=new FileOutputStream(file)){
    stream.write(bytes);
    stream.flush();
    stream.getFD().sync();
   }
   byte[] expected=hash(bytes),actual=hash(file);
   if(file.length()!=bytes.length||!MessageDigest.isEqual(expected,actual))
    throw new IOException("CSV was not written or reopened accurately");
   verified=true;
   return file;
  }finally{
   Arrays.fill(bytes,(byte)0);
   if(!verified)file.delete();
  }
 }

 static void verify(File csv)throws IOException {
  if(csv==null||!csv.isFile()||csv.length()==0||csv.length()>MAX_CSV_BYTES||
     !csv.getName().endsWith(".csv"))
   throw new IOException("CSV share file missing or outside allowed bounds");
  byte[] block=new byte[8192];
  int n;
  long length=0;
  try(InputStream in=new FileInputStream(csv)){
   while((n=in.read(block))!=-1){
    if(n>0)length=Math.addExact(length,n);
    if(length>MAX_CSV_BYTES)throw new IOException("CSV share exceeds limit");
   }
  }
  if(length!=csv.length())throw new IOException("CSV share content was truncated");
 }

 private static byte[] hash(byte[] source)throws IOException{
  try{return MessageDigest.getInstance("SHA-256").digest(source);}
  catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
 }
 private static byte[] hash(File file)throws IOException {
  try{
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   try(InputStream input=new FileInputStream(file)){
    byte[] buf=new byte[16384];int n;
    while((n=input.read(buf))!=-1)if(n>0)md.update(buf,0,n);
   }
   return md.digest();
  }catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
 }
}

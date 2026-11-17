package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.security.*;
import java.util.*;

/**
 * Independent, immutable-by-convention share snapshots for existing PDFs.
 *
 * Renderer files can use millisecond-based filenames. Never hand a renderer's
 * mutable/shared cache path directly to Android's asynchronous share chooser.
 * Instead, copy only a verified PDF-like file to a uniquely named private
 * attachment, pin the exact bytes, then share that independent file.
 *
 * This does not edit the renderer, original receipt, Room or ledger history.
 * A complete semantic PDF check is done by Android PdfRenderer in instrumentation.
 */
final class PremiumV2799PdfShare {
 static final long MAX_BYTES=50_000_000L;
 private PremiumV2799PdfShare(){}

 static final class Share {
  final File file;
  final long length;
  final byte[] sha256;
  Share(File file,long length,byte[] sha256){
   this.file=file;this.length=length;this.sha256=sha256;
  }
 }

 static Share snapshot(File dir,File original)throws IOException {
  if(dir==null||!dir.isDirectory()||original==null||!original.isFile())
   throw new IOException("PDF share source or private folder unavailable");
  // Verify the source before creating the new attachment. The second source
  // check below closes the window for changes occurring during the copy.
  Evidence before=inspect(original);
  File copy=File.createTempFile("Badminton_PDF_Share_",".pdf",dir);
  boolean ready=false;
  try {
   try(InputStream input=new BufferedInputStream(new FileInputStream(original));
       FileOutputStream output=new FileOutputStream(copy)){
    byte[] buf=new byte[16384];int n;
    long total=0;
    while((n=input.read(buf))!=-1){
     if(n<=0)continue;
     total=Math.addExact(total,n);
     if(total>MAX_BYTES)throw new IOException("PDF share exceeds 50 MB limit");
     output.write(buf,0,n);
    }
    output.flush();
    output.getFD().sync();
   }
   Evidence after=inspect(original);
   Evidence copied=inspect(copy);
   if(before.size!=after.size || before.size!=copied.size ||
      !MessageDigest.isEqual(before.digest,after.digest)||
      !MessageDigest.isEqual(before.digest,copied.digest))
    throw new IOException("PDF source changed during share snapshot; no attachment shared");
   ready=true;
   return new Share(copy,copied.size,copied.digest);
  }finally{
   if(!ready)copy.delete();
  }
 }

 static void verify(Share shared)throws IOException {
  if(shared==null||shared.file==null||!shared.file.isFile()||
     shared.file.length()!=shared.length)
   throw new IOException("PDF sharing attachment is missing or was replaced");
  Evidence actual=inspect(shared.file);
  if(actual.size!=shared.length||
     !MessageDigest.isEqual(actual.digest,shared.sha256))
   throw new IOException("PDF sharing attachment was changed after verification");
 }

 private static final class Evidence {
  final long size;
  final byte[] digest;
  Evidence(long size,byte[] digest){
   this.size=size;this.digest=digest;
  }
 }

 private static Evidence inspect(File pdf)throws IOException {
  if(!pdf.isFile()||pdf.length()<100||pdf.length()>MAX_BYTES)
   throw new IOException("PDF is empty, incomplete, or oversized");
  try{
   MessageDigest sha=MessageDigest.getInstance("SHA-256");
   byte[] buf=new byte[16384],header=new byte[5],tail=new byte[2048];
   long count=0;
   int headerCount=0,tailCount=0,n;
   try(InputStream stream=new BufferedInputStream(new FileInputStream(pdf))){
    while((n=stream.read(buf))!=-1){
     if(n<=0)continue;
     count=Math.addExact(count,n);
     if(count>MAX_BYTES)throw new IOException("PDF content exceeds safe limit");
     for(int i=0;i<n&&headerCount<5;i++)header[headerCount++]=buf[i];
     if(n>=tail.length){
      System.arraycopy(buf,n-tail.length,tail,0,tail.length);
      tailCount=tail.length;
     }else{
      int retained=Math.min(tailCount,tail.length-n);
      System.arraycopy(tail,tailCount-retained,tail,0,retained);
      System.arraycopy(buf,0,tail,retained,n);
      tailCount=retained+n;
     }
     sha.update(buf,0,n);
    }
   }
   if(count<100||count!=pdf.length()||headerCount!=5||
      header[0]!='%'||header[1]!='P'||header[2]!='D'||
      header[3]!='F'||header[4]!='-'||!eofSeen(tail,tailCount))
    throw new IOException("Shared document is not a complete PDF-like file");
   return new Evidence(count,sha.digest());
  }catch(NoSuchAlgorithmException error){
   throw new IOException("SHA-256 unavailable",error);
  }
 }

 private static boolean eofSeen(byte[] tail,int length){
  byte[] marker=new byte[]{'%','%','E','O','F'};
  for(int i=0;i<=length-marker.length;i++){
   boolean found=true;
   for(int k=0;k<marker.length;k++){
    if(tail[i+k]!=marker[k]){found=false;break;}
   }
   if(found)return true;
  }
  return false;
 }
}

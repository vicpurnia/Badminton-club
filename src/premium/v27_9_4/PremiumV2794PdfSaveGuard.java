package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.security.*;
import java.util.*;

/**
 * v27.9.4 PDF Documents export verifier.
 *
 * One active picker at a time; snapshot of the exact verified PDF BEFORE
 * opening Documents; after save compare staged source AND provider readback.
 * Does not mutate club accounting, Room state, or the original Classic PDF.
 */
final class PremiumV2794PdfSaveGuard {
 private static final long MAX_PDF_BYTES=50_000_000L;
 private boolean busy=false;
 private Evidence staged;

 private static final class Evidence {
  final String path;
  final long length;
  final byte[] sha;
  Evidence(String path,long length,byte[] sha){
   this.path=path;this.length=length;this.sha=sha;
  }
 }

 synchronized boolean begin(){
  if(busy)return false;
  busy=true;
  staged=null;
  return true;
 }

 synchronized boolean active(){return busy;}

 synchronized void stage(File pdf)throws IOException {
  if(!busy||staged!=null)throw new IOException("PDF save review session unavailable");
  if(pdf==null||!pdf.isFile())throw new IOException("Generated PDF is missing");
  Evidence e=digest(pdf);
  staged=e;
 }

 synchronized void verify(File original,InputStream saved)throws IOException {
  if(!busy||staged==null)
   throw new IOException("PDF save verification session expired");
  if(original==null||!staged.path.equals(original.getCanonicalPath()))
   throw new IOException("PDF source differs from the reviewed file");
  Evidence unchanged=digest(original);
  if(unchanged.length!=staged.length||
     !MessageDigest.isEqual(unchanged.sha,staged.sha))
   throw new IOException("PDF changed after preview; destination is not verified");
  Evidence remote=digest(saved);
  if(remote.length!=staged.length||
     !MessageDigest.isEqual(remote.sha,staged.sha))
   throw new IOException("Android Documents saved incomplete or altered PDF bytes");
 }

 synchronized void finish(){
  busy=false;
  staged=null;
 }

 private static Evidence digest(File pdf)throws IOException {
  try(InputStream input=new FileInputStream(pdf)){
   Evidence raw=digest(input);
   return new Evidence(pdf.getCanonicalPath(),raw.length,raw.sha);
  }
 }

 private static Evidence digest(InputStream input)throws IOException {
  if(input==null)throw new IOException("Saved PDF could not be reopened");
  try{
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   byte[] block=new byte[16384],header=new byte[5];
   int head=0,n;
   long total=0;
   while((n=input.read(block))!=-1){
    if(n==0)continue;
    total=Math.addExact(total,n);
    if(total>MAX_PDF_BYTES)throw new IOException("PDF exceeds safe 50 MB export limit");
    for(int i=0;i<n&&head<header.length;i++)header[head++]=block[i];
    md.update(block,0,n);
   }
   if(total<100||head!=5||!Arrays.equals(header,new byte[]{37,80,68,70,45}))
    throw new IOException("Output is not a valid generated PDF header");
   return new Evidence("",total,md.digest());
  }catch(NoSuchAlgorithmException e){
   throw new IOException("SHA-256 is unavailable",e);
  }
 }
}

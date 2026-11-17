package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.security.*;
import java.util.*;

/**
 * One unencrypted verified JSON export per Android Documents picker.
 * Remembers only the exact staged file path, length and digest.
 * The app's MigrationCodec.inspect validates ledger structure before staging;
 * BackupGuardian.verifySavedCopy validates the saved Android document.
 */
final class PremiumV2795JsonExportGuard {
 static final long MAX_BYTES=12_000_000L;
 private boolean busy;
 private String stagedPath;
 private long stagedSize;
 private byte[] stagedSha;

 synchronized boolean begin(){
  if(busy)return false;
  busy=true;
  stagedPath=null;stagedSize=0;stagedSha=null;
  return true;
 }

 synchronized boolean active(){return busy;}

 synchronized void stage(File staged)throws IOException {
  if(!busy||stagedSha!=null)
   throw new IOException("Verified JSON export session unavailable");
  if(staged==null||!staged.isFile())
   throw new IOException("Verified JSON staged file is missing");
  byte[] sha=digest(staged);
  stagedPath=staged.getCanonicalPath();
  stagedSize=staged.length();
  stagedSha=sha;
 }

 synchronized void requireUnchanged(File staged)throws IOException {
  if(!busy||stagedSha==null||staged==null||
     !staged.getCanonicalPath().equals(stagedPath)||
     staged.length()!=stagedSize||
     !MessageDigest.isEqual(stagedSha,digest(staged)))
   throw new IOException("Verified JSON staging file changed while Documents picker was open");
 }

 synchronized void finish(){
  busy=false;
  stagedPath=null;stagedSize=0;
  if(stagedSha!=null)Arrays.fill(stagedSha,(byte)0);
  stagedSha=null;
 }

 private static byte[] digest(File file)throws IOException {
  long bytes=0;
  try(InputStream input=new FileInputStream(file)){
   MessageDigest digest=MessageDigest.getInstance("SHA-256");
   byte[] buffer=new byte[16384];int n;
   while((n=input.read(buffer))!=-1){
    if(n<=0)continue;
    bytes=Math.addExact(bytes,n);
    if(bytes>MAX_BYTES)throw new IOException("Verified JSON export exceeds safe 12 MB limit");
    digest.update(buffer,0,n);
   }
   if(bytes==0)throw new IOException("Verified JSON file is empty");
   return digest.digest();
  }catch(NoSuchAlgorithmException exception){
   throw new IOException("SHA-256 provider unavailable",exception);
  }
 }
}

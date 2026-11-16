package com.vinayak.badminton.nativepreview;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Before exposing the document picker, verify the exact ciphertext saved to private cache. */
final class PremiumV27BackupStaging {
 private PremiumV27BackupStaging(){}
 static void verify(File staged,byte[] original)throws IOException {
  if(original==null||original.length<40||original.length>19_000_000||
     staged==null||!staged.isFile()||staged.length()!=original.length)
   throw new IOException("Encrypted staging file missing or wrong length");
  try{
   MessageDigest expected=MessageDigest.getInstance("SHA-256");
   byte[] originalHash=expected.digest(original);
   MessageDigest observed=MessageDigest.getInstance("SHA-256");
   long count=0;
   byte[] buffer=new byte[16384];
   try(InputStream input=new FileInputStream(staged)){
    int read;
    while((read=input.read(buffer))!=-1){
     count=Math.addExact(count,read);
     if(count>19_000_000)throw new IOException("Staged encrypted file too large");
     observed.update(buffer,0,read);
    }
   }
   if(count!=original.length||!MessageDigest.isEqual(originalHash,observed.digest()))
    throw new IOException("Encrypted export was modified or truncated during staging");
  }catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
 }
}

package com.vinayak.badminton.nativepreview;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class V27BackupStagingSmoke {
 static void yes(boolean x){if(!x)throw new AssertionError("Staging invariant");}
 static void reject(File f,byte[] src)throws Exception{
  boolean caught=false;try{PremiumV27BackupStaging.verify(f,src);}catch(IOException expected){caught=true;}yes(caught);
 }
 public static void main(String[] args)throws Exception {
  Path path=Files.createTempFile("v27-encrypted", ".bpe");
  try{
   byte[] encoded="{\"format\":\"encrypted\",\"payload\":\"aBcDeF12345678901234567890\"}".getBytes(StandardCharsets.UTF_8);
   Files.write(path,encoded);
   PremiumV27BackupStaging.verify(path.toFile(),encoded);
   byte[] changed=encoded.clone();changed[25]^=7;
   reject(path.toFile(),changed);
   Files.write(path,new byte[20]);reject(path.toFile(),encoded);
   Files.delete(path);reject(path.toFile(),encoded);
   System.out.println("PASS v27.8 encrypted ciphertext staged-file byte integrity");
  }finally{Files.deleteIfExists(path);}
 }
}

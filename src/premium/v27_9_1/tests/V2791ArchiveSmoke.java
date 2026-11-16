package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.zip.*;

/** Demonstrates old ZIP-stream-only acceptance and strict central-dir rejection. */
public final class V2791ArchiveSmoke {
 private static void yes(boolean value,String why){
  if(!value)throw new AssertionError(why);
 }
 private static void reject(File archive,int count,String why)throws Exception{
  boolean blocked=false;
  try{PremiumV27ReceiptArchive.verify(archive,count);}
  catch(IOException expected){blocked=true;}
  yes(blocked,why);
 }
 private static File make(Path dir,String filename,String entry)throws Exception{
  File output=dir.resolve(filename).toFile();
  try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(output))){
   zip.putNextEntry(new ZipEntry(entry));
   byte[] pdf=new byte[512];
   Arrays.fill(pdf,(byte)37);
   zip.write(pdf);
   zip.closeEntry();
  }
  return output;
 }
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("badminton-v2791-archive");
  try{
   File valid=make(dir,"valid.zip","001_2026-08_pay-ci-fixture.pdf");
   PremiumV27ReceiptArchive.verify(valid,1);
   byte[] full=Files.readAllBytes(valid.toPath());
   int central=-1;
   for(int i=0;i<full.length-4;i++)
    if((full[i]&0xff)==0x50&&(full[i+1]&0xff)==0x4b&&
       (full[i+2]&0xff)==0x01&&(full[i+3]&0xff)==0x02){
      central=i;break;
    }
   yes(central>40,"Central directory must exist in fixture");
   File noIndex=dir.resolve("missing-central.zip").toFile();
   Files.write(noIndex.toPath(),Arrays.copyOf(full,central));
   // Local-record stream can read this broken archive, but desktop ZIP apps
   // reject it; verifier must reject it before Android sharing.
   int streamed=0;
   try(ZipInputStream input=new ZipInputStream(new FileInputStream(noIndex))){
    ZipEntry e;
    byte[] scratch=new byte[128];
    while((e=input.getNextEntry())!=null){
     int n;while((n=input.read(scratch))!=-1)streamed+=n;
     input.closeEntry();
    }
   }
   yes(streamed==512,"This fixture must demonstrate the unsafe stream-only acceptance");
   reject(noIndex,1,"Central-directory-less ZIP must be rejected");
   reject(valid,2,"ZIP with unexpected entry count must be rejected");
   reject(make(dir,"unsafe.zip","../unsafe.pdf"),1,"Path traversal must be rejected");
   // Corrupt the central-directory CRC while preserving a valid local record.
   byte[] damaged=full.clone();
   int crcOffset=central+16;
   damaged[crcOffset]=(byte)(damaged[crcOffset]^0x5a);
   File crc=dir.resolve("crc-mismatch.zip").toFile();
   Files.write(crc.toPath(),damaged);
   reject(crc,1,"Local and central archive metadata must agree");
   System.out.println("PASS v27.9.1: ZIP index, CRC, missing EOCD, traversal and count safety");
  }finally{
   try(java.util.stream.Stream<Path> paths=Files.list(dir)){
    paths.forEach(path->path.toFile().delete());
   }
   Files.deleteIfExists(dir);
  }
 }
}

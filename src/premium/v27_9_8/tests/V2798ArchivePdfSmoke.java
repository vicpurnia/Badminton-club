package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Receipt ZIP payload sanity check: archive CRC alone does not make a valid PDF. */
public final class V2798ArchivePdfSmoke {
 private static void yes(boolean value,String why){
  if(!value)throw new AssertionError(why);
 }
 private static void reject(File file,int entries,String why)throws Exception{
  boolean blocked=false;
  try{PremiumV27ReceiptArchive.verify(file,entries);}
  catch(IOException expected){blocked=true;}
  yes(blocked,why);
 }
 private static byte[] pdf(int bytes){
  byte[] result=new byte[bytes];
  Arrays.fill(result,(byte)' ');
  byte[] start="%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
  byte[] eof="\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
  System.arraycopy(start,0,result,0,start.length);
  System.arraycopy(eof,0,result,bytes-eof.length,eof.length);
  return result;
 }
 private static File archive(Path path,byte[] first,byte[] second)throws Exception{
  File output=path.toFile();
  try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(output))){
   zip.putNextEntry(new ZipEntry("001_2026-08_ci-original.pdf"));
   zip.write(first);
   zip.closeEntry();
   if(second!=null){
    zip.putNextEntry(new ZipEntry("002_2026-08_ci-secondary.pdf"));
    zip.write(second);
    zip.closeEntry();
   }
  }
  return output;
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("badminton-v2798-receipt-pdf");
  try{
   File good=archive(root.resolve("good.zip"),pdf(350),null);
   PremiumV27ReceiptArchive.verify(good,1);
   byte[] fakePdf=new byte[350];
   Arrays.fill(fakePdf,(byte)'X');
   reject(archive(root.resolve("wrong-header.zip"),fakePdf,null),1,
    "Valid ZIP with non-PDF payload must fail");
   byte[] missingFooter=pdf(350);
   Arrays.fill(missingFooter,330,350,(byte)' ');
   reject(archive(root.resolve("no-eof.zip"),missingFooter,null),1,
    "PDF with missing %%EOF marker must fail");
   byte[] wrongStart=pdf(350);
   wrongStart[0]='X';
   reject(archive(root.resolve("bad-prefix.zip"),wrongStart,null),1,
    "PDF that does not start with %PDF- must fail");
   reject(archive(root.resolve("mixed.zip"),pdf(350),fakePdf),2,
    "One valid and one invalid receipt PDF must reject entire bundle");

   File staged=archive(root.resolve("bad.partial"),pdf(350),fakePdf);
   File finalZip=root.resolve("never-shared.zip").toFile();
   boolean failClosed=false;
   try{PremiumV27ReceiptArchive.promote(staged,finalZip,2);}
   catch(IOException expected){failClosed=true;}
   yes(failClosed&&!staged.exists()&&!finalZip.exists(),
    "Invalid PDF archive must not be promoted or leave staging data");

   File promoted=PremiumV27ReceiptArchive.promote(
    archive(root.resolve("good.partial"),pdf(512),pdf(200)),
    root.resolve("ready-to-share.zip").toFile(),2);
   PremiumV27ReceiptArchive.verify(promoted,2);

   // Once successfully promoted, any modification before the Android share
   // chooser must be rejected by the added final pre-share verification.
   byte[] changed=Files.readAllBytes(promoted.toPath());
   changed[changed.length-1]=(byte)(changed[changed.length-1]^0x1f);
   Files.write(promoted.toPath(),changed);
   reject(promoted,2,"Post-promotion archive corruption must be rejected");
   System.out.println("PASS v27.9.8: PDF header/EOF inside ZIP, mixed entries, staged cleanup and pre-share corruption");
  }finally{
   try(java.util.stream.Stream<Path> paths=Files.list(root)){
    paths.forEach(p->p.toFile().delete());
   }
   Files.deleteIfExists(root);
  }
 }
}

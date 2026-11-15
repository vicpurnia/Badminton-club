package com.vinayak.badminton.nativepreview;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
public final class V27ReceiptArchiveSmoke {
 static void yes(boolean result,String why){if(!result)throw new AssertionError(why);}
 static void bad(File file,int count)throws Exception {
  boolean rejected=false;
  try{PremiumV27ReceiptArchive.verify(file,count);}catch(IOException expected){rejected=true;}
  yes(rejected,"unsafe ZIP must fail");
 }
 static File zip(Path dir,String name,String entry,int bytes)throws Exception {
  File file=dir.resolve(name).toFile();
  try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(file))){
   out.putNextEntry(new ZipEntry(entry));
   byte[] content=new byte[bytes];
   for(int i=0;i<bytes;i++)content[i]=(byte)(i%97);
   out.write(content);
   out.closeEntry();
  }
  return file;
 }
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("v27-receipts");
  try {
   File a=zip(dir,"ok.partial","001_2026-11_pay-1.pdf",180);
   PremiumV27ReceiptArchive.verify(a,1);
   File target=dir.resolve("verified.zip").toFile();
   PremiumV27ReceiptArchive.promote(a,target,1);
   yes(target.isFile()&&!a.exists(),"staged ZIP promoted");
   bad(target,2);
   bad(zip(dir,"escape.zip","../unsafe.pdf",180),1);
   bad(zip(dir,"small.zip","001_2026-11_pay-1.pdf",50),1);
   File truncated=zip(dir,"broken.zip","001_2026-11_pay-1.pdf",180);
   try(java.io.RandomAccessFile out=new java.io.RandomAccessFile(truncated,"rw")){
    out.setLength(12);
   }
   bad(truncated,1);
   yes(!target.renameTo(dir.resolve("verified_again.zip").toFile())==false,"valid renamed file");
   System.out.println("PASS v27.3 staged, CRC-reviewed member receipt ZIP verification");
  }finally{
   try(java.util.stream.Stream<Path> paths=Files.list(dir)){paths.forEach(p->p.toFile().delete());}
   Files.deleteIfExists(dir);
  }
 }
}

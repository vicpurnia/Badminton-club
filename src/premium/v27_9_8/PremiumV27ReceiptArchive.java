package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Verifies a generated receipt ZIP by streaming every entry: ZIP index,
 * CRC, safe names and a lightweight PDF signature/trailer check.
 * All validation occurs before the temporary ZIP is promoted for sharing.
 * Does not modify original cash, receipt IDs, or transaction dates.
 */
final class PremiumV27ReceiptArchive {
 static final int MAX_RECEIPTS=400;
 static final long MAX_ARCHIVE_BYTES=35_000_000L;
 private PremiumV27ReceiptArchive(){}
 static void verify(File archive,int expectedEntries)throws IOException {
  if(archive==null || !archive.isFile() || archive.length()<100 ||
     archive.length()>MAX_ARCHIVE_BYTES ||
     expectedEntries<1 || expectedEntries>MAX_RECEIPTS)
   throw new IOException("Receipt archive missing, oversized, or empty");
  // A local-entry stream alone accepts ZIPs with the entire central directory
  // truncated. Those often fail when saved/opened by Android or desktop ZIP apps.
  // ZipFile requires a valid end-of-central-directory and entry index first.
  final Set<String> indexedNames=new HashSet<>();
  final Map<String,Long> indexedCrc=new HashMap<>(),indexedSizes=new HashMap<>();
  try(ZipFile index=new ZipFile(archive)){
   if(index.size()!=expectedEntries)
    throw new IOException("Receipt ZIP central directory entry count mismatch");
   Enumeration<? extends ZipEntry> registered=index.entries();
   while(registered.hasMoreElements()){
    ZipEntry item=registered.nextElement();
    String name=item.getName();
    if(item.isDirectory()||name==null||
       !name.matches("[0-9]{3}_[0-9]{4}-[0-9]{2}_[A-Za-z0-9_-]+\\.pdf")||
       !indexedNames.add(name)||item.getSize()<100||
       item.getSize()>MAX_ARCHIVE_BYTES||item.getCrc()<0)
     throw new IOException("Invalid receipt ZIP central directory entry");
    indexedCrc.put(name,item.getCrc());
    indexedSizes.put(name,item.getSize());
   }
  }
  int count=0;
  long unpacked=0;
  byte[] buffer=new byte[8192];
  Set<String> seen=new HashSet<>();
  try(ZipInputStream input=new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))){
   ZipEntry entry;
   while((entry=input.getNextEntry())!=null){
    String name=entry.getName();
    if(++count>expectedEntries || entry.isDirectory() || name==null ||
       !name.matches("[0-9]{3}_[0-9]{4}-[0-9]{2}_[A-Za-z0-9_-]+\\.pdf") ||
       !seen.add(name)||!indexedNames.contains(name))
     throw new IOException("Unexpected/duplicated receipt entry");
    long fileBytes=0;int n;
    CRC32 crc=new CRC32();
    byte[] pdfStart=new byte[5];
    int prefixCount=0;
    byte[] pdfTail=new byte[2048];
    int tailCount=0;
    while((n=input.read(buffer))!=-1){
     if(n==0)continue;
     fileBytes=Math.addExact(fileBytes,n);
     unpacked=Math.addExact(unpacked,n);
     crc.update(buffer,0,n);
     if(unpacked>MAX_ARCHIVE_BYTES)throw new IOException("Receipt archive exceeds safe unpacked size");
     for(int k=0;k<n&&prefixCount<pdfStart.length;k++)
      pdfStart[prefixCount++]=buffer[k];
     // Keep only the final 2 KB, without loading a large PDF into memory.
     if(n>=pdfTail.length){
      System.arraycopy(buffer,n-pdfTail.length,pdfTail,0,pdfTail.length);
      tailCount=pdfTail.length;
     }else{
      int keep=Math.min(tailCount,pdfTail.length-n);
      System.arraycopy(pdfTail,tailCount-keep,pdfTail,0,keep);
      System.arraycopy(buffer,0,pdfTail,keep,n);
      tailCount=keep+n;
     }
    }
    if(fileBytes<100)throw new IOException("Incomplete receipt PDF");
    if(prefixCount!=5||pdfStart[0]!='%'||pdfStart[1]!='P'||
       pdfStart[2]!='D'||pdfStart[3]!='F'||pdfStart[4]!='-'||
       !pdfEofPresent(pdfTail,tailCount))
     throw new IOException("Receipt archive contains a non-PDF or truncated PDF payload");
    if(fileBytes!=indexedSizes.get(name)||crc.getValue()!=indexedCrc.get(name))
     throw new IOException("Receipt ZIP central directory CRC or entry size does not match contents");
    input.closeEntry(); // ZipInputStream validates CRC where supported.
   }
  }
  if(count!=expectedEntries||!seen.equals(indexedNames))
   throw new IOException("Receipt ZIP stream and directory disagree");
 }
 /** Lightweight PDF header/trailer sanity; Android PdfRenderer validates output separately. */
 private static boolean pdfEofPresent(byte[] tail,int length){
  final byte[] end=new byte[]{'%','%','E','O','F'};
  for(int i=0;i<=length-end.length;i++){
   boolean match=true;
   for(int k=0;k<end.length;k++)if(tail[i+k]!=end[k]){match=false;break;}
   if(match)return true;
  }
  return false;
 }
 static File promote(File staged,File destination,int expectedEntries)throws IOException {
  try{
   verify(staged,expectedEntries);
   if(destination==null||destination.exists()||!staged.renameTo(destination))
    throw new IOException("Could not finalize receipt ZIP safely");
   return destination;
  }catch(IOException|RuntimeException problem){
   // A failed CRC/index validation must not leave a malformed .partial
   // file in the receipts/share folder, nor alter any existing archive.
   if(staged!=null)staged.delete();
   throw problem;
  }
 }
}

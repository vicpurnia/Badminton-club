package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Same-millisecond renderer collisions must never alter an already-shared PDF. */
public final class V2799PdfShareSmoke {
 private static void yes(boolean ok,String why){
  if(!ok)throw new AssertionError(why);
 }
 private interface Action{void run()throws Exception;}
 private static void reject(Action fn,String why)throws Exception {
  boolean denied=false;
  try{fn.run();}catch(IOException expected){denied=true;}
  yes(denied,why);
 }
 private static byte[] fakePdf(int amount,char distinguishing){
  byte[] data=new byte[amount];
  Arrays.fill(data,(byte)' ');
  byte[] header="%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
  byte[] footer="\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
  System.arraycopy(header,0,data,0,header.length);
  data[amount/2]=(byte)distinguishing;
  System.arraycopy(footer,0,data,amount-footer.length,footer.length);
  return data;
 }
 public static void main(String[] args)throws Exception {
  Path folder=Files.createTempDirectory("badminton-v2799-pdf");
  File renderer=folder.resolve("Historical_Contribution_1700000000000.pdf").toFile();
  try {
   byte[] before=fakePdf(500,'A');
   byte[] after=fakePdf(500,'B');
   Files.write(renderer.toPath(),before);
   PremiumV2799PdfShare.Share first=
    PremiumV2799PdfShare.snapshot(folder.toFile(),renderer);
   PremiumV2799PdfShare.verify(first);
   // Same generated source path now contains another receipt. Existing
   // FileProvider URI for the first share must still return original bytes.
   Files.write(renderer.toPath(),after);
   PremiumV2799PdfShare.Share second=
    PremiumV2799PdfShare.snapshot(folder.toFile(),renderer);
   PremiumV2799PdfShare.verify(second);
   yes(!first.file.equals(second.file),"Each PDF share requires a unique attachment file");
   yes(!first.file.equals(renderer)&&!second.file.equals(renderer),
    "Never share the original mutable renderer path");
   yes(Arrays.equals(Files.readAllBytes(first.file.toPath()),before),
    "Previously shared receipt must survive a later render collision");
   yes(Arrays.equals(Files.readAllBytes(second.file.toPath()),after),
    "New share must contain its own independent receipt bytes");
   Files.delete(renderer.toPath());
   PremiumV2799PdfShare.verify(first);
   PremiumV2799PdfShare.verify(second);

   byte[] modified=Files.readAllBytes(first.file.toPath());
   modified[250]=(byte)'Q';
   Files.write(first.file.toPath(),modified);
   reject(()->PremiumV2799PdfShare.verify(first),
    "Same-size altered shared PDF must fail SHA-256");
   byte[] truncated=Arrays.copyOf(Files.readAllBytes(second.file.toPath()),55);
   Files.write(second.file.toPath(),truncated);
   reject(()->PremiumV2799PdfShare.verify(second),
    "Truncated shared PDF must fail");

   Files.write(renderer.toPath(),new byte[300]);
   reject(()->PremiumV2799PdfShare.snapshot(folder.toFile(),renderer),
    "Non-PDF payload with valid length cannot be shared");
   byte[] noFooter=fakePdf(300,'Z');
   Arrays.fill(noFooter,280,300,(byte)' ');
   Files.write(renderer.toPath(),noFooter);
   reject(()->PremiumV2799PdfShare.snapshot(folder.toFile(),renderer),
    "Incomplete PDF footer cannot be shared");
   reject(()->PremiumV2799PdfShare.snapshot(folder.toFile(),
    new File(folder.toFile(),"missing.pdf")),"Missing renderer source rejected");

   System.out.println("PASS v27.9.9 immutable shared PDF, renderer collision, tampering and missing PDF");
  }finally{
   try(java.util.stream.Stream<Path> paths=Files.list(folder)){
    paths.forEach(p->p.toFile().delete());
   }
   Files.deleteIfExists(folder);
  }
 }
}

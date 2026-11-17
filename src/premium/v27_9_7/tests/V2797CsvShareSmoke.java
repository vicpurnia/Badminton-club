package com.vinayak.badminton.nativepreview;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class V2797CsvShareSmoke{
 static void yes(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void reject(Checked c,String why)throws Exception{
  boolean failed=false;
  try{c.run();}catch(IOException expected){failed=true;}
  yes(failed,why);
 }
 interface Checked{void run()throws Exception;}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("badminton-v2797-csv");
  String a="Fee_Period,Recognized_Contributions_INR\r\n2026-11,100\r\n";
  String b="Fee_Period,Recognized_Contributions_INR\r\n2026-11,200\r\n";
  try {
   PremiumV2797CsvShare.Share first=PremiumV2797CsvShare.create(dir.toFile(),"Premium_Dashboard_2026-11",a);
   PremiumV2797CsvShare.Share second=PremiumV2797CsvShare.create(dir.toFile(),"Premium_Dashboard_2026-11",b);
   yes(!first.file.equals(second.file),"Concurrent share filenames must be unique");
   yes(!first.file.getName().equals(second.file.getName()),"No overwritten attachment");
   yes(Files.readString(first.file.toPath(),StandardCharsets.UTF_8).equals(a),
    "First attachment bytes must survive later same-month exports");
   yes(Files.readString(second.file.toPath(),StandardCharsets.UTF_8).equals(b),
    "Second report independently accurate");
   PremiumV2797CsvShare.verify(first);
   PremiumV2797CsvShare.verify(second);
   PremiumV2797CsvShare.Share yearly=PremiumV2797CsvShare.create(dir.toFile(),
    "Premium_Yearly_2026-11","Year,Recorded_Months\r\n2026,11\r\n");
   PremiumV2797CsvShare.verify(yearly);
   reject(()->PremiumV2797CsvShare.create(dir.toFile(),"../escape",a),
    "Path traversal prefix rejected");
   reject(()->PremiumV2797CsvShare.create(dir.toFile(),"Prem",null),
    "Missing CSV blocked");
   reject(()->PremiumV2797CsvShare.create(dir.toFile(),"Prem","=HYPERLINK(\"x\")\r\n"),
    "Formula-leading body cannot be shared");
   Files.writeString(first.file.toPath(),"x",StandardCharsets.UTF_8);
   reject(()->PremiumV2797CsvShare.verify(first),"Changed share file must fail SHA-256 check");
   System.out.println("PASS v27.9.7 independent monthly/yearly CSV share, no overwrite");
  }finally {
   try(java.util.stream.Stream<Path> paths=Files.list(dir)){
    paths.forEach(path->path.toFile().delete());
   }
   Files.deleteIfExists(dir);
  }
 }
}

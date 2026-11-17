package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class V2795JsonExportSmoke {
 private static void check(boolean value,String reason){
  if(!value)throw new AssertionError(reason);
 }
 private static void reject(RunnableIO task,String reason)throws Exception{
  boolean refused=false;
  try{task.run();}catch(IOException expected){refused=true;}
  check(refused,reason);
 }
 interface RunnableIO{void run()throws Exception;}
 public static void main(String[] args)throws Exception{
  Path folder=Files.createTempDirectory("v2795-json-export");
  File first=folder.resolve("first.json").toFile();
  File other=folder.resolve("other.json").toFile();
  try{
   byte[] original="{\"verifiedLedger\":true,\"cash\":1579}".getBytes(StandardCharsets.UTF_8);
   Files.write(first.toPath(),original);
   Files.write(other.toPath(),original);
   PremiumV2795JsonExportGuard gate=new PremiumV2795JsonExportGuard();
   check(gate.begin(),"First export must reserve Documents picker");
   check(!gate.begin(),"Rapid second export must not overwrite staged file");
   gate.stage(first);
   gate.requireUnchanged(first);
   reject(()->gate.requireUnchanged(other),"Wrong file must not be verified");
   Files.write(first.toPath(),"{\"verifiedLedger\":true,\"cash\":2579}".getBytes(StandardCharsets.UTF_8));
   reject(()->gate.requireUnchanged(first),"Same-sized rewritten content must be rejected");
   gate.finish();
   check(!gate.active(),"Cancel releases export");
   check(gate.begin(),"Next independent verified export may begin");
   reject(()->gate.requireUnchanged(first),"A session without stage cannot pass");
   gate.stage(other);
   gate.requireUnchanged(other);
   gate.finish();
   check(gate.begin(),"Release allows new export");
   File empty=folder.resolve("empty.json").toFile();
   Files.write(empty.toPath(),new byte[0]);
   reject(()->gate.stage(empty),"Empty staged JSON rejected");
   gate.finish();
   check(gate.begin(),"Failure does not permanently lock the export");
   gate.finish();
   System.out.println("PASS v27.9.5 verified JSON export overlap, identity, tamper, cancel");
  }finally{
   try(java.util.stream.Stream<Path> stream=Files.list(folder)){
    stream.forEach(p->p.toFile().delete());
   }
   Files.deleteIfExists(folder);
  }
 }
}

package com.vinayak.badminton.nativepreview;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

public final class V2794PdfSaveSmoke {
 static void yes(boolean b,String reason){if(!b)throw new AssertionError(reason);}
 static void reject(Checked action,String reason)throws Exception{
  boolean refused=false;
  try{action.run();}catch(IOException expected){refused=true;}
  yes(refused,reason);
 }
 interface Checked{void run()throws Exception;}
 static byte[] pdf(){
  byte[] result=new byte[700];
  byte[] h="%PDF-1.4\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  System.arraycopy(h,0,result,0,h.length);
  Arrays.fill(result,h.length,result.length,(byte)32);
  return result;
 }
 public static void main(String[] args)throws Exception {
  Path dir=Files.createTempDirectory("v2794-pdf-save");
  File src=dir.resolve("original.pdf").toFile();
  File other=dir.resolve("different.pdf").toFile();
  try {
   byte[] correct=pdf(),tampered=pdf();
   tampered[250]=65;
   Files.write(src.toPath(),correct);
   Files.write(other.toPath(),correct);
   PremiumV2794PdfSaveGuard gate=new PremiumV2794PdfSaveGuard();
   yes(gate.begin(),"first save reserves the picker");
   yes(!gate.begin(),"repeated save cannot replace pending PDF");
   gate.stage(src);
   gate.verify(src,new ByteArrayInputStream(correct));
   reject(()->gate.verify(src,new ByteArrayInputStream(tampered)),
    "same-size modified PDF must reject readback");
   reject(()->gate.verify(other,new ByteArrayInputStream(correct)),
    "different source path must not pass");
   reject(()->gate.verify(src,new ByteArrayInputStream(Arrays.copyOf(correct,100))),
    "truncated saved file must be rejected");
   Files.write(src.toPath(),tampered);
   reject(()->gate.verify(src,new ByteArrayInputStream(tampered)),
    "source modified while picker open must be rejected");
   gate.finish();
   yes(!gate.active(),"completed picker releases reservation");
   yes(gate.begin(),"new PDF may save after completion");
   reject(()->gate.verify(src,new ByteArrayInputStream(tampered)),
    "session without staged PDF cannot verify");
   gate.finish();
   yes(gate.begin(),"cancel can release gate");
   Files.write(src.toPath(),new byte[]{1,2,3,4});
   reject(()->gate.stage(src),"invalid PDF header or too short must reject");
   gate.finish();
   yes(gate.begin(),"failure cannot permanently lock further PDF saves");
   gate.finish();
   System.out.println("PASS v27.9.4 PDF identity, SHA readback, cancellation and overlapping picker");
  }finally{
   Files.deleteIfExists(src.toPath());
   Files.deleteIfExists(other.toPath());
   Files.deleteIfExists(dir);
  }
 }
}

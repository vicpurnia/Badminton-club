package com.vinayak.badminton.nativepreview;
import java.io.IOException;

/** Strict print media only; real printer review remains a physical acceptance gate. */
public final class V27100PrintSmoke {
 private static void yes(boolean v,String why){if(!v)throw new AssertionError(why);}
 private interface A{void run()throws IOException;}
 private static void reject(A a,String why){
  boolean blocked=false;
  try{a.run();}catch(IOException expected){blocked=true;}
  yes(blocked,why);
 }
 public static void main(String[] args)throws Exception {
  PremiumV27100A4PrintGuard.requireExact("ISO_A4",11690,8270,"ISO_A4",11690,8270);
  reject(()->PremiumV27100A4PrintGuard.requireExact(
   "ISO_A4",8270,11690,"ISO_A4",11690,8270),"A4 portrait blocked");
  reject(()->PremiumV27100A4PrintGuard.requireExact(
   "NA_LETTER",11000,8500,"ISO_A4",11690,8270),"Letter landscape blocked");
  reject(()->PremiumV27100A4PrintGuard.requireExact(
   "ISO_A4",11691,8270,"ISO_A4",11690,8270),"Incorrect A4 width blocked");
  reject(()->PremiumV27100A4PrintGuard.requireExact(
   null,11690,8270,"ISO_A4",11690,8270),"Missing media blocked");
  reject(()->PremiumV27100A4PrintGuard.requireExact(
   "ISO_A4",11690,8270,"ISO_A4",8270,11690),"Invalid expected orientation blocked");
  System.out.println("PASS v27.9.10: Classic Master exact A4 landscape print media contract");
 }
}

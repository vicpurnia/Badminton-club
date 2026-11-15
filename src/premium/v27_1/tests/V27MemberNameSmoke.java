package com.vinayak.badminton.nativepreview;
import java.util.*;
public final class V27MemberNameSmoke {
 static void yes(boolean v,String why){if(!v)throw new AssertionError(why);}
 static void reject(Runnable r,String why) {
  try {r.run();throw new AssertionError(why);}
  catch(IllegalArgumentException expected){}
 }
 public static void main(String[] args) {
  yes(PremiumV27MemberNames.key("  RaVi   Sharma  ").equals("ravi sharma"),"spaces");
  yes(PremiumV27MemberNames.key("ＲＡＶＩ").equals("ravi"),"fullwidth");
  yes(PremiumV27MemberNames.key("Ravi\u200B").equals("ravi"),"invisible mark");
  reject(()->PremiumV27MemberNames.requireUnique("  RAVI  ",Arrays.asList("Ravi")),"case duplicate");
  reject(()->PremiumV27MemberNames.requireUnique("Ｒａｖｉ",Arrays.asList("Ravi")),"compatibility duplicate");
  PremiumV27MemberNames.requireUnique("Ravi",Arrays.asList("Kanishk","Rohan"));
  reject(()->PremiumV27MemberNames.key("  "), "empty member");
  System.out.println("PASS v27.1 Unicode/case/spacing member identity smoke");
 }
}

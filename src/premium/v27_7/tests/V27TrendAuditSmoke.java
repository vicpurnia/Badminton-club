package com.vinayak.badminton.nativepreview;
public final class V27TrendAuditSmoke {
 static void yes(boolean x){if(!x)throw new AssertionError("Dashboard review invariant");}
 static void reject(Runnable r){boolean failed=false;try{r.run();}catch(IllegalArgumentException|IllegalStateException e){failed=true;}yes(failed);}
 public static void main(String[] args){
  PremiumV27TrendAudit.requireOrdered(new String[]{"2026-02","2026-04","2026-11"});
  reject(()->PremiumV27TrendAudit.requireOrdered(new String[]{"2026-11","2026-04"}));
  reject(()->PremiumV27TrendAudit.requireOrdered(new String[]{"2026-11","2026-11"}));
  reject(()->PremiumV27TrendAudit.requireOrdered(new String[]{"2026-13"}));
  reject(()->PremiumV27TrendAudit.requireOrdered(new String[]{}));
  System.out.println("PASS v27.7 dashboard export chronology, duplicates and invalid dates");
 }
}

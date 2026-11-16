package com.vinayak.badminton.nativepreview;
public final class V27StockVarianceSmoke {
 static void yes(boolean v){if(!v)throw new AssertionError("Stock review contract failed");}
 static void reject(Runnable r){boolean caught=false;try{r.run();}catch(IllegalArgumentException e){caught=true;}yes(caught);}
 public static void main(String[] args){
  yes(PremiumV27StockVariance.describe(50,50).startsWith("MATCH"));
  yes(PremiumV27StockVariance.describe(50,45).contains("SHORTFALL: 5"));
  yes(PremiumV27StockVariance.describe(50,65).contains("EXCESS: 15"));
  reject(()->PremiumV27StockVariance.describe(-1,2));
  reject(()->PremiumV27StockVariance.describe(2,-1));
  System.out.println("PASS v27.5 observed stock variance, never implicit stock adjustment");
 }
}

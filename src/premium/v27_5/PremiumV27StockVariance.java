package com.vinayak.badminton.nativepreview;

/** Human-readable, non-mutating reconciliation of physical vs recorded stock. */
final class PremiumV27StockVariance {
 private PremiumV27StockVariance(){}
 static String describe(long expected,long counted){
  if(expected<0||counted<0||expected>100_000_000L||counted>100_000_000L)
   throw new IllegalArgumentException("Stock quantities must be independently observed nonnegative counts");
  long delta=Math.subtractExact(counted,expected);
  if(delta==0)return "MATCH: physically counted stock equals recorded stock";
  if(delta<0)return "SHORTFALL: "+(-delta)+" individual shuttles below recorded stock; evidence required";
  return "EXCESS: "+delta+" individual shuttles above recorded stock; evidence required";
 }
}

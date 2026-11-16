package com.vinayak.badminton.nativepreview;
public final class V27CloseReviewSmoke {
 private static void yes(boolean result){if(!result)throw new AssertionError("Close review invariant");}
 private static void reject(Runnable r){boolean blocked=false;try{r.run();}catch(IllegalStateException e){blocked=true;}yes(blocked);}
 public static void main(String[] args){
  String baseline="{\"cash\":1000,\"feeRule\":\"November\",\"physicalCount\":10}";
  String hash=PremiumV27CloseReview.fingerprint(baseline);
  yes(hash.length()==64);
  PremiumV27CloseReview.requireUnchanged(hash,baseline);
  reject(()->PremiumV27CloseReview.requireUnchanged(hash,
    "{\"cash\":1000,\"feeRule\":\"December\",\"physicalCount\":10}"));
  reject(()->PremiumV27CloseReview.requireUnchanged(hash,
    "{\"cash\":1000,\"feeRule\":\"November\",\"physicalCount\":9}"));
  reject(()->PremiumV27CloseReview.requireUnchanged("invalid",baseline));
  System.out.println("PASS v27.6 stale month-end metadata snapshot blocked");
 }
}

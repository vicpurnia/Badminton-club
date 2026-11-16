package com.vinayak.badminton.nativepreview;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Locale;

/** v27.6 review-to-commit snapshot guard; never writes month-end data itself. */
final class PremiumV27CloseReview {
 private PremiumV27CloseReview(){}
 static String fingerprint(String verifiedMetadata){
  if(verifiedMetadata==null)throw new IllegalArgumentException("Verified metadata required");
  try{
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(verifiedMetadata.getBytes(StandardCharsets.UTF_8));
   StringBuilder value=new StringBuilder(64);
   for(byte b:bytes)value.append(String.format(Locale.ROOT,"%02x",b&0xff));
   return value.toString();
  }catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 not available",e);}
 }
 static void requireUnchanged(String reviewFingerprint,String latestMetadata){
  if(reviewFingerprint==null || reviewFingerprint.length()!=64||
     !MessageDigest.isEqual(reviewFingerprint.getBytes(StandardCharsets.US_ASCII),
       fingerprint(latestMetadata).getBytes(StandardCharsets.US_ASCII)))
   throw new IllegalStateException("Ledger/fee/stock metadata changed after month-end review. Reopen review before closing or reopening. No month status changed.");
 }
}

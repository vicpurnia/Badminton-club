package com.vinayak.badminton.nativepreview;

public final class V2796EncryptedPickerSmoke {
 private static void yes(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  PremiumV2796EncryptedPickerGate gate=new PremiumV2796EncryptedPickerGate();
  yes(gate.begin(false),"Read-only backup picker must start");
  yes(!gate.begin(true),"Import intent must not replace active test-only picker");
  PremiumV2796EncryptedPickerGate.Review first=gate.pickerReturned();
  yes(first!=null&&!first.allowImport,"Original TEST-ONLY mode unchanged");
  yes(gate.pickerReturned()==null,"A picker result cannot be claimed twice");
  yes(!gate.begin(true),"Password dialog still owns the picker");
  yes(gate.beginVerification(first.id),"Correct reviewed session verifies");
  yes(!gate.beginVerification(first.id),"One decryption per confirmation");
  yes(!gate.begin(false),"Second picker blocked throughout asynchronous decrypt");
  yes(gate.finish(first.id),"Verification completion releases owner");
  yes(!gate.finish(first.id),"Replayed finish no effect");

  yes(gate.begin(true),"A new import-if-empty picker can start");
  PremiumV2796EncryptedPickerGate.Review second=gate.pickerReturned();
  yes(second!=null&&second.allowImport&&second.id!=first.id,
    "Import session belongs to different token");
  yes(!gate.finish(first.id),"Old dialog cancellation cannot cancel new mode");
  yes(!gate.beginVerification(first.id),"Old dialog cannot decrypt new request");
  yes(gate.finish(second.id),"Cancel password releases new session");
  yes(!gate.active(),"Canceled request does not remain locked");
  yes(gate.begin(false),"New test-only picker may be opened");
  PremiumV2796EncryptedPickerGate.Review third=gate.pickerReturned();
  yes(third!=null&&!third.allowImport,"Default reverts to test-only mode");
  yes(gate.finish(third.id),"Cancellation releases picker");
  System.out.println("PASS v27.9.6 encrypted restore mode bound to one picker and one verification");
 }
}

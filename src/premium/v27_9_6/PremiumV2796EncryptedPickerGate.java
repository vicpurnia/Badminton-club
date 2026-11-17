package com.vinayak.badminton.nativepreview;

/**
 * Tracks a single encrypted recovery picker through password and verification.
 * Prevents a second picker from changing a first picker's TEST-ONLY/IMPORT mode.
 * No password, URI, ledger data or session is persisted.
 */
final class PremiumV2796EncryptedPickerGate {
 static final class Review {
  final long id;
  final boolean allowImport;
  Review(long id,boolean allowImport){this.id=id;this.allowImport=allowImport;}
 }
 private enum State { IDLE,PICKER,PASSWORD,VERIFY }
 private State state=State.IDLE;
 private long sequence=0;
 private Review review;

 synchronized boolean begin(boolean allowImport){
  if(state!=State.IDLE)return false;
  long id=Math.addExact(sequence,1);
  sequence=id;
  review=new Review(id,allowImport);
  state=State.PICKER;
  return true;
 }

 /** Android's single in-flight READ_ENCRYPTED request has returned. */
 synchronized Review pickerReturned(){
  if(state!=State.PICKER||review==null)return null;
  state=State.PASSWORD;
  return review;
 }

 synchronized boolean beginVerification(long token){
  if(state!=State.PASSWORD||review==null||review.id!=token)return false;
  state=State.VERIFY;
  return true;
 }

 /** Idempotent for same session; an old callback cannot close a new session. */
 synchronized boolean finish(long token){
  if(state==State.IDLE||review==null||review.id!=token)return false;
  state=State.IDLE;
  review=null;
  return true;
 }

 synchronized boolean active(){return state!=State.IDLE;}
}

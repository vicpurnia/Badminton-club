package com.vinayak.badminton.nativepreview;

/**
 * A recovery confirmation applies only to the exact independently reviewed
 * backup, never to whichever backup most recently replaced a shared field.
 *
 * The session is single-use. All methods synchronize cross-thread staging
 * (the database executor) and confirmation (the Android UI thread).
 * This class never writes a ledger or decrypts backup content.
 */
final class PremiumV2792ImportGate<T> {
 private T pending;
 private long sequence;
 private long activeSequence;

 synchronized long stage(T reviewed) {
  if(reviewed==null)throw new IllegalArgumentException("Verified backup required");
  sequence=Math.addExact(sequence,1L);
  activeSequence=sequence;
  pending=reviewed;
  return activeSequence;
 }

 synchronized boolean consume(T reviewed,long session) {
  if(reviewed==null||pending!=reviewed||
     session<=0||session!=activeSequence)return false;
  pending=null;
  activeSequence=0;
  return true;
 }

 synchronized boolean cancel(long session) {
  if(pending==null||session<=0||session!=activeSequence)return false;
  pending=null;
  activeSequence=0;
  return true;
 }
}

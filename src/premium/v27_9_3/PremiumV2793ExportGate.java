package com.vinayak.badminton.nativepreview;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One encrypted Android Documents export at a time. A second attempt must
 * not overwrite the cached bytes associated with an open document picker.
 *
 * This gate does not persist passwords or encryption keys.
 */
final class PremiumV2793ExportGate {
 private final AtomicBoolean busy=new AtomicBoolean(false);
 boolean begin(){
  return busy.compareAndSet(false,true);
 }
 void finish(){
  busy.set(false);
 }
 boolean inProgress(){
  return busy.get();
 }
}

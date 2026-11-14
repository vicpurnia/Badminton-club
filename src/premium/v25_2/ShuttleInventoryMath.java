package com.vinayak.badminton.nativepreview;

/** Pure integer stock accounting. One pack holds the user-confirmed number of shuttles. */
final class ShuttleInventoryMath {
 private ShuttleInventoryMath(){}
 static long remaining(long openingShuttles,long shuttlesPerPack,long acquiredPacks,long usedShuttles){
  if(openingShuttles<0 || openingShuttles>100000000L)
   throw new IllegalArgumentException("Opening shuttle count must be between 0 and 100 million.");
  if(shuttlesPerPack<1 || shuttlesPerPack>60)
   throw new IllegalArgumentException("Enter the confirmed shuttle count per pack (1-60).");
  if(acquiredPacks<0 || usedShuttles<0)
   throw new IllegalArgumentException("Acquisitions and usage cannot be negative.");
  return Math.subtractExact(Math.addExact(openingShuttles,Math.multiplyExact(shuttlesPerPack,acquiredPacks)),usedShuttles);
 }
}

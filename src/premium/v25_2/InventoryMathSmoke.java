package com.vinayak.badminton.nativepreview;

/** Tiny plain Java regression tests for signed Android inventory logic. */
public final class InventoryMathSmoke {
 private static void eq(long a,long b){if(a!=b)throw new AssertionError(a+" != "+b);}
 private static void shouldReject(Runnable op){try{op.run();}catch(IllegalArgumentException|ArithmeticException expected){return;}throw new AssertionError("Expected rejection");}
 public static void main(String[] args){
  eq(63,ShuttleInventoryMath.remaining(30,12,4,15));
  eq(0,ShuttleInventoryMath.remaining(0,12,1,12));
  eq(-5,ShuttleInventoryMath.remaining(0,12,1,17)); // Deficits are shown, never hidden
  eq(9,ShuttleInventoryMath.remaining(9,12,0,0));
  shouldReject(()->ShuttleInventoryMath.remaining(0,0,1,0));
  shouldReject(()->ShuttleInventoryMath.remaining(0,61,1,0));
  shouldReject(()->ShuttleInventoryMath.remaining(-1,12,1,0));
  shouldReject(()->ShuttleInventoryMath.remaining(0,12,-1,0));
  shouldReject(()->ShuttleInventoryMath.remaining(0,12,1,-1));
  shouldReject(()->ShuttleInventoryMath.remaining(0,60,Long.MAX_VALUE,0));
  System.out.println("v25.2 stock accounting tests: PASSED");
 }
}

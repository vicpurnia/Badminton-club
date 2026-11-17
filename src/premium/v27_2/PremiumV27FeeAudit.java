package com.vinayak.badminton.nativepreview;

/**
 * v27.2: read-only fee reconciliation. Cash and assessed fees have different
 * accounting identities; this deliberately never reallocates a payment.
 */
final class PremiumV27FeeAudit {
 private PremiumV27FeeAudit(){}
 static final class Result {
  final long received, assigned, unassigned, arrears, surplus;
  final int unconfiguredMonths;
  Result(long received,long assigned,long unassigned,long arrears,long surplus,int unconfiguredMonths){
   this.received=received;this.assigned=assigned;this.unassigned=unassigned;
   this.arrears=arrears;this.surplus=surplus;this.unconfiguredMonths=unconfiguredMonths;
  }
 }
 static Result review(long received,long unassigned,long arrears,long surplus,int unconfiguredMonths) {
  if(received<0 || unassigned<0 || unassigned>received || arrears<0 || surplus<0 || unconfiguredMonths<0)
   throw new IllegalArgumentException("Inconsistent read-only fee reconciliation");
  long assigned=Math.subtractExact(received,unassigned);
  return new Result(received,assigned,unassigned,arrears,surplus,unconfiguredMonths);
 }
 static String headline(Result r){
  return "Cash assigned to assessed months: ₹"+r.assigned+
   " · Cash on unassessed months: ₹"+r.unassigned+
   " · Arrears: ₹"+r.arrears;
 }
 static String caution(Result r){
  if(r.unconfiguredMonths>0)return r.unconfiguredMonths+
   " month(s) have no configured fee; cash for those periods remains unassessed.";
  if(r.unassigned>0)return "Cash recorded for non-billable months is separate from assessed dues.";
  if(r.surplus>0)return "Surplus remains credited to its original fee month, not silently moved.";
  return "No reassignment of original cash or fee periods.";
 }
}

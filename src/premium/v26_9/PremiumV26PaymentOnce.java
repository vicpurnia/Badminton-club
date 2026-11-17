package com.vinayak.badminton.nativepreview;

import java.util.*;

/**
 * A payment form owns one immutable submission ID.
 * Its already-committed ID may be retried after process/UI duplication but
 * must never create a second contribution or second audit event.
 * Two genuinely separate payments use two separate form IDs.
 */
final class PremiumV26PaymentOnce {
 private PremiumV26PaymentOnce(){}
 static boolean alreadyCommitted(List<Contribution> ledger,String submissionId,
    String memberId,String month,long amount,String method){
  if(submissionId==null||!submissionId.startsWith("pay-")||submissionId.length()>128)
   throw new IllegalArgumentException("Missing safe payment submission identity");
  if(memberId==null||memberId.trim().isEmpty()||amount<=0||
     method==null||method.trim().isEmpty())
   throw new IllegalArgumentException("Payment member, amount or method invalid");
  String feeMonth=MigrationCodec.ym(month);
  if(ledger==null)throw new IllegalArgumentException("Payment ledger unavailable");
  int matches=0;
  for(Contribution original:ledger){
   if(!submissionId.equals(original.id))continue;
   matches++;
   if(!"cash".equals(original.kind)||!memberId.equals(original.memberId)||
      !feeMonth.equals(original.feeMonth)||amount!=original.amount||
      !method.equals(original.method))
    throw new IllegalStateException("Payment submission ID already belongs to a different record; no changes made");
  }
  if(matches>1)throw new IllegalStateException("Duplicate original payment ID in ledger");
  return matches==1;
 }
}

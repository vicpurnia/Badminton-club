package com.vinayak.badminton.nativepreview;

import java.util.*;

/**
 * Chronological financial lock. Closing month M protects every prior month's
 * cash/expense roll-forward, not merely entries directly dated M.
 *
 * In the previous code, an OPEN February could be edited after March was
 * CLOSED, silently changing March's opening/closing snapshots.
 */
final class PremiumV26ClosingGuard {
 private PremiumV26ClosingGuard(){}

 static void requireWritable(String rawMetadata,String targetMonth){
  Map<String,MonthlyCloseV258.MonthState> status=MonthlyCloseV258.read(rawMetadata);
  Map<String,Boolean> closed=new HashMap<>();
  for(Map.Entry<String,MonthlyCloseV258.MonthState> e:status.entrySet())
   closed.put(e.getKey(),e.getValue().closed());
  requireWritable(closed,targetMonth);
 }

 static void requireWritable(Map<String,Boolean> closed,String targetMonth){
  String target=MigrationCodec.ym(targetMonth);
  if(closed==null)throw new IllegalArgumentException("Month-close record must be available");
  String earliestBlocking=null;
  for(Map.Entry<String,Boolean> entry:closed.entrySet()){
   if(!Boolean.TRUE.equals(entry.getValue()))continue;
   String month=MigrationCodec.ym(entry.getKey());
   if(month.compareTo(target)>=0 &&
      (earliestBlocking==null||month.compareTo(earliestBlocking)<0))
     earliestBlocking=month;
  }
  if(earliestBlocking!=null){
   if(earliestBlocking.equals(target))
    throw new IllegalStateException("Month "+target+
     " is CLOSED. Reopen it with a recorded audit before changing transactions.");
   throw new IllegalStateException(
    "Month "+target+" rolls forward into CLOSED "+earliestBlocking+
    ". Reopen affected closed months before changing earlier cash, expenses or "+
    "stock entries. No ledger changes were committed.");
  }
 }

 static String projectedImpact(String targetMonth,Map<String,Boolean> closed){
  String target=MigrationCodec.ym(targetMonth);
  ArrayList<String> protectedMonths=new ArrayList<>();
  if(closed!=null)for(Map.Entry<String,Boolean> e:closed.entrySet())
   if(Boolean.TRUE.equals(e.getValue())&&MigrationCodec.ym(e.getKey()).compareTo(target)>=0)
    protectedMonths.add(e.getKey());
  Collections.sort(protectedMonths);
  return protectedMonths.isEmpty()
   ?"No recorded closed months at or after "+target
   :"Changes to "+target+" would affect closed months: "+String.join(", ",protectedMonths);
 }
}

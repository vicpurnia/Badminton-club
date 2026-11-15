package com.vinayak.badminton.nativepreview;

import java.util.*;
import java.text.*;

/**
 * Diagnostic view of cash entries with genuinely recorded receipt dates.
 * Not the club's official fee-period ledger or a bank cash-flow statement:
 * cash corrections are applied to original entries, but refund dates and
 * historical approximate receipt dates are NEVER invented.
 */
final class PremiumCashDateAudit {
 static final class Month {
  final String month;
  long originallyRecorded,effectiveRecognized;
  int entries,corrected;
  Month(String month){this.month=month;}
 }
 static final class Result {
  final List<Month> months;
  final int unknownDateEntries,exactDatedEntries,correctedDatedEntries;
  final long unknownOriginal,unknownEffective;
  Result(List<Month> months,int unknown,int dated,int corrected,
         long unknownOriginal,long unknownEffective){
   this.months=months;unknownDateEntries=unknown;exactDatedEntries=dated;
   correctedDatedEntries=corrected;
   this.unknownOriginal=unknownOriginal;this.unknownEffective=unknownEffective;
  }
 }
 private PremiumCashDateAudit(){}
 private static boolean exactDay(String candidate){
  if(candidate==null||!candidate.matches("\\d{4}-\\d{2}-\\d{2}"))return false;
  SimpleDateFormat date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);
  date.setLenient(false);
  try{return candidate.equals(date.format(date.parse(candidate)));}
  catch(ParseException error){return false;}
 }
 static Result review(String auditedMetadata,List<Contribution> records,String throughMonth){
  String limit=MigrationCodec.ym(throughMonth);
  Map<String,CashCorrectionsV256.State> corrections=
   CashCorrectionsV256.states(auditedMetadata,records);
  TreeMap<String,Month> byDate=new TreeMap<>();
  long undatedOriginal=0L,undatedEffective=0L;
  int undatedCount=0,datedCount=0,correctedCount=0;
  for(Contribution original:records){
   if(!"cash".equals(original.kind))continue; // Donated packs are noncash.
   if(original.amount<=0)throw new IllegalArgumentException("Original cash must be positive");
   CashCorrectionsV256.State state=corrections.get(original.id);
   long effective=state==null?original.amount:state.effective;
   // An imported first-of-month placeholder is not an actual payment date.
   boolean dated=!original.approximate&&exactDay(original.receivedDate);
   if(!dated){
    undatedCount++;
    undatedOriginal=Math.addExact(undatedOriginal,original.amount);
    undatedEffective=Math.addExact(undatedEffective,effective);
    continue;
   }
   String month=original.receivedDate.substring(0,7);
   if(month.compareTo(limit)>0)continue;
   Month row=byDate.get(month);
   if(row==null){row=new Month(month);byDate.put(month,row);}
   row.originallyRecorded=Math.addExact(row.originallyRecorded,original.amount);
   row.effectiveRecognized=Math.addExact(row.effectiveRecognized,effective);
   row.entries++;datedCount++;
   if(state!=null&&state.corrected()){row.corrected++;correctedCount++;}
  }
  return new Result(new ArrayList<>(byDate.values()),undatedCount,datedCount,
   correctedCount,undatedOriginal,undatedEffective);
 }
 static String csv(Result review){
  StringBuilder s=new StringBuilder("Receipt_Date_Month,Original_Recorded_Cash_INR,Currently_Recognized_Cash_INR,Exact_Dated_Payment_Entries,Corrected_Entries,Scope\r\n");
  for(Month row:review.months){
   s.append(row.month).append(',').append(row.originallyRecorded).append(',')
    .append(row.effectiveRecognized).append(',').append(row.entries)
    .append(',').append(row.corrected).append(",RECORDED_DATE\r\n");
  }
  s.append("DATE_UNAVAILABLE,").append(review.unknownOriginal).append(',')
   .append(review.unknownEffective).append(',').append(review.unknownDateEntries)
   .append(",0,NOT_DISTRIBUTED_TO_MONTH\r\n");
  s.append("NOTE,,,,,Current recognized values may reflect later corrections; this is not dated refund cash flow or a bank statement.\r\n");
  return s.toString();
 }
}

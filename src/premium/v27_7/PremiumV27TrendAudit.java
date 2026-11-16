package com.vinayak.badminton.nativepreview;
import java.util.*;

/** Read-only chronological gate for independently exported dashboard trends. */
final class PremiumV27TrendAudit {
 private PremiumV27TrendAudit(){}
 static void requireOrdered(String[] periods){
  if(periods==null||periods.length==0)throw new IllegalArgumentException("Dashboard months unavailable");
  String previous="";
  for(String month:periods){
   if(month==null||!month.matches("[0-9]{4}-(0[1-9]|1[0-2])"))
    throw new IllegalArgumentException("Invalid month in dashboard export");
   if(!previous.isEmpty()&&month.compareTo(previous)<=0)
    throw new IllegalStateException("Dashboard months are duplicated or out of order; export stopped");
   previous=month;
  }
 }
}

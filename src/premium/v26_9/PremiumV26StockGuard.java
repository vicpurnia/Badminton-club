package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.util.*;

/**
 * v26 physical-count protection: never backdate stock-affecting entries across
 * a previously confirmed physical-count anchor. This prevents invisible stock
 * history drift without mutating any original inventory, cash or audit rows.
 */
final class PremiumV26StockGuard {
 static final String COUNT_KEY="vinayakPhysicalCountsV1";
 private PremiumV26StockGuard(){}

 static List<String> countMonths(String raw){
  try{
   JSONObject root=raw==null||raw.trim().isEmpty()?new JSONObject():new JSONObject(raw);
   Object value=root.opt(COUNT_KEY);
   if(value==null||value==JSONObject.NULL)return new ArrayList<>();
   if(!(value instanceof JSONArray))throw new IllegalArgumentException("Physical stock count history is malformed");
   JSONArray records=(JSONArray)value;
   if(records.length()>1000)throw new IllegalArgumentException("Too many physical stock counts");
   ArrayList<String> out=new ArrayList<>();
   String previous="";
   HashSet<String> ids=new HashSet<>();
   for(int i=0;i<records.length();i++){
    JSONObject record=records.getJSONObject(i);
    String month=MigrationCodec.ym(record.getString("month"));
    String id=record.getString("id");
    if(id.trim().isEmpty()||!ids.add(id)||month.compareTo(previous)<=0)
     throw new IllegalArgumentException("Physical count event history has duplicate or unordered events");
    previous=month;out.add(month);
   }
   return out;
  }catch(JSONException ex){
   throw new IllegalArgumentException("Physical stock count history cannot be verified",ex);
  }
 }
 static void requireEditableStockMonth(String raw,String month){
  String selected=MigrationCodec.ym(month);
  for(String count:countMonths(raw)){
   if(selected.compareTo(count)<=0)
    throw new IllegalStateException(
     "Stock for "+selected+" is protected by the physical count at the end of "+count+
     ". Do not backdate a purchase, donation or usage before a confirmed physical count. "+
     "Use a documented new-period adjustment and a new real stock count rather than rewriting history.");
  }
 }
 static void requireEditableOpeningCheckpoint(String raw){
  List<String> counts=countMonths(raw);
  if(!counts.isEmpty())
   throw new IllegalStateException("Opening-stock checkpoint is locked by the "+counts.get(0)+
    " physical count. An arbitrary replacement could invalidate later audited stock. "+
    "Keep the original and use documented reconciliation for a later month.");
 }
}

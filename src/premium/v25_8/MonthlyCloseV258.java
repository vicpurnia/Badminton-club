package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.util.*;

/** Append-only monthly closing ledger inside existing verified inventory metadata. */
final class MonthlyCloseV258 {
 static final String KEY="vinayakMonthlyCloseEventsV1";
 static final class MonthState {
  final String month,action,reason,id;
  final long opening,collections,memberCash,expenses,closing,difference;
  MonthState(String month,String action,String reason,String id,long opening,long collections,
       long memberCash,long expenses,long closing,long difference){
   this.month=month;this.action=action;this.reason=reason;this.id=id;
   this.opening=opening;this.collections=collections;this.memberCash=memberCash;
   this.expenses=expenses;this.closing=closing;this.difference=difference;
  }
  boolean closed(){return "CLOSE".equals(action);}
 }
 private MonthlyCloseV258(){}
 private static JSONObject root(String raw)throws JSONException {
  return raw==null||raw.trim().isEmpty()?new JSONObject():new JSONObject(raw);
 }
 private static JSONArray events(JSONObject obj)throws JSONException {
  Object prop=obj.opt(KEY);
  if(prop==null||prop==JSONObject.NULL)return new JSONArray();
  if(!(prop instanceof JSONArray))throw new IllegalArgumentException("Month-close history is malformed.");
  return (JSONArray)prop;
 }
 static Map<String,MonthState> read(String raw){
  try{
   JSONArray items=events(root(raw));
   if(items.length()>3000)throw new IllegalArgumentException("Month-close event limit exceeded");
   Map<String,MonthState> last=new HashMap<>();
   HashSet<String> unique=new HashSet<>();
   for(int i=0;i<items.length();i++){
    JSONObject item=items.getJSONObject(i);
    String id=item.getString("id"),month=MigrationCodec.ym(item.getString("month"));
    String action=item.getString("action"),reason=item.getString("reason"),at=item.getString("at");
    if(id.isEmpty()||!unique.add(id)||at.isEmpty()||
      !("CLOSE".equals(action)||"REOPEN".equals(action)))
     throw new IllegalArgumentException("Invalid month-close audit event");
    if(reason.length()<8||reason.length()>200)
     throw new IllegalArgumentException("Month-close reason must be 8-200 characters");
    long opening=item.getLong("opening"),col=item.getLong("collections");
    long member=item.getLong("memberCash"),exp=item.getLong("expenses");
    long ending=item.getLong("closing"),delta=item.getLong("difference");
    if(delta!=Math.subtractExact(col,member)||
      ending!=Math.subtractExact(Math.addExact(opening,col),exp))
     throw new IllegalArgumentException("Month-close financial snapshot is inconsistent");
    MonthState previous=last.get(month);
    if((previous!=null&&previous.closed())==("CLOSE".equals(action))||
      (previous==null&&"REOPEN".equals(action)))
     throw new IllegalArgumentException("Illegal month-close/reopen sequence");
    last.put(month,new MonthState(month,action,reason,id,opening,col,member,exp,ending,delta));
   }
   return last;
  }catch(JSONException ex){throw new IllegalArgumentException("Malformed closing history: "+ex.getMessage(),ex);}
 }
 static boolean isClosed(String raw,String month){
  MonthState found=read(raw).get(MigrationCodec.ym(month));
  return found!=null&&found.closed();
 }
 static void requireOpen(String raw,String month){
  if(isClosed(raw,month))
   throw new IllegalStateException("Month "+month+" is CLOSED. Reopen it with an audited reason before adding or correcting transactions.");
 }
 static String record(String raw,String month,String action,String reason,
    String id,String at,ReportMath.Result row){
  String mm=MigrationCodec.ym(month);
  if(row==null||!mm.equals(row.month))throw new IllegalArgumentException("No verified monthly totals to close.");
  String why=reason==null?"":reason.trim();
  if(why.length()<8||why.length()>200||id==null||id.isEmpty()||at==null||at.isEmpty())
   throw new IllegalArgumentException("Closing action requires a reason and audit identity.");
  Map<String,MonthState> status=read(raw);
  boolean closed=status.containsKey(mm)&&status.get(mm).closed();
  if("CLOSE".equals(action)&&closed)throw new IllegalStateException("Month already closed");
  if("REOPEN".equals(action)&&!closed)throw new IllegalStateException("Month is not closed");
  if(!"CLOSE".equals(action)&&!"REOPEN".equals(action))
   throw new IllegalArgumentException("Unsupported month-close action");
  try{
   JSONObject data=root(raw),event=new JSONObject();
   event.put("id",id);event.put("month",mm);event.put("action",action);
   event.put("reason",why);event.put("at",at);
   event.put("opening",row.opening);event.put("collections",row.collections);
   event.put("memberCash",row.memberEntries);event.put("expenses",row.expenses);
   event.put("closing",row.closing);event.put("difference",row.difference);
   JSONArray items=events(data);items.put(event);data.put(KEY,items);
   String next=MigrationCodec.canonical(data);
   read(next);
   return next;
  }catch(JSONException ex){throw new IllegalArgumentException("Could not record month closing",ex);}
 }
}

package com.vinayak.badminton.nativepreview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;

/**
 * Non-destructive accounting corrections. Original Contribution rows stay immutable.
 * Append-only correction chain is in LedgerMeta.inventoryStockJson and verified exports.
 * No Room schema change. Does not permit edits to undated aggregated legacy entries.
 */
final class CashCorrectionsV256 {
 static final String KEY="vinayakCashCorrectionsV1";
 static final class State {
  final long original,effective;
  final String action,reason;
  State(long original,long effective,String action,String reason){
   this.original=original;this.effective=effective;this.action=action;this.reason=reason;
  }
  boolean corrected(){return effective!=original||!"ORIGINAL".equals(action);}
 }
 private static final class Base {
  final String id, member,month;
  final long amount;
  Base(String id,String member,String month,long amount){
   this.id=id;this.member=member;this.month=month;this.amount=amount;
  }
 }
 private CashCorrectionsV256(){}
 private static JSONObject getObject(String raw) throws JSONException {
  return (raw==null||raw.trim().isEmpty())?new JSONObject():new JSONObject(raw);
 }
 private static JSONArray events(JSONObject object) throws JSONException {
  Object property=object.opt(KEY);
  if(property==null || property==JSONObject.NULL)return new JSONArray();
  if(!(property instanceof JSONArray))
   throw new IllegalArgumentException("Cash correction history is not an array");
  return (JSONArray)property;
 }
 private static Map<String,Base> originalRows(List<Contribution> rows){
  Map<String,Base> result=new HashMap<>();
  for(Contribution c:rows){
   if(!"cash".equals(c.kind))continue;
   if(c.amount<=0)throw new IllegalArgumentException("Invalid original cash contribution "+c.id);
   if(result.put(c.id,new Base(c.id,c.memberId,c.feeMonth,c.amount))!=null)
    throw new IllegalArgumentException("Duplicate original cash payment "+c.id);
  }
  return result;
 }
 private static Map<String,Base> originalRows(JSONArray rows) throws JSONException{
  Map<String,Base> result=new HashMap<>();
  for(int i=0;i<rows.length();i++){
   JSONObject c=rows.getJSONObject(i);
   if(!"cash".equals(c.optString("kind","cash")))continue;
   String id=c.getString("id");
   String month=c.optString("feeMonth","");
   if(month.isEmpty()){
    String date=c.optString("date","");
    if(date.length()<7)throw new IllegalArgumentException("Payment lacks a valid fee period "+id);
    month=date.substring(0,7);
   }
   Base item=new Base(id,c.getString("playerId"),MigrationCodec.ym(month),c.getLong("amount"));
   if(item.amount<=0||result.put(id,item)!=null)
    throw new IllegalArgumentException("Invalid or duplicate original cash payment "+id);
  }
  return result;
 }
 private static Map<String,State> parse(JSONObject object,Map<String,Base> records)throws JSONException{
  JSONArray history=events(object);
  if(history.length()>5000)throw new IllegalArgumentException("Too many correction events");
  Map<String,State> latest=new HashMap<>();
  HashSet<String> eventIds=new HashSet<>();
  for(int i=0;i<history.length();i++){
   JSONObject e=history.getJSONObject(i);
   String id=e.getString("eventId"),paymentId=e.getString("paymentId");
   if(id.trim().isEmpty()||!eventIds.add(id))throw new IllegalArgumentException("Duplicate correction event");
   Base original=records.get(paymentId);
   if(original==null)throw new IllegalArgumentException("Correction refers to unknown cash payment "+paymentId);
   if(!original.member.equals(e.getString("memberId"))||
      !original.month.equals(e.getString("feeMonth"))||
      original.amount!=e.getLong("originalAmount"))
    throw new IllegalArgumentException("Correction payment identity mismatch for "+paymentId);
   State preceding=latest.get(paymentId);
   long prior=preceding==null?original.amount:preceding.effective;
   long before=e.getLong("before"),after=e.getLong("after");
   String kind=e.getString("action"),reason=e.getString("reason");
   if(before!=prior||prior<=0||after<0||after>1000000000L||before==after)
    throw new IllegalArgumentException("Invalid correction chain for "+paymentId);
   if(!("CORRECT".equals(kind)||"CANCEL".equals(kind)||"REFUND".equals(kind)))
    throw new IllegalArgumentException("Unknown correction action");
   if("CANCEL".equals(kind)&&after!=0)throw new IllegalArgumentException("Cancellation must zero the original payment");
   if("REFUND".equals(kind)&&after>=before)throw new IllegalArgumentException("Refund must decrease recognized payment");
   if(reason.trim().length()<8||reason.length()>200)
    throw new IllegalArgumentException("Correction must include an 8-200 character reason");
   if(e.getString("at").isEmpty())throw new IllegalArgumentException("Correction timestamp missing");
   latest.put(paymentId,new State(original.amount,after,kind,reason));
  }
  return latest;
 }
 static Map<String,State> states(String json,List<Contribution> rows) {
  try{return parse(getObject(json),originalRows(rows));}
  catch(JSONException e){throw new IllegalArgumentException("Invalid cash corrections: "+e.getMessage(),e);}
 }
 static State stateFor(String json,List<Contribution> rows,Contribution payment){
  if(payment==null||!"cash".equals(payment.kind))
   throw new IllegalArgumentException("Cash receipt not found");
  Map<String,State> latest=states(json,rows);
  return latest.getOrDefault(payment.id,new State(payment.amount,payment.amount,"ORIGINAL",""));
 }
 static Map<String,Long> monthDeltas(String json,List<Contribution> rows){
  Map<String,Base> originals=originalRows(rows);
  try{return deltas(parse(getObject(json),originals),originals);}
  catch(JSONException ex){throw new IllegalArgumentException("Invalid cash correction: "+ex.getMessage(),ex);}
 }
 private static Map<String,Long> deltas(Map<String,State> current,Map<String,Base> originals) {
  Map<String,Long> amounts=new HashMap<>();
  for(Map.Entry<String,State> e:current.entrySet()){
   Base original=originals.get(e.getKey());
   long delta=Math.subtractExact(e.getValue().effective,original.amount);
   amounts.merge(original.month,delta,Math::addExact);
  }
  return amounts;
 }
 static Map<String,Long> monthDeltas(JSONObject ledger) {
  try{
   JSONObject metadata=ledger.optJSONObject("inventoryStock");
   if(metadata==null||!metadata.has(KEY))return new HashMap<>();
   Map<String,Base> records=originalRows(ledger.getJSONArray("payments"));
   return deltas(parse(metadata,records),records);
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid archived correction history: "+ex.getMessage(),ex);}
 }
 static String append(String current,List<Contribution> payments,Contribution selected,long next,
                       String action,String reason,String eventId,String at){
  if(selected==null||!"cash".equals(selected.kind)||next<0||next>1000000000L)
   throw new IllegalArgumentException("Select a recorded cash payment and valid corrected amount");
  String why=reason==null?"":reason.trim();
  if(why.length()<8||why.length()>200)throw new IllegalArgumentException("Reason must be 8-200 characters");
  if(eventId==null||eventId.isEmpty()||at==null||at.isEmpty())
   throw new IllegalArgumentException("Correction audit identity missing");
  if(!("CANCEL".equals(action)||"CORRECT".equals(action)||"REFUND".equals(action)))
   throw new IllegalArgumentException("Unknown correction action");
  Map<String,State> state=states(current,payments);
  State previous=state.getOrDefault(selected.id,new State(selected.amount,selected.amount,"ORIGINAL",""));
  if(previous.effective<=0||previous.effective==next)
   throw new IllegalArgumentException("Already canceled or no amount change");
  if("CANCEL".equals(action)&&next!=0)throw new IllegalArgumentException("Cancellation amount must be zero");
  if("REFUND".equals(action)&&next>=previous.effective)
   throw new IllegalArgumentException("Refund must decrease the recognized amount");
  try{
   JSONObject root=getObject(current);
   JSONArray history=events(root);
   JSONObject record=new JSONObject();
   record.put("eventId",eventId);
   record.put("paymentId",selected.id);
   record.put("memberId",selected.memberId);
   record.put("feeMonth",selected.feeMonth);
   record.put("originalAmount",selected.amount);
   record.put("before",previous.effective);
   record.put("after",next);
   record.put("action",action);
   record.put("reason",why);
   record.put("at",at);
   history.put(record);root.put(KEY,history);
   parse(root,originalRows(payments)); // Validates entire chain before write
   return MigrationCodec.canonical(root); // Keeps confirmed inventory properties
  }catch(JSONException e){throw new IllegalArgumentException("Could not record correction",e);}
 }
}

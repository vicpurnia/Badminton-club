package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * End-of-completed-month physical stock counts are immutable anchors, not guessed
 * corrections to historical purchases. Latest count is the starting point for
 * subsequent fully recorded calendar months. Original checkpoint remains intact.
 */
final class StockReconciliationV257 {
 static final String KEY="vinayakPhysicalCountsV1";
 static final String PACK_PREFIX="PACK_SIZE=";
 static final class Count {
  final String id,month,reason,at;
  final long physical,expected,difference;
  Count(String id,String month,long physical,long expected,long difference,String reason,String at){
   this.id=id;this.month=month;this.physical=physical;this.expected=expected;
   this.difference=difference;this.reason=reason;this.at=at;
  }
 }
 static final class Position {
  final long acquiredPacks,acquiredShuttles,usedShuttles,remainingShuttles;
  final String basis;
  Position(long p,long a,long u,long r,String basis){
   acquiredPacks=p;acquiredShuttles=a;usedShuttles=u;remainingShuttles=r;this.basis=basis;
  }
 }
 private StockReconciliationV257(){}
 private static JSONObject obj(String raw)throws JSONException{
  return raw==null||raw.trim().isEmpty()?new JSONObject():new JSONObject(raw);
 }
 private static JSONArray history(JSONObject j)throws JSONException {
  Object found=j.opt(KEY);
  if(found==null||found==JSONObject.NULL)return new JSONArray();
  if(!(found instanceof JSONArray))throw new IllegalArgumentException("Physical count history must be an array.");
  return (JSONArray)found;
 }
 static long packSize(String note,long fallback) {
  if(fallback<1||fallback>60)throw new IllegalArgumentException("Unconfirmed default pack size.");
  if(note==null||!note.contains(PACK_PREFIX))return fallback;
  int start=note.indexOf(PACK_PREFIX)+PACK_PREFIX.length(),end=start;
  while(end<note.length()&&Character.isDigit(note.charAt(end)))end++;
  if(end==start||end-start>3)throw new IllegalArgumentException("Malformed explicit pack-size note.");
  int factor=Integer.parseInt(note.substring(start,end));
  if(factor<1||factor>60)throw new IllegalArgumentException("Explicit shuttle pack size must be 1-60.");
  return factor;
 }
 static String packNote(long factor,String optional){
  if(factor<1||factor>60)throw new IllegalArgumentException("Pack size must be 1-60.");
  String extra=optional==null?"":optional.trim();
  if(extra.length()>150)throw new IllegalArgumentException("Pack note too long.");
  return PACK_PREFIX+factor+(extra.isEmpty()?"":"; "+extra);
 }
 static List<Count> read(String raw){
  try{
   JSONArray rows=history(obj(raw));
   if(rows.length()>1000)throw new IllegalArgumentException("Too many physical-count records");
   List<Count> result=new ArrayList<>();Set<String> seen=new HashSet<>();
   String priorMonth="";
   for(int i=0;i<rows.length();i++){
    JSONObject x=rows.getJSONObject(i);
    String id=x.getString("id"),month=MigrationCodec.ym(x.getString("month"));
    long counted=x.getLong("physical"),expected=x.getLong("expected"),delta=x.getLong("difference");
    String note=x.getString("reason"),at=x.getString("at");
    if(id.isEmpty()||!seen.add(id)||counted<0||counted>100000000L||
      expected< -100000000L||expected>100000000L||delta!=Math.subtractExact(counted,expected)||
      note.trim().length()<8||note.length()>200||at.isEmpty())
     throw new IllegalArgumentException("Physical stock count history is invalid");
    if(!priorMonth.isEmpty()&&month.compareTo(priorMonth)<=0)
     throw new IllegalArgumentException("Physical count months must be strictly increasing.");
    result.add(new Count(id,month,counted,expected,delta,note,at));priorMonth=month;
   }
   return result;
  }catch(JSONException ex){throw new IllegalArgumentException("Corrupted stock reconciliation history",ex);}
 }
 static Position calculate(String raw,String through,List<Expense> purchases,
   List<Contribution> contributions,List<StockUsage> usage) {
  InventoryCheckpoint.Config checkpoint=InventoryCheckpoint.read(raw);
  if(checkpoint==null)return null;
  String month=MigrationCodec.ym(through);
  if(month.compareTo(checkpoint.month)<0)return null;
  long starting=checkpoint.openingShuttles;
  String from=checkpoint.month;
  String basis="Opening physical count · start of "+checkpoint.month;
  for(Count count:read(raw)){
   if(count.month.compareTo(checkpoint.month)<0)
    throw new IllegalArgumentException("Physical count predates the confirmed baseline.");
   if(count.month.compareTo(month)<=0){
    starting=count.physical;
    from=ReportMath.nextMonth(count.month); // Count is AFTER all transactions in count.month
    basis="Verified month-end physical count · "+count.month;
   }
  }
  long packs=0,shuttles=0,used=0;
  for(Expense row:purchases)if(row.month!=null&&row.month.compareTo(from)>=0&&
    row.month.compareTo(month)<=0&&row.category!=null&&
    row.category.toLowerCase(Locale.ROOT).contains("shuttle")){
   if(row.quantity<0)throw new IllegalArgumentException("Negative acquired packs");
   packs=Math.addExact(packs,row.quantity);
   shuttles=Math.addExact(shuttles,
     Math.multiplyExact(row.quantity,packSize(row.note,checkpoint.perPack)));
  }
  for(Contribution row:contributions)if("packs".equals(row.kind)&&
    row.feeMonth!=null&&row.feeMonth.compareTo(from)>=0&&row.feeMonth.compareTo(month)<=0){
   if(row.amount<0)throw new IllegalArgumentException("Negative donated packs");
   packs=Math.addExact(packs,row.amount);
   shuttles=Math.addExact(shuttles,Math.multiplyExact(row.amount,
     packSize(row.note,checkpoint.perPack)));
  }
  for(StockUsage item:usage)if(item.month!=null&&item.month.compareTo(from)>=0&&
    item.month.compareTo(month)<=0){
   if(item.shuttles<0)throw new IllegalArgumentException("Negative shuttle usage");
   used=Math.addExact(used,item.shuttles);
  }
  return new Position(packs,shuttles,used,
   Math.subtractExact(Math.addExact(starting,shuttles),used),basis);
 }
 static String append(String raw,String month,long physical,String reason,long reviewed,
    String id,String at,List<Expense> expenses,List<Contribution> donated,List<StockUsage> used){
  if(physical<0||physical>100000000L)throw new IllegalArgumentException("Physical count must be 0-100 million");
  String why=reason==null?"":reason.trim();
  if(why.length()<8||why.length()>200)throw new IllegalArgumentException("Physical count reason must be 8-200 characters");
  if(id==null||id.isEmpty()||at==null||at.isEmpty())throw new IllegalArgumentException("Missing physical count audit ID");
  try{
   JSONObject object=obj(raw);
   InventoryCheckpoint.Config checkpoint=InventoryCheckpoint.read(raw);
   String chosen=MigrationCodec.ym(month);
   if(checkpoint==null||chosen.compareTo(checkpoint.month)<0)
    throw new IllegalArgumentException("Opening stock checkpoint must be configured first");
   List<Count> previous=read(raw);
   if(!previous.isEmpty()&&chosen.compareTo(previous.get(previous.size()-1).month)<=0)
    throw new IllegalArgumentException("Only later completed months may be reconciled. Earlier counts remain immutable");
   Position before=calculate(raw,chosen,expenses,donated,used);
   if(before==null||before.remainingShuttles!=reviewed)
    throw new IllegalArgumentException("Recorded inventory has changed since physical count review");
   JSONObject entry=new JSONObject();
   entry.put("id",id);entry.put("month",chosen);entry.put("physical",physical);
   entry.put("expected",reviewed);entry.put("difference",Math.subtractExact(physical,reviewed));
   entry.put("reason",why);entry.put("at",at);
   JSONArray rows=history(object);rows.put(entry);object.put(KEY,rows);
   String updated=MigrationCodec.canonical(object);
   read(updated); // Full history validation
   return updated;
  }catch(JSONException ex){throw new IllegalArgumentException("Could not record physical stock count",ex);}
 }
}

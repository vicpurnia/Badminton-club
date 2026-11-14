package com.vinayak.badminton.nativepreview;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

/** Stock settings in the pre-existing LedgerMeta.inventoryStockJson column. No schema migration. */
final class InventoryCheckpoint {
 static final String FIELD="vinayakStockCheckpointV1";
 private InventoryCheckpoint(){}
 static final class Config {
  final String month,note,confirmedAt;
  final long openingShuttles,perPack;
  Config(String month,long openingShuttles,long perPack,String note,String confirmedAt){
   this.month=month;this.openingShuttles=openingShuttles;this.perPack=perPack;
   this.note=note;this.confirmedAt=confirmedAt;
  }
 }
 static final class Position {
  final long acquiredPacks,usedShuttles,remainingShuttles;
  Position(long p,long u,long r){acquiredPacks=p;usedShuttles=u;remainingShuttles=r;}
 }
 static Config read(String raw){
  if(raw==null||raw.trim().isEmpty())return null;
  try{
   JSONObject object=new JSONObject(raw);
   JSONObject node=object.optJSONObject(FIELD);
   if(!object.has(FIELD))return null;
   if(node==null)throw new IllegalArgumentException("Inventory checkpoint structure is invalid.");
   String month=MigrationCodec.ym(node.getString("effectiveMonth"));
   long opening=node.getLong("openingShuttles");
   long pack=node.getLong("shuttlesPerPack");
   ShuttleInventoryMath.remaining(opening,pack,0,0);
   String note=node.optString("note","");
   if(note.length()>200)throw new IllegalArgumentException("Inventory checkpoint note is too long.");
   String at=node.getString("confirmedAt");
   if(at.trim().isEmpty())throw new IllegalArgumentException("Inventory confirmation time is missing.");
   if(!"manual-start-of-month".equals(node.optString("basis")))
    throw new IllegalArgumentException("Unsupported inventory checkpoint basis.");
   return new Config(month,opening,pack,note,at);
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid inventory checkpoint: "+ex.getMessage(),ex);}
 }
 static String updatedJson(String existing,Config next){
  try{
   JSONObject object=(existing==null||existing.trim().isEmpty())?new JSONObject():new JSONObject(existing);
   JSONObject value=new JSONObject();
   value.put("effectiveMonth",MigrationCodec.ym(next.month));
   value.put("openingShuttles",next.openingShuttles);
   value.put("shuttlesPerPack",next.perPack);
   value.put("note",next.note);
   value.put("confirmedAt",next.confirmedAt);
   value.put("basis","manual-start-of-month");
   ShuttleInventoryMath.remaining(next.openingShuttles,next.perPack,0,0);
   object.put(FIELD,value);
   return MigrationCodec.canonical(object); // Retains any pre-existing inventory fields
  }catch(JSONException ex){throw new IllegalArgumentException("Could not store inventory checkpoint",ex);}
 }
 static Config confirmed(String month,long opening,long perPack,String note){
  String when=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.ROOT).format(new Date());
  Config result=new Config(MigrationCodec.ym(month),opening,perPack,note,when);
  ShuttleInventoryMath.remaining(opening,perPack,0,0);
  if(note!=null&&note.length()>200)throw new IllegalArgumentException("Inventory note is too long.");
  return result;
 }
 static Position calculate(Config checkpoint,String through,
       List<Expense> expenses,List<Contribution> contributions,List<StockUsage> usage){
  if(checkpoint==null)throw new IllegalArgumentException("Opening stock is not configured.");
  String month=MigrationCodec.ym(through);
  if(month.compareTo(checkpoint.month)<0)return null;
  long packs=0,used=0;
  for(Expense row:expenses){
   if(row.month!=null&&row.month.compareTo(checkpoint.month)>=0&&row.month.compareTo(month)<=0
     &&row.category!=null&&row.category.toLowerCase(Locale.ROOT).contains("shuttle")){
    if(row.quantity<0)throw new IllegalArgumentException("Negative purchased pack count detected.");
    packs=Math.addExact(packs,row.quantity);
   }
  }
  for(Contribution row:contributions){
   if("packs".equals(row.kind)&&row.feeMonth!=null
      &&row.feeMonth.compareTo(checkpoint.month)>=0&&row.feeMonth.compareTo(month)<=0){
    if(row.amount<0)throw new IllegalArgumentException("Negative donated pack count detected.");
    packs=Math.addExact(packs,row.amount);
   }
  }
  for(StockUsage row:usage){
   if(row.month!=null&&row.month.compareTo(checkpoint.month)>=0&&row.month.compareTo(month)<=0){
    if(row.shuttles<0)throw new IllegalArgumentException("Negative usage count detected.");
    used=Math.addExact(used,row.shuttles);
   }
  }
  return new Position(packs,used,ShuttleInventoryMath.remaining(
    checkpoint.openingShuttles,checkpoint.perPack,packs,used));
 }
}

package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Read-only full ledger comparison for pre-upgrade external backups.
 * Compares every verified ledger JSON field, not only summary cash balances.
 * A DIFFERENT result is not proof of loss; it can reflect newer authorized
 * entries since the selected snapshot. Never writes to Room or the backup.
 */
final class PremiumV26BackupCompare {
 static final class Comparison {
  final boolean exact;
  final List<String> differences;
  final String currentHash,backupHash;
  Comparison(boolean exact,List<String> changes,String currentHash,String backupHash){
   this.exact=exact;differences=Collections.unmodifiableList(new ArrayList<>(changes));
   this.currentHash=currentHash;this.backupHash=backupHash;
  }
  String summary(){
   if(exact)return "EXACT VERIFIED LEDGER MATCH: Every JSON data field in the selected "+
    "backup matches the current app. No fields, records or metadata differed. "+
    "This is a read-only comparison, not an independent device backup.";
   return "VERIFIED LEDGERS DIFFER in "+differences.size()+" data field(s): "+
    String.join(", ",differences)+
    ". Compare with the time of the backup: newer authorized transactions can "+
    "change the ledger. If no new records were added since the upgrade, "+
    "investigate before relying on the new app. No records were changed.";
  }
 }
 private PremiumV26BackupCompare(){}
 private static String canonical(Object v){
  try{return MigrationCodec.canonical(new JSONObject().put("data",v));}
  catch(JSONException e){throw new IllegalArgumentException("Cannot canonicalize verified ledger value",e);}
 }
 private static String digest(String text){
  try{
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
   StringBuilder out=new StringBuilder();
   for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&0xff));
   return out.toString();
  }catch(Exception e){throw new IllegalStateException("SHA-256 is unavailable",e);}
 }
 static Comparison compare(JSONObject current,JSONObject backup){
  if(current==null||backup==null)throw new IllegalArgumentException("Both verified ledgers are required");
  TreeSet<String> names=new TreeSet<>();
  for(Iterator<String> i=current.keys();i.hasNext();)names.add(i.next());
  for(Iterator<String> i=backup.keys();i.hasNext();)names.add(i.next());
  if(names.size()>500)throw new IllegalArgumentException("Unexpected verified ledger field count");
  List<String> changed=new ArrayList<>();
  for(String key:names){
   if(!current.has(key)||!backup.has(key)||
      !canonical(current.opt(key)).equals(canonical(backup.opt(key))))
    changed.add(key);
  }
  String live=digest(MigrationCodec.canonical(current)),old=digest(MigrationCodec.canonical(backup));
  boolean exact=changed.isEmpty()&&live.equals(old);
  // Fail closed if the canonical whole-object digests disagree even when a
  // future field comparison implementation misses a nested data difference.
  if(!exact&&changed.isEmpty())changed.add("canonical-ledger-content");
  return new Comparison(exact,changed,live,old);
 }
}

package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.util.*;

/** Synthetic, privacy-safe executable v26 regression suite (no real member records). */
public final class V26CoreSmoke {
 static void yes(boolean value,String message){
  if(!value)throw new AssertionError(message);
 }
 static void reject(Runnable task,String message){
  try{task.run();}catch(RuntimeException expected){return;}
  throw new AssertionError("Expected rejection: "+message);
 }
 static Contribution payment(String id,String month,long amount){
  Contribution c=new Contribution();
  c.id=id;c.memberId="synthetic-A";c.feeMonth=month;c.kind="cash";c.amount=amount;
  return c;
 }
 public static void main(String[] args)throws Exception{
  String data="{}";Set<String> known=new HashSet<>(Arrays.asList("synthetic-A","synthetic-B"));
  Contribution c=payment("cash-1","2026-02",2000);
  Contribution another=payment("cash-2","2026-03",900);
  List<Contribution> payments=Arrays.asList(c,another);
  yes(PremiumV26State.policies(data,known).isEmpty(),"Empty policy should be unconfigured");
  data=PremiumV26State.addPolicy(data,"ALL","2026-02",2000,"Agreed club annual rate",
   "fee-1","2026-11-14",known);
  data=PremiumV26State.addPolicy(data,"synthetic-A","2026-03",1800,"Individual approved fee rate",
   "fee-2","2026-11-14",known);
  yes(PremiumV26State.rateFor(PremiumV26State.policies(data,known),"synthetic-A","2026-03")==1800,
   "Member rate must supersede global rate");
  yes(PremiumV26State.rateFor(PremiumV26State.policies(data,known),"synthetic-B","2026-03")==2000,
   "Other member keeps global fee");
  String afterWaiver=PremiumV26State.addWaiver(data,"synthetic-A","2026-03",300,
   "Approved member concession","waiver-1","2026-11-14",known);
  data=CashCorrectionsV256.append(afterWaiver,payments,c,1500,"CORRECT",
   "Verified deposited cash","fix-1","2026-11-14");
  data=CashCorrectionsV256.append(data,payments,c,1200,"REFUND",
   "Actual partial cash returned","fix-2","2026-11-14");
  data=CashCorrectionsV256.append(data,payments,c,0,"CANCEL",
   "Original transaction invalid","fix-3","2026-11-14");
  final String canceled=data;
  reject(()->CashCorrectionsV256.append(canceled,payments,c,100,"CORRECT",
   "Illegal edit after cancel","fix-4","2026-11-14"),"Canceled entry cannot be adjusted");
  data=CashCorrectionsV256.append(data,payments,c,1000,"RESTORE",
   "Prior cancellation mistaken","fix-5","2026-11-14");
  yes(CashCorrectionsV256.stateFor(data,payments,c).effective==1000,
   "RESTORE must be audited, original payment unchanged");
  yes(c.amount==2000,"Original cash immutable after correction chain");
  yes(CashCorrectionsV256.monthDeltas(data,payments).get("2026-02")==-1000,
   "Restored cash delta must reconcile exactly");
  PremiumV26State.FeeSummary totals=PremiumV26State.feeSummary(
   data,"synthetic-A","2026-02","2026-03",payments);
  yes(totals.billed==3800,"Assessed fees equal configured amounts");
  yes(totals.waived==300,"Approved noncash concession deducted from dues");
  yes(totals.paid==1900,"Fees use corrected cash not original amounts");
  yes(totals.arrears==1600,"Outstanding dues do not invent collected cash");
  data=PremiumV26State.addProfile(data,"synthetic-A","Synthetic A renamed","123","","Test profile",false,
   "profile-1","2026-11-14",known);
  yes(!PremiumV26State.profiles(data,known).get("synthetic-A").active,
   "Archiving must retain immutable member identity");
  yes(PremiumV26State.billableMonth(data,"synthetic-A","2026-03"),"Earlier owed months must survive archiving");
  yes(!PremiumV26State.billableMonth(data,"synthetic-A","2027-02"),"Future months must not bill archived members");
  final String completed=data;
  reject(()->PremiumV26State.addPolicy(completed,"synthetic-unknown","2026-12",500,
   "Unknown member blocked","fee-3","2026-11-14",known),"Unknown member");
  reject(()->PremiumV26State.addPolicy(completed,"ALL","2026-02",-1,
   "Negative fee forbidden","fee-4","2026-11-14",known),"Negative fee");
  reject(()->PremiumV26State.addWaiver(completed,"synthetic-A","2026-03",0,
   "Zero concession invalid","waiver-2","2026-11-14",known),"Zero waiver");
  yes(PremiumV26State.reorderThreshold(completed)==-1,"No implicit reorder policy");
  data=PremiumV26State.setReorder(data,12,"Purchase when fewer than twelve shuttles",
   "threshold-1","2026-11-14");
  yes(PremiumV26State.reorderThreshold(data)==12,"Inventory threshold preserved");
  PremiumV26State.validate(data,known);

  // Encryption test uses a synthetic verified ledger and never touches a real database.
  String json="{\"verified\":true,\"synthetic\":\"fee-1\"}";
  char[] password="synthetic-secure-password-2026".toCharArray();
  String encrypted=PremiumBackupCrypto.encrypt(json,password);
  yes(!encrypted.contains("fee-1"),"Ciphertext must not expose a sample member payment");
  yes(PremiumBackupCrypto.decrypt(encrypted,password).equals(json),
   "AES-GCM authenticated roundtrip");
  try{PremiumBackupCrypto.decrypt(encrypted,"incorrect-password".toCharArray());
   throw new AssertionError("Wrong password should reject encrypted backup");}
  catch(IllegalArgumentException expected){}
  JSONObject damaged=new JSONObject(encrypted);
  damaged.put("plainBytes",damaged.getInt("plainBytes")+1);
  try{PremiumBackupCrypto.decrypt(damaged.toString(),password);
   throw new AssertionError("Modified encrypted file should not authenticate");}
  catch(IllegalArgumentException expected){}
  Arrays.fill(password,'\u0000');
  System.out.println("v26 synthetic fee / correction / archive / inventory / encrypted-backup regression: PASSED");
 }
}

/* Minimal compile-time stubs: tests exercise the real production v26 logic. */
final class Contribution {
 String id,memberId,feeMonth,kind;long amount;
}
final class MigrationCodec {
 static String ym(String s){
  if(s==null||!s.matches("\\d{4}-(0[1-9]|1[0-2])"))
   throw new IllegalArgumentException("Invalid month: "+s);
  return s;
 }
 static String canonical(JSONObject j){return j.toString();}
 static Object inspect(String s){
  if(s==null||!s.contains("\"verified\":true"))
   throw new IllegalArgumentException("Synthetic verified JSON required");
  return s;
 }
}
final class ReportMath {
 static final class Result {
  String month;long opening,collections,memberEntries,expenses,closing,difference;
 }
}
final class MonthlyCloseV258 {
 static final class MonthState {
  String month;long opening,collections,memberCash,expenses,closing,difference;
  boolean closed(){return true;}
 }
 static Map<String,MonthState> read(String s){return new HashMap<>();}
}

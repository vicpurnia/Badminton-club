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
  // A later lower fee rate cannot silently discard an authorized waiver.
  reject(()->PremiumV26State.addPolicy(afterWaiver,"synthetic-A","2026-03",200,
   "Proposed rate lower than approved concession","fee-unsafe","2026-11-14",known),
   "Revised fee may not be lower than cumulative approved concession");
  // Separate concessions must not collectively exceed a member's monthly rate.
  final String alreadyReduced=afterWaiver;
  reject(()->PremiumV26State.addWaiver(alreadyReduced,"synthetic-A","2026-03",1600,
   "Second concession exceeds effective rate","waiver-unsafe","2026-11-14",known),
   "Cumulative concessions cannot exceed the effective policy rate");
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

  // Fee status must account for authorized concessions, not the gross fee rate.
  PremiumV26State.FeeSummary march=PremiumV26State.feeSummary(
   data,"synthetic-A","2026-03","2026-03",payments);
  yes(march.billed==1800&&march.waived==300&&march.paid==900,
   "March displays gross assessed fee, waiver and actual cash separately");
  yes("Partial".equals(PremiumV26State.monthStatus(march,true)),
   "Member with 900 received against 1500 net due is partial");
  Contribution remainder=payment("cash-3","2026-03",600);
  List<Contribution> paidMarch=Arrays.asList(c,another,remainder);
  PremiumV26State.FeeSummary fullySettled=PremiumV26State.feeSummary(
   data,"synthetic-A","2026-03","2026-03",paidMarch);
  yes("Paid".equals(PremiumV26State.monthStatus(fullySettled,true)),
   "Concession plus net cash fully settles fee even when cash is below gross rate");
  PremiumV26State.FeeSummary unconfigured=PremiumV26State.feeSummary(
   data,"synthetic-A","2026-01","2026-01",payments);
  yes("Unconfigured".equals(PremiumV26State.monthStatus(unconfigured,true)),
   "Missing historical fee rate never implies zero due or Paid");
  PremiumV26State.FeeSummary zeroAssessed=new PremiumV26State.FeeSummary();
  zeroAssessed.configuredMonths=1;
  yes("Settled".equals(PremiumV26State.monthStatus(zeroAssessed,true)),
   "Fee fully waived or set to zero must never be labeled as cash Paid");
  yes("Not due".equals(PremiumV26State.monthStatus(fullySettled,false)),
   "Member not billable must not appear as unpaid");

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

  // New member starts at the actual registration month and must not accrue old fees.
  String newlyJoined=PremiumV26State.addNewMemberOrigin("{}","synthetic-B","New member",
   "2026-11","origin-1","2026-11-14",known);
  yes(!PremiumV26State.billableMonth(newlyJoined,"synthetic-B","2026-03"),
   "New November member must not inherit the historic March fee");
  yes(PremiumV26State.billableMonth(newlyJoined,"synthetic-B","2026-11"),
   "New member is billable beginning in verified joining month");
  final String immutableOrigin=newlyJoined;
  reject(()->PremiumV26State.addNewMemberOrigin(immutableOrigin,"synthetic-B","New member",
   "2026-01","origin-2","2026-11-14",known),
   "New member billing-start event may not be silently rewritten");
  // A later profile entry cannot retroactively erase earlier assessed fees.
  String laterProfile=PremiumV26State.addProfile(data,"synthetic-B",
   "Imported member with actual joining-date note","","2026-10-15",
   "Historical joining date added today",true,"profile-2","2026-11-14",known);
  yes(PremiumV26State.billableMonth(laterProfile,"synthetic-B","2026-03"),
   "Adding a joining-date note cannot reclassify previously assessed months");

  // A physical stock anchor cannot be retroactively invalidated by newly inserted
  // purchase, donated pack, shuttle usage or arbitrary opening-checkpoint edits.
  String physicallyCounted=new JSONObject().put(PremiumV26StockGuard.COUNT_KEY,
   new JSONArray().put(new JSONObject().put("id","count-1").put("month","2026-05"))).toString();
  PremiumV26StockGuard.requireEditableStockMonth(physicallyCounted,"2026-06");
  final String protectedCounts=physicallyCounted;
  reject(()->PremiumV26StockGuard.requireEditableStockMonth(protectedCounts,"2026-05"),
   "Counted month cannot accept backdated stock changes");
  reject(()->PremiumV26StockGuard.requireEditableStockMonth(protectedCounts,"2026-04"),
   "Previous period cannot accept backdated stock changes");
  reject(()->PremiumV26StockGuard.requireEditableOpeningCheckpoint(protectedCounts),
   "Confirmed physical count prevents changing opening checkpoint");
  PremiumV26StockGuard.requireEditableOpeningCheckpoint("{}");
  reject(()->PremiumV26StockGuard.countMonths(
   new JSONObject().put(PremiumV26StockGuard.COUNT_KEY,
    new JSONArray().put(new JSONObject().put("id","c").put("month","2026-07"))
                   .put(new JSONObject().put("id","c").put("month","2026-08"))).toString()),
   "Duplicated physical count event ID must fail closed");

  // Import must reject fee/profile/waiver events that reference absent member IDs.
  String orphanPolicy=new JSONObject().put(PremiumV26State.FEE_KEY,new JSONArray().put(
   new JSONObject().put("id","orphan-fee").put("scope","not-a-member")
    .put("month","2026-11").put("rate",1000)
    .put("reason","Unknown identity forbidden").put("at","2026-11-14"))).toString();
  reject(()->PremiumV26State.validate(orphanPolicy,known),
   "Orphan fee policy must never be imported");
  String orphanConcession=new JSONObject().put(PremiumV26State.WAIVER_KEY,new JSONArray().put(
   new JSONObject().put("id","orphan-waiver").put("member","not-a-member")
    .put("month","2026-11").put("amount",100)
    .put("reason","Unknown concession owner").put("at","2026-11-14"))).toString();
  reject(()->PremiumV26State.validate(orphanConcession,known),
   "Orphan waiver must never be imported");
  String orphanProfile=new JSONObject().put(PremiumV26State.PROFILE_KEY,new JSONArray().put(
   new JSONObject().put("id","orphan-profile").put("member","not-a-member")
    .put("name","Unknown member").put("joined","").put("active",true)
    .put("at","2026-11-14"))).toString();
  reject(()->PremiumV26State.validate(orphanProfile,known),
   "Orphan member profile must never be imported");

  // A payment's FEE MONTH is not necessarily the date that cash was received.
  // Imported day-of-month placeholders must remain in an explicit undated bucket.
  c.receivedDate="2026-11-10";c.approximate=false;
  another.receivedDate="2026-03-01";another.approximate=true;
  PremiumCashDateAudit.Result byReceiptDate=PremiumCashDateAudit.review(
    data,payments,"2026-11");
  yes(byReceiptDate.months.size()==1 &&
      "2026-11".equals(byReceiptDate.months.get(0).month),
      "Payment dated November cannot be assigned to February simply because of feeMonth");
  yes(byReceiptDate.months.get(0).originallyRecorded==2000 &&
      byReceiptDate.months.get(0).effectiveRecognized==1000 &&
      byReceiptDate.months.get(0).corrected==1,
      "Receipt-date evidence displays original and correction-adjusted values separately");
  yes(byReceiptDate.unknownDateEntries==1 &&
      byReceiptDate.unknownOriginal==900 &&
      byReceiptDate.unknownEffective==900,
      "Approximate imported first-day dates never count as known receipt dates");
  yes(PremiumCashDateAudit.csv(byReceiptDate).contains("DATE_UNAVAILABLE"),
      "CSV must preserve unknown dates in a distinct bucket");
  another.approximate=false;another.receivedDate="2026-02-30";
  yes(PremiumCashDateAudit.review(data,payments,"2026-11").unknownDateEntries==1,
      "Impossible dates must never be placed on financial timeline");
  another.approximate=true;another.receivedDate="2026-03-01";

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
 String id,memberId,feeMonth,kind,receivedDate;long amount;boolean approximate;
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

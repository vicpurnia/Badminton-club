package com.vinayak.badminton.nativepreview;

import org.json.*;
import java.util.*;

/**
 * v26 additive, auditable metadata. Never changes original Room rows, imported
 * historical totals or v25 JSON properties. All money is integer INR.
 * The existing inventoryStockJson is included in verified JSON exports.
 */
final class PremiumV26State {
 static final String FEE_KEY="vinayakFeePoliciesV26";
 static final String WAIVER_KEY="vinayakFeeWaiversV26";
 static final String PROFILE_KEY="vinayakMemberProfilesV26";
 static final String STOCK_KEY="vinayakStockThresholdsV26";
 static final int MAX_EVENTS=5000;
 private PremiumV26State(){}

 static final class FeePolicy {
  final String id,scope,month,reason,at;
  final long rate;
  FeePolicy(String id,String scope,String month,long rate,String reason,String at){
   this.id=id;this.scope=scope;this.month=month;this.rate=rate;this.reason=reason;this.at=at;
  }
 }
 static final class Waiver {
  final String id,member,month,reason,at;
  final long amount;
  Waiver(String id,String member,String month,long amount,String reason,String at){
   this.id=id;this.member=member;this.month=month;this.amount=amount;this.reason=reason;this.at=at;
  }
 }
 static final class Profile {
  final String member,name,contact,joined,notes,at;
  final boolean active;
  Profile(String member,String name,String contact,String joined,String notes,boolean active,String at){
   this.member=member;this.name=name;this.contact=contact;this.joined=joined;
   this.notes=notes;this.active=active;this.at=at;
  }
 }
 static final class FeeSummary {
  long billed,waived,paid,arrears,excess,unallocated;
  int configuredMonths,unconfiguredMonths;
  String label(){
   return "Amount due ₹"+arrears+"; cash allocated to these fee months ₹"+paid+
    "; concessions ₹"+waived+"; excess ₹"+excess+
    (unconfiguredMonths>0?"; "+unconfiguredMonths+" month(s) without configured fee policy":"");
  }
 }
 static String monthStatus(FeeSummary assessment,boolean billable){
  if(assessment==null)throw new IllegalArgumentException("Fee assessment not available");
  if(!billable)return "Not due";
  if(assessment.unconfiguredMonths>0)return "Unconfigured";
  if(assessment.configuredMonths!=1)
   throw new IllegalArgumentException("A monthly payment status requires exactly one assessed month");
  long net=Math.subtractExact(assessment.billed,assessment.waived);
  if(net==0)return "Settled"; // Completely waived or zero fee: not a cash receipt
  if(assessment.paid>=net)return "Paid";
  return assessment.paid>0?"Partial":"Unpaid";
 }
 private static JSONObject root(String json)throws JSONException {
  return json==null||json.trim().isEmpty()?new JSONObject():new JSONObject(json);
 }
 private static JSONArray entries(JSONObject data,String key)throws JSONException{
  Object value=data.opt(key);
  if(value==null||value==JSONObject.NULL)return new JSONArray();
  if(!(value instanceof JSONArray))throw new IllegalArgumentException(key+" must be an array");
  JSONArray a=(JSONArray)value;
  if(a.length()>MAX_EVENTS)throw new IllegalArgumentException(key+" exceeds permitted event count");
  return a;
 }
 private static void checkReason(String reason){
  if(reason==null||reason.trim().length()<8||reason.length()>200)
   throw new IllegalArgumentException("A meaningful reason of 8–200 characters is required");
 }
 private static void checkId(String id,String at){
  if(id==null||id.trim().isEmpty()||id.length()>120||at==null||at.trim().isEmpty()||at.length()>90)
   throw new IllegalArgumentException("Missing or invalid audit identity");
 }
 private static String validMember(String member,Set<String> known){
  if(member==null||member.trim().isEmpty())throw new IllegalArgumentException("Member identity required");
  if(known!=null&&!known.contains(member))throw new IllegalArgumentException("Unknown member identity");
  return member;
 }
 static List<FeePolicy> policies(String json,Set<String> known){
  try{
   JSONArray a=entries(root(json),FEE_KEY);List<FeePolicy> out=new ArrayList<>();
   Set<String> ids=new HashSet<>();
   for(int i=0;i<a.length();i++){
    JSONObject e=a.getJSONObject(i);
    String id=e.getString("id"),at=e.getString("at"),scope=e.getString("scope");
    checkId(id,at);if(!ids.add(id))throw new IllegalArgumentException("Duplicate fee policy event");
    if(!"ALL".equals(scope))validMember(scope,known);
    String month=MigrationCodec.ym(e.getString("month")),why=e.getString("reason");
    long rate=e.getLong("rate");
    checkReason(why);
    if(rate<0||rate>100000000L)throw new IllegalArgumentException("Monthly fee rate is out of range");
    out.add(new FeePolicy(id,scope,month,rate,why,at));
   }
   return out;
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid fee policy metadata",ex);}
 }
 static List<Waiver> waivers(String json,Set<String> known){
  try{
   JSONArray a=entries(root(json),WAIVER_KEY);List<Waiver> out=new ArrayList<>();
   Set<String> ids=new HashSet<>();
   for(int i=0;i<a.length();i++){
    JSONObject e=a.getJSONObject(i);
    String id=e.getString("id"),at=e.getString("at");
    checkId(id,at);if(!ids.add(id))throw new IllegalArgumentException("Duplicate waiver event");
    String member=validMember(e.getString("member"),known);
    String month=MigrationCodec.ym(e.getString("month")),why=e.getString("reason");
    long amount=e.getLong("amount");
    checkReason(why);
    if(amount<=0||amount>100000000L)throw new IllegalArgumentException("Invalid waiver amount");
    out.add(new Waiver(id,member,month,amount,why,at));
   }
   return out;
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid waiver metadata",ex);}
 }
 static Map<String,Profile> profiles(String json,Set<String> known){
  try{
   JSONArray a=entries(root(json),PROFILE_KEY);Map<String,Profile> result=new HashMap<>();
   Set<String> ids=new HashSet<>();
   Set<String> memberOrigins=new HashSet<>();
   for(int i=0;i<a.length();i++){
    JSONObject e=a.getJSONObject(i);
    String id=e.getString("id"),at=e.getString("at");
    checkId(id,at);if(!ids.add(id))throw new IllegalArgumentException("Duplicate profile update");
    String member=validMember(e.getString("member"),known);
    String name=e.getString("name").trim(),contact=e.optString("contact","");
    String joined=e.optString("joined",""),notes=e.optString("notes","");
    String effective=e.optString("statusFrom","");
    if(!effective.isEmpty())MigrationCodec.ym(effective);
    String origin=e.optString("memberStartMonth","");
    if(!origin.isEmpty()){
     MigrationCodec.ym(origin);
     if(!memberOrigins.add(member))
      throw new IllegalArgumentException("A member may have only one original billing-start event");
     if(!origin.equals(effective))
      throw new IllegalArgumentException("Original billing-start event must take effect in its creation month");
    }
    if(name.isEmpty()||name.length()>100||contact.length()>80||joined.length()>10||notes.length()>400)
     throw new IllegalArgumentException("Profile field length out of bounds");
    if(!joined.isEmpty()){
     if(!joined.matches("\\d{4}-\\d{2}-\\d{2}"))
      throw new IllegalArgumentException("Joining date must be YYYY-MM-DD");
     java.text.SimpleDateFormat parser=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);
     parser.setLenient(false);
     try{
      java.util.Date date=parser.parse(joined);
      if(date==null||!joined.equals(parser.format(date)))
       throw new IllegalArgumentException("Joining date must be a valid calendar day");
     }catch(java.text.ParseException err){
      throw new IllegalArgumentException("Joining date must be a valid calendar day",err);
     }
    }
    result.put(member,new Profile(member,name,contact,joined,notes,e.getBoolean("active"),at));
   }
   return result;
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid member profile metadata",ex);}
 }
 static long reorderThreshold(String json){
  try{
   JSONArray a=entries(root(json),STOCK_KEY);
   long value=-1;Set<String> ids=new HashSet<>();
   for(int i=0;i<a.length();i++){
    JSONObject e=a.getJSONObject(i);
    String id=e.getString("id"),at=e.getString("at");
    checkId(id,at);if(!ids.add(id))throw new IllegalArgumentException("Duplicate inventory threshold event");
    checkReason(e.getString("reason"));
    value=e.getLong("shuttles");
    if(value<0||value>1000000L)throw new IllegalArgumentException("Invalid reorder threshold");
   }
   return value;
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid stock threshold metadata",ex);}
 }
 static void validate(String json,Set<String> known){
  List<FeePolicy> configured=policies(json,known);
  List<Waiver> concessions=waivers(json,known);
  profiles(json,known);reorderThreshold(json);
  // Never silently truncate an already-authorized concession if a fee rule
  // is later lowered or removed. This must fail before committing a new rule,
  // and must also fail when inspecting a tampered migration JSON.
  HashMap<String,Long> total=new HashMap<>();
  for(Waiver item:concessions){
   String key=item.member+"\\u0000"+item.month;
   long sum=total.merge(key,item.amount,Math::addExact);
   long fee=rateFor(configured,item.member,item.month);
   if(fee<0||sum>fee)
    throw new IllegalArgumentException("Audited concession for "+item.member+
     " in "+item.month+" exceeds the effective fee. Review the policy instead of hiding the difference.");
  }
 }
 static long rateFor(List<FeePolicy> all,String member,String month){
  String mm=MigrationCodec.ym(month);long rate=-1;
  String globalMonth="",memberMonth="";
  // Later append-only event wins for the same scope and effective month.
  for(FeePolicy e:all)if(e.month.compareTo(mm)<=0){
   if("ALL".equals(e.scope)&&e.month.compareTo(globalMonth)>=0){globalMonth=e.month;rate=e.rate;}
  }
  for(FeePolicy e:all)if(member.equals(e.scope)&&e.month.compareTo(mm)<=0){
   if(e.month.compareTo(memberMonth)>=0){memberMonth=e.month;}
  }
  if(!memberMonth.isEmpty()){
   // Member-specific policy has priority until a later member-specific change.
   for(FeePolicy e:all)if(member.equals(e.scope)&&memberMonth.equals(e.month))rate=e.rate;
  }
  return rate;
 }
 static boolean billableMonth(String raw,String member,String month){
  try{
   String selected=MigrationCodec.ym(month);
   JSONArray history=entries(root(raw),PROFILE_KEY);
   boolean active=true;
   String joined="",firstBillable="";
   for(int i=0;i<history.length();i++){
    JSONObject e=history.getJSONObject(i);
    if(!member.equals(e.getString("member")))continue;
    String created=e.optString("memberStartMonth","");
    if(!created.isEmpty()){
     String monthOrigin=MigrationCodec.ym(created);
     if(!firstBillable.isEmpty())
      throw new IllegalArgumentException("Duplicate membership billing origin");
     firstBillable=monthOrigin;
    }
    String effective=e.optString("statusFrom","");
    if(!effective.isEmpty()&&MigrationCodec.ym(effective).compareTo(selected)<=0){
     String entered=e.optString("joined","");
     if(!entered.isEmpty())joined=entered;
     active=e.getBoolean("active");
    }
   }
   return (firstBillable.isEmpty()||selected.compareTo(firstBillable)>=0)
       &&active&&(joined.isEmpty()||joined.substring(0,7).compareTo(selected)<=0);
  }catch(JSONException ex){throw new IllegalArgumentException("Invalid membership status history",ex);}
 }
 static FeeSummary feeSummary(String raw,String member,String from,String through,
                             List<Contribution> payments){
  String start=MigrationCodec.ym(from),end=MigrationCodec.ym(through);
  if(start.compareTo(end)>0)throw new IllegalArgumentException("Start is after ending month");
  List<FeePolicy> rules=policies(raw,null);List<Waiver> reductions=waivers(raw,null);
  Map<String,CashCorrectionsV256.State> corrections=CashCorrectionsV256.states(raw,payments);
  Map<String,Long> cash=new HashMap<>(),credits=new HashMap<>();
  for(Contribution c:payments)if(member.equals(c.memberId)&&"cash".equals(c.kind)){
   long effective=corrections.containsKey(c.id)?corrections.get(c.id).effective:c.amount;
   cash.merge(MigrationCodec.ym(c.feeMonth),effective,Math::addExact);
  }
  for(Waiver w:reductions)if(member.equals(w.member)){
   credits.merge(w.month,w.amount,Math::addExact);
  }
  FeeSummary result=new FeeSummary();
  String month=start;int safety=0;
  while(month.compareTo(end)<=0){
   if(++safety>240)throw new IllegalArgumentException("Maximum fee history review is 240 months");
   long rate=rateFor(rules,member,month),paid=cash.getOrDefault(month,0L);
   result.paid=Math.addExact(result.paid,paid);
   if(!billableMonth(raw,member,month)){
    result.unallocated=Math.addExact(result.unallocated,paid);
   }else if(rate<0){
    result.unconfiguredMonths++;result.unallocated=Math.addExact(result.unallocated,paid);
   }else{
    result.configuredMonths++;
    long waived=credits.getOrDefault(month,0L);
    if(waived<0||waived>rate)
     throw new IllegalStateException("Authorized concession exceeds the configured fee for "+month);
    long owed=Math.subtractExact(rate,waived);
    result.billed=Math.addExact(result.billed,rate);
    result.waived=Math.addExact(result.waived,waived);
    result.arrears=Math.addExact(result.arrears,Math.max(0L,owed-paid));
    result.excess=Math.addExact(result.excess,Math.max(0L,paid-owed));
   }
   String[] b=month.split("-");int y=Integer.parseInt(b[0]),m=Integer.parseInt(b[1])+1;
   if(m==13){m=1;y++;}
   month=String.format(Locale.ROOT,"%04d-%02d",y,m);
  }
  return result;
 }
 private static String add(String raw,String key,JSONObject event){
  try{
   JSONObject data=root(raw);
   JSONArray a=entries(data,key);
   a.put(event);data.put(key,a);
   return MigrationCodec.canonical(data);
  }catch(JSONException ex){throw new IllegalArgumentException("Could not record audited change",ex);}
 }
 static String addPolicy(String raw,String scope,String month,long rate,String reason,
                         String id,String at,Set<String> known){
  checkId(id,at);checkReason(reason);
  if(!"ALL".equals(scope))validMember(scope,known);
  if(rate<0||rate>100000000L)throw new IllegalArgumentException("Invalid fee rate");
  try{
   JSONObject e=new JSONObject().put("id",id).put("scope",scope)
    .put("month",MigrationCodec.ym(month)).put("rate",rate)
    .put("reason",reason.trim()).put("at",at);
   String updated=add(raw,FEE_KEY,e);validate(updated,known);return updated;
  }catch(JSONException ex){throw new IllegalArgumentException("Cannot save fee policy",ex);}
 }
 static String addWaiver(String raw,String member,String month,long amount,String reason,
                         String id,String at,Set<String> known){
  checkId(id,at);checkReason(reason);validMember(member,known);
  if(amount<=0||amount>100000000L)throw new IllegalArgumentException("Invalid concession");
  try{
   JSONObject e=new JSONObject().put("id",id).put("member",member)
    .put("month",MigrationCodec.ym(month)).put("amount",amount)
    .put("reason",reason.trim()).put("at",at);
   String updated=add(raw,WAIVER_KEY,e);validate(updated,known);return updated;
  }catch(JSONException ex){throw new IllegalArgumentException("Cannot save waiver",ex);}
 }
 static String addProfile(String raw,String member,String name,String contact,String joined,String notes,
                          boolean active,String id,String at,Set<String> known){
  checkId(id,at);validMember(member,known);
  try{
   JSONObject e=new JSONObject().put("id",id).put("member",member)
    .put("name",name==null?"":name.trim()).put("contact",contact==null?"":contact.trim())
    .put("joined",joined==null?"":joined.trim()).put("notes",notes==null?"":notes.trim())
    .put("active",active).put("at",at)
    .put("statusFrom",new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new java.util.Date()));
   String updated=add(raw,PROFILE_KEY,e);validate(updated,known);return updated;
  }catch(JSONException ex){throw new IllegalArgumentException("Cannot update member profile",ex);}
 }
 /** Records the first billed month for a newly created member without rewriting old ledger rows. */
 static String addNewMemberOrigin(String raw,String member,String name,String startMonth,
                                  String id,String at,Set<String> known){
  checkId(id,at);validMember(member,known);
  String beginning=MigrationCodec.ym(startMonth);
  if(name==null||name.trim().isEmpty()||name.trim().length()>100)
   throw new IllegalArgumentException("Member name is required");
  if(profiles(raw,known).containsKey(member))
   throw new IllegalArgumentException("Member profile already has a recorded origin");
  try{
   JSONObject e=new JSONObject().put("id",id).put("member",member)
    .put("name",name.trim()).put("contact","")
    .put("joined",beginning+"-01").put("notes","")
    .put("active",true).put("at",at)
    .put("statusFrom",beginning).put("memberStartMonth",beginning);
   String updated=add(raw,PROFILE_KEY,e);
   validate(updated,known);
   return updated;
  }catch(JSONException ex){throw new IllegalArgumentException("Cannot record new member billing origin",ex);}
 }
 static String setReorder(String raw,long threshold,String reason,String id,String at){
  checkId(id,at);checkReason(reason);
  if(threshold<0||threshold>1000000)throw new IllegalArgumentException("Invalid threshold");
  try{
   JSONObject e=new JSONObject().put("id",id).put("shuttles",threshold)
    .put("reason",reason.trim()).put("at",at);
   String updated=add(raw,STOCK_KEY,e);validate(updated,null);return updated;
  }catch(JSONException ex){throw new IllegalArgumentException("Cannot update reorder threshold",ex);}
 }
 static String closedMonthDrift(String raw,List<ReportMath.Result> rows){
  Map<String,ReportMath.Result> now=new HashMap<>();
  for(ReportMath.Result r:rows)now.put(r.month,r);
  StringBuilder issues=new StringBuilder();
  for(MonthlyCloseV258.MonthState state:MonthlyCloseV258.read(raw).values()){
   if(!state.closed())continue;
   ReportMath.Result r=now.get(state.month);
   if(r==null||r.opening!=state.opening||r.collections!=state.collections||
      r.memberEntries!=state.memberCash||r.expenses!=state.expenses||
      r.closing!=state.closing||r.difference!=state.difference){
    if(issues.length()>0)issues.append(", ");
    issues.append(state.month);
   }
  }
  return issues.length()==0?"CLOSED MONTH SNAPSHOTS MATCH CURRENT LEDGER":
   "WARNING: closed-month discrepancy detected for "+issues;
 }
}

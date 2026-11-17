package com.vinayak.badminton.nativepreview;
public final class V27FeeAuditSmoke {
 private static void yes(boolean pass,String context){if(!pass)throw new AssertionError(context);}
 private static void reject(Runnable r,String name){
  boolean rejected=false;
  try{r.run();}catch(IllegalArgumentException e){rejected=true;}
  yes(rejected,name);
 }
 public static void main(String[] args){
  PremiumV27FeeAudit.Result a=PremiumV27FeeAudit.review(3500,1500,1000,2000,1);
  yes(a.received==3500&&a.assigned==2000&&a.unassigned==1500,"split assigned/unassigned");
  yes(a.arrears==1000&&a.surplus==2000,"preserve arrears and surplus separately");
  yes(PremiumV27FeeAudit.headline(a).contains("₹1500"),"unassigned visible");
  yes(PremiumV27FeeAudit.caution(a).contains("no configured fee"),"unconfigured visible");
  PremiumV27FeeAudit.Result b=PremiumV27FeeAudit.review(0,0,800,0,0);
  yes(b.assigned==0&&b.arrears==800,"unpaid fee never counted as received cash");
  PremiumV27FeeAudit.Result c=PremiumV27FeeAudit.review(500,500,0,0,0);
  yes(c.assigned==0&&c.unassigned==500,"nonbillable cash remains unassigned");
  reject(()->PremiumV27FeeAudit.review(20,21,0,0,0),"cannot allocate negative cash");
  reject(()->PremiumV27FeeAudit.review(-1,0,0,0,0),"negative received");
  reject(()->PremiumV27FeeAudit.review(0,0,-1,0,0),"negative due");
  reject(()->PremiumV27FeeAudit.review(0,0,0,0,-1),"negative unconfigured");
  System.out.println("PASS v27.2 read-only fee reconciliation and negative states");
 }
}

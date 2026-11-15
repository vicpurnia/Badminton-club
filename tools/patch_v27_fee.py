#!/usr/bin/env python3
"""v27.2 additive UI overlay; no payments/Room metadata edited."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=root/"MainActivity.java"
s=activity.read_text()
old='''    String detail="Cash allocated: "+rupee(f.paid)+"  ·  Concessions: "+rupee(f.waived)+
     "  ·  Excess assigned: "+rupee(f.excess)+
     (f.unconfiguredMonths>0?"  ·  Unconfigured: "+f.unconfiguredMonths+" mo":"");'''
new='''    PremiumV27FeeAudit.Result reconciled=PremiumV27FeeAudit.review(
      f.paid,f.unallocated,f.arrears,f.excess,f.unconfiguredMonths);
    String detail="Cash assigned to assessed months: "+rupee(reconciled.assigned)+
     "  ·  Unassessed/non-billable cash: "+rupee(reconciled.unassigned)+
     "  ·  Concessions: "+rupee(f.waived)+
     "  ·  Surplus retained in original months: "+rupee(reconciled.surplus)+
     (reconciled.unconfiguredMonths>0?
      "  ·  Unconfigured: "+reconciled.unconfiguredMonths+" mo":"");'''
if s.count(old)!=1: raise AssertionError("Fee detail UI anchor changed; refuse unsafe patch")
s=s.replace(old,new,1)
old='''    view.addView(text("Unpaid fees are obligations only, not incoming cash. Excess paid against one month is not silently reassigned to another.",11,GRAY,false));gap(view,10);'''
new='''    view.addView(text("Unpaid fees are obligations, not cash received. Payments recorded for unconfigured or non-billable periods remain unassessed; surplus remains in its original fee month, never automatically reassigned.",11,GRAY,false));gap(view,10);'''
if s.count(old)!=1: raise AssertionError("Fee caution UI anchor changed; refuse unsafe patch")
s=s.replace(old,new,1)
activity.write_text(s)
(root/"PremiumV27FeeAudit.java").write_text(Path("src/premium/v27_2/PremiumV27FeeAudit.java").read_text())
print("PASS: v27.2 read-only fee reconciliation overlay; historical transactions preserved")

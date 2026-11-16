#!/usr/bin/env python3
"""Fail closed if ledger metadata changes after month-end review."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=root/"MainActivity.java"
s=activity.read_text()
patches=[
 ('''   final ReportMath.Result summary=row;
   final MonthlyCloseV258.MonthState state=''','''   final ReportMath.Result summary=row;
   final String reviewedMetaHash=PremiumV27CloseReview.fingerprint(meta.inventoryStockJson);
   final MonthlyCloseV258.MonthState state='''),
 ('''askMonthCloseReason(month,"REOPEN",summary);''','''askMonthCloseReason(month,"REOPEN",summary,reviewedMetaHash);'''),
 ('''askMonthCloseReason(month,"CLOSE",summary);''','''askMonthCloseReason(month,"CLOSE",summary,reviewedMetaHash);'''),
 ('''private void askMonthCloseReason(String month,String action,ReportMath.Result reviewed){''',
  '''private void askMonthCloseReason(String month,String action,ReportMath.Result reviewed,String reviewedMetaHash){'''),
 ('''      String id="month-close-"+UUID.randomUUID(),at=new Date().toString();''',
  '''      PremiumV27CloseReview.requireUnchanged(reviewedMetaHash,live.inventoryStockJson);
      String id="month-close-"+UUID.randomUUID(),at=new Date().toString();''')
]
for old,new in patches:
 if s.count(old)!=1:raise AssertionError("v27.6 month-end source anchor changed: "+old[:75])
 s=s.replace(old,new,1)
activity.write_text(s)
(root/"PremiumV27CloseReview.java").write_text(Path("src/premium/v27_6/PremiumV27CloseReview.java").read_text())
print("PASS v27.6 close/reopen guarded against stale ledger metadata")

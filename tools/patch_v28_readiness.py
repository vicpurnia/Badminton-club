#!/usr/bin/env python3
"""v28.0/28.1 read-only release readiness and immutable-ledger rehearsal UI."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java";s=p.read_text(encoding="utf-8")
old='''private void showReports(){title("Reports","Your club finances, beautifully presented");
    reportTile("▣","v26 EXECUTIVE DASHBOARD"'''
new='''private void showReports(){title("Reports","Your club finances, beautifully presented");
    reportTile("✓","v28 BACKUP & RELEASE READINESS",
     "Read-only full-ledger rehearsal, latest backup status and local member-photo count; no live import",
     this::showV28ReleaseReadiness);
    reportTile("▣","v26 EXECUTIVE DASHBOARD"'''
if s.count(old)!=1:raise AssertionError("Report release-readiness entry changed")
s=s.replace(old,new,1)
target=''' private void showReports(){'''
method=''' private void showV28ReleaseReadiness(){
  background(()->{
   final String result=PremiumV28ReleaseAudit.review(this,database);
   runOnUiThread(()->{
    LinearLayout body=col();pad(body,18,16);
    body.addView(text("Release readiness · read-only",21,DARK,true));gap(body,11);
    TextView resultText=text(result,12,DARK,false);
    resultText.setTextIsSelectable(true);
    body.addView(resultText);
    ScrollView scroll=new ScrollView(this);scroll.addView(body);
    new AlertDialog.Builder(this)
     .setTitle("v28 · Backup & ledger acceptance")
     .setView(scroll)
     .setPositiveButton("Close",null).show();
   });
  });
 }

'''
if s.count(target)!=1:raise AssertionError("Report UI insertion point changed")
s=s.replace(target,method+target,1)
p.write_text(s,encoding="utf-8")
(root/"PremiumV28ReleaseAudit.java").write_text(
 Path("src/premium/v28_1/PremiumV28ReleaseAudit.java").read_text(encoding="utf-8"),
 encoding="utf-8")
for required in ("PremiumV28ReleaseAudit.review(this,database)",
                 "v28 BACKUP & RELEASE READINESS","setTextIsSelectable(true)"):
 if required not in s:raise AssertionError("Missing v28 safety UI integration "+required)
print("PASS v28.1 read-only in-memory financial audit and backup/photo readiness screen")

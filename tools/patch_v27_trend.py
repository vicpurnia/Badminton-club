#!/usr/bin/env python3
"""Require chronological and unique accounting periods before exporting dashboards."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java";s=p.read_text()
for method in ("shareAnalyticsCsv","shareAnnualCsv"):
 marker="private void "+method+"(List<PremiumV26Analytics.Month> data,String month){\n  background(()->{\n"
 replacement=marker+'''   String[] checkedMonths=new String[data.size()];
   for(int i=0;i<data.size();i++)checkedMonths[i]=data.get(i).month;
   PremiumV27TrendAudit.requireOrdered(checkedMonths);
'''
 if s.count(marker)!=1:raise AssertionError("Dashboard export entry changed: "+method)
 s=s.replace(marker,replacement,1)
p.write_text(s)
(root/"PremiumV27TrendAudit.java").write_text(Path("src/premium/v27_7/PremiumV27TrendAudit.java").read_text())
print("PASS v27.7 independent CSV exports fail closed on duplicated or unsorted periods")

#!/usr/bin/env python3
"""Add descriptive month-end physical-count evidence; no stock ledger mutation."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text()
old=r'''         " individual shuttles\nStock reconciliation difference: "+difference+
         "\nReason: "+reason+'''
new=r'''         " individual shuttles\nStock reconciliation: "+PremiumV27StockVariance.describe(estimated,quantity)+
         "\nReason: "+reason+'''
if s.count(old)!=1: raise AssertionError("Physical reconciliation preview source anchor changed")
s=s.replace(old,new,1)
p.write_text(s)
(root/"PremiumV27StockVariance.java").write_text(Path("src/premium/v27_5/PremiumV27StockVariance.java").read_text())
print("PASS v27.5 positive/negative physical variance clarity without accounting changes")

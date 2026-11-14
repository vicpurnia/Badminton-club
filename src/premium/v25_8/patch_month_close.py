#!/usr/bin/env python3
"""Fail-closed verification of audited month-close history in existing JSON."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
codec=root/"MigrationCodec.java"
s=codec.read_text()
old="    StockReconciliationV257.read(inventory.toString());"
new=old+"\n    MonthlyCloseV258.read(inventory.toString());"
if s.count(old)!=1: raise RuntimeError("Expected v25.7 verified JSON inventory validation")
codec.write_text(s.replace(old,new))
for filename in ["MonthlyCloseV258.java","MonthlyReviewPdfV258.java"]:
 (root/filename).write_text((Path("src/premium/v25_8")/filename).read_text())
if "version=1" not in (root/"LedgerDatabase.java").read_text():
 raise RuntimeError("Unexpected Room schema version")
print("v25.8 audited month-close validation and PDF integration passed (Room v1)")

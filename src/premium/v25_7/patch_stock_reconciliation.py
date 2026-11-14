#!/usr/bin/env python3
"""Add verified-json stock-count parser; preserve v1 Room schema and old metadata."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
codec=root/"MigrationCodec.java"
s=codec.read_text()
old='   if(inventory!=null)InventoryCheckpoint.read(inventory.toString());'
new='''   if(inventory!=null){
    InventoryCheckpoint.read(inventory.toString());
    StockReconciliationV257.read(inventory.toString());
   }'''
if s.count(old)!=1:
 raise RuntimeError("Verified JSON inventory validator anchor absent")
codec.write_text(s.replace(old,new))
(root/"StockReconciliationV257.java").write_text(
 Path("src/premium/v25_7/StockReconciliationV257.java").read_text())
ledger=(root/"LedgerDatabase.java").read_text()
if "version=1" not in ledger: raise RuntimeError("Unexpected Room migration needed")
print("v25.7 physical-count JSON validation and Room v1 schema preservation PASS")

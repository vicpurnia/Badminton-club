#!/usr/bin/env python3
"""Precise, fail-closed integration of v25.2 checkpoint into existing Room schema."""
from pathlib import Path

root = Path("app/src/main/java/com/vinayak/badminton/nativepreview")

def inject(path, anchor, replacement, label):
    content=path.read_text()
    if content.count(anchor)!=1:
        raise RuntimeError(f"{label}: expected one known source anchor, found {content.count(anchor)}")
    path.write_text(content.replace(anchor,replacement))

dao=root/"LedgerDao.java"
old=' @Insert(onConflict=OnConflictStrategy.ABORT) void audit(AuditEvent e);'
add=" @Query(\"UPDATE ledger_meta SET inventoryStockJson=:json WHERE \x60key\x60='active'\") int updateInventoryJson(String json);"
inject(dao,old,add+"\n"+old,"ledger update accessor")
codec=root/"MigrationCodec.java"
before='   JSONArray members=ledger.getJSONArray("players"),payments=ledger.getJSONArray("payments"),expenses=ledger.getJSONArray("expenses"),usage=ledger.optJSONArray("stockUsage");'
after='   JSONObject inventory=ledger.optJSONObject("inventoryStock");\n   if(inventory!=null)InventoryCheckpoint.read(inventory.toString());\n'+before
inject(codec,before,after,"stock validation on import")
before='    state.put("club",meta.club);state.put("receiptPrefix",meta.receiptPrefix);'
after=before+'\n    // Preserve original inventory structure plus the confirmed checkpoint.\n    if(meta.inventoryStockJson!=null && !meta.inventoryStockJson.isEmpty())\n     state.put("inventoryStock",new JSONObject(meta.inventoryStockJson));'
inject(codec,before,after,"stock checkpoint in verified JSON export")
for name in ["InventoryCheckpoint.java","ShuttleInventoryMath.java"]:
    source=Path("src/premium/v25_2")/name
    (root/name).write_text(source.read_text())
print("v25.2 inventory patch: Room schema unchanged; verified JSON import/export patched")

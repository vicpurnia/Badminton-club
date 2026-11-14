#!/usr/bin/env python3
"""Fail-closed cash correction overlay integration. Must run AFTER patch_inventory.py."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
def swap(file,old,new,why):
  p=root/file
  s=p.read_text()
  count=s.count(old)
  if count!=1: raise RuntimeError(f"{file}/{why}: expected 1 anchor; found {count}")
  p.write_text(s.replace(old,new,1))
dao=root/"LedgerDao.java"
if "updateInventoryJson(String json)" not in dao.read_text():
  raise RuntimeError("Run the verified inventory patch before cash correction patch")

# ReportMath: official cash and member sum both use corrected amounts, preserving
# the pre-existing official-minus-members reconciliation discrepancy.
swap("ReportMath.java",
'  for(Contribution p:dao.contributions())if(!"packs".equals(p.kind))receipts.merge(p.feeMonth,p.amount,Long::sum);',
'''  List<Contribution> payments=dao.contributions();
  for(Contribution p:payments)if(!"packs".equals(p.kind))receipts.merge(p.feeMonth,p.amount,Long::sum);
  Map<String,Long> corrections=CashCorrectionsV256.monthDeltas(meta.inventoryStockJson,payments);
  for(Map.Entry<String,Long> adjustment:corrections.entrySet())
    receipts.merge(adjustment.getKey(),adjustment.getValue(),Math::addExact);''',
"effective receipt sums")

# Migration inspector: verify all original cash entries and correction-chain IDs
# before applying effective cash amounts to reconciliation.
swap("MigrationCodec.java",
'   JSONObject controls=ledger.optJSONObject("monthControlTotals");if(controls==null)controls=new JSONObject();',
'''   Map<String,Long> cashCorrectionDeltas=CashCorrectionsV256.monthDeltas(ledger);
   for(Map.Entry<String,Long> correction:cashCorrectionDeltas.entrySet())
     col.merge(correction.getKey(),correction.getValue(),Math::addExact);
   JSONObject controls=ledger.optJSONObject("monthControlTotals");if(controls==null)controls=new JSONObject();''',
"verify correction chain in import inspector")
# Snapshot imported official control contains the net adjustment already; remove
# it from the pristine control baseline so that export->import->export is idempotent.
swap("MigrationCodec.java",
'    for(String m:keys(controls)){MonthlyControl x=new MonthlyControl();x.month=m;x.cash=controls.getLong(m);x.memberCashAtImport=cashAtImport.getOrDefault(m,0L);cs.add(x);}',
'''    Map<String,Long> importedCorrectionDeltas=CashCorrectionsV256.monthDeltas(d);
    for(String m:keys(controls)){
     MonthlyControl x=new MonthlyControl();x.month=m;
     x.cash=Math.subtractExact(controls.getLong(m),importedCorrectionDeltas.getOrDefault(m,0L));
     x.memberCashAtImport=cashAtImport.getOrDefault(m,0L);cs.add(x);
    }''',
"import official baseline without double counting")
swap("MigrationCodec.java",
'    JSONObject controls=new JSONObject();for(MonthlyControl c:dao.controls()){',
'''    Map<String,Long> localCorrections=CashCorrectionsV256.monthDeltas(meta.inventoryStockJson,dao.contributions());
    for(Map.Entry<String,Long> correction:localCorrections.entrySet())
      memberCash.merge(correction.getKey(),correction.getValue(),Math::addExact);
    JSONObject controls=new JSONObject();for(MonthlyControl c:dao.controls()){''',
"export effective official totals")

# Existing A4 one-page layout is unchanged: only cash cell values reflect net
# recognized collections; noncash donated packs retain their original counts.
swap("ClassicLedgerV231.java",
'  for(Contribution entry:contributions){',
'''  Map<String,CashCorrectionsV256.State> correctionStates=
   CashCorrectionsV256.states(meta.inventoryStockJson,contributions);
  for(Contribution entry:contributions){''',
"classic ledger adjusted cash data")
swap("ClassicLedgerV231.java",
'   pair["packs".equals(entry.kind)?1:0]+=entry.amount;',
'''   long effective="packs".equals(entry.kind)?entry.amount:
     correctionStates.containsKey(entry.id)?correctionStates.get(entry.id).effective:entry.amount;
   pair["packs".equals(entry.kind)?1:0]=Math.addExact(pair["packs".equals(entry.kind)?1:0],effective);''',
"classic ledger cash cell")

# Monthly pdf keeps cancelled entries identifiable without pretending they are valid receipts.
swap("PdfReports.java",
'  List<Contribution> received=new ArrayList<>();for(Contribution p:dao.contributions())if(p.feeMonth.equals(month))received.add(p);',
'''  List<Contribution> allPayments=dao.contributions();
  Map<String,CashCorrectionsV256.State> cashState=CashCorrectionsV256.states(meta.inventoryStockJson,allPayments);
  List<Contribution> received=new ArrayList<>();
  for(Contribution p:allPayments)if(p.feeMonth.equals(month))received.add(p);''',
"monthly PDF corrections state")
swap("PdfReports.java",
'    txt(c,"packs".equals(p.kind)?p.amount+" donated pack(s)":"₹"+number(p.amount),285,y,10.5f,DARK,true);',
'''    long reported="packs".equals(p.kind)?p.amount:
     cashState.containsKey(p.id)?cashState.get(p.id).effective:p.amount;
    txt(c,"packs".equals(p.kind)?p.amount+" donated pack(s)":
     reported==0?"VOID":("₹"+number(reported)),285,y,10.5f,reported==0?RED:DARK,true);''',
"monthly PDF effective amount")
(root/"CashCorrectionsV256.java").write_text(Path("src/premium/v25_6/CashCorrectionsV256.java").read_text())
print("v25.6 cash corrections patch passed: existing schema v1 and A4 layout unchanged")

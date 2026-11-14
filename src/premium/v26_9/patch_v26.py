#!/usr/bin/env python3
"""v26 integration: maintain Room schema v1 and fail-closed verified migration metadata."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
codec=root/"MigrationCodec.java"
s=codec.read_text()
old="    MonthlyCloseV258.read(inventory.toString());"
new=old+"\n    PremiumV26State.validate(inventory.toString(),null);"
if s.count(old)!=1:
 raise RuntimeError(f"v26 verified JSON validator anchor count {s.count(old)} != 1")
codec.write_text(s.replace(old,new,1))
for filename in ["PremiumV26State.java","PremiumV26Analytics.java","PremiumBackupCrypto.java"]:
 p=Path("src/premium/v26_9")/filename
 if not p.is_file(): raise RuntimeError(f"Missing v26 module {p}")
 (root/filename).write_text(p.read_text())
ledger=(root/"LedgerDatabase.java").read_text()
assert "version=1" in ledger, "Room schema changed; destructive migration forbidden"
main=(root/"MainActivity.java").read_text()
for required in ["v26.9 DEVELOPMENT CANDIDATE","showFeeCentre()","startEncryptedExport()",
                 "showExecutiveDashboard()","exportMemberReceiptsZip(", "requireOpenMonth("]:
 assert required in main, "Missing feature: "+required
classic=(root/"ClassicLedgerV231.java").read_text()
assert "new PdfDocument.PageInfo.Builder(842,595,1)" in classic
print("v26 additive overlay integrated; verified JSON validation extended; Room v1 and A4 Classic guarded")

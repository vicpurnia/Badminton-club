#!/usr/bin/env python3
"""v26 integration: maintain Room schema v1 and fail-closed verified migration metadata."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
codec=root/"MigrationCodec.java"
s=codec.read_text()
old="    MonthlyCloseV258.read(inventory.toString());"
new=old+"""
    // Validate every fee and profile reference against the immutable imported roster.
    // A structurally valid metadata array referring to a nonexistent member is not safe.
    org.json.JSONArray v26Players=ledger.getJSONArray("players");
    java.util.Set<String> v26KnownMembers=new java.util.HashSet<>();
    for(int v26i=0;v26i<v26Players.length();v26i++){
     String v26id=v26Players.getJSONObject(v26i).getString("id");
     if(v26id.trim().isEmpty()||!v26KnownMembers.add(v26id))
      throw new IllegalArgumentException("Invalid or duplicate member in verified ledger");
    }
    PremiumV26State.validate(inventory.toString(),v26KnownMembers);"""
if s.count(old)!=1:
 raise RuntimeError(f"v26 verified JSON validator anchor count {s.count(old)} != 1")
s=s.replace(old,new,1)
# Source-backed historical native audit events already live in sourceLedgerJson;
# do not drop them on each export. Import itself is a device-local operation and
# must not invent an extra ledger history item in a read-only recovery rehearsal.
audit_old='JSONArray audit=new JSONArray();for(AuditEvent e:dao.auditEvents()){'
audit_new="""JSONArray audit=state.optJSONArray("nativeAuditHistory");
     boolean sourceAuditPresent=state.has("nativeAuditHistory");
     if(sourceAuditPresent&&audit==null)
      throw new IllegalArgumentException("Source audit history is not an array");
     if(audit==null)audit=new JSONArray();
     for(AuditEvent e:dao.auditEvents()){"""
if s.count(audit_old)!=1:
 raise RuntimeError(f"v26 audit export source anchor count {s.count(audit_old)} != 1")
s=s.replace(audit_old,audit_new,1)
audit_filter='if(e.id.startsWith("original-history-")||e.id.startsWith("original-migrationHistory-")||e.id.startsWith("original-nativeAuditHistory-"))continue;'
audit_replacement=audit_filter+'\n     if(e.id.startsWith("import-"))continue; // import provenance is local, not a new club financial event'
if s.count(audit_filter)!=1:
 raise RuntimeError(f"v26 historical audit dedup anchor count {s.count(audit_filter)} != 1")
s=s.replace(audit_filter,audit_replacement,1)
audit_put='state.put("nativeAuditHistory",audit);'
audit_put_new='if(sourceAuditPresent||audit.length()>0)state.put("nativeAuditHistory",audit);'
if s.count(audit_put)!=1:
 raise RuntimeError(f"v26 historical audit write anchor count {s.count(audit_put)} != 1")
s=s.replace(audit_put,audit_put_new,1)
codec.write_text(s)
for filename in ["PremiumV26State.java","PremiumV26Analytics.java","PremiumBackupCrypto.java","CashCorrectionsV256.java","BackupRecoveryV259.java","PremiumV26StockGuard.java","PremiumCashDateAudit.java","PremiumV26ClosingGuard.java","PremiumV26PaymentOnce.java","PremiumV26BackupCompare.java"]:
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

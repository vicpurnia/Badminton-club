#!/usr/bin/env python3
"""v27.9.2: exact-reviewed backup confirmation, no stale/overlapping import.

Fail-closed build-only overlay: never changes Room schema or imported ledger.
The source is generated from the original v26.9 app after v27.9 modules.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text(encoding="utf-8")
def once(old,new):
 global s
 if s.count(old)!=1:
  raise AssertionError(f"Import gate source anchor changed ({s.count(old)}): {old[:100]}")
 s=s.replace(old,new,1)

once(" private MigrationCodec.Preview pendingImport;",
     " private final PremiumV2792ImportGate<MigrationCodec.Preview> importGate=new PremiumV2792ImportGate<>();")

once('''if(database.dao().metaCount()>0)throw new IllegalStateException("The native ledger already contains data. Replacement is blocked for your safety.");pendingImport=result;''',
     '''if(database.dao().metaCount()>0)throw new IllegalStateException("The native ledger already contains data. Replacement is blocked for your safety.");
   final long importReview=importGate.stage(result);''')
once('''.setNegativeButton("Cancel",(d,w)->pendingImport=null)
    .setPositiveButton("Import securely",(d,w)->importPending()).show());''',
     '''.setNegativeButton("Cancel",(d,w)->importGate.cancel(importReview))
    .setPositiveButton("Import securely",(d,w)->importPending(result,importReview)).show());''')

once('''      if(allowImport&&database.dao().metaCount()==0){
       pendingImport=preview;
       runOnUiThread''',
     '''      if(allowImport&&database.dao().metaCount()==0){
       final long importReview=importGate.stage(preview);
       runOnUiThread''')
once('''.setNegativeButton("Keep empty",(dd,ww)->pendingImport=null)
        .setPositiveButton("Import verified history",(dd,ww)->importPending()).show());''',
     '''.setNegativeButton("Keep empty",(dd,ww)->importGate.cancel(importReview))
        .setPositiveButton("Import verified history",(dd,ww)->importPending(preview,importReview)).show());''')

once(''' private void importPending(){MigrationCodec.Preview preview=pendingImport;pendingImport=null;if(preview==null)return;
  background(()->{MigrationCodec.importChecked(database,preview);BackupGuardian.save(this,database,"import",true);warnOnBackupFailure();runOnUiThread(()->{Toast.makeText(this,"Ledger imported. Review backups in Reports.",Toast.LENGTH_LONG).show();selectTab("Home");});});}''',
     ''' private void importPending(MigrationCodec.Preview preview,long reviewToken){
  if(!importGate.consume(preview,reviewToken)){
   error(new IllegalStateException(
    "This backup confirmation is stale or was already used. Review the selected backup again. No records changed."));
   return;
  }
  background(()->{
   LedgerDao current=database.dao();
   // The preview was created earlier; verify emptiness *again* at commit time.
   // Single-thread database executor prevents another local worker insertion
   // between this check and importChecked's own transactional validation.
   if(current.metaCount()!=0||!current.members().isEmpty()||
      !current.contributions().isEmpty()||!current.expenses().isEmpty()||
      !current.stockUsages().isEmpty()||!current.controls().isEmpty()||
      !current.legacy().isEmpty()||!current.auditEvents().isEmpty())
    throw new IllegalStateException(
     "Import blocked: the installed app is no longer completely empty. No existing record may be replaced.");
   MigrationCodec.importChecked(database,preview);
   BackupGuardian.save(this,database,"import",true);
   warnOnBackupFailure();
   runOnUiThread(()->{
    Toast.makeText(this,"Ledger imported. Review backups in Reports.",Toast.LENGTH_LONG).show();
    selectTab("Home");
   });
  });
 }''')
if "pendingImport" in s:
 raise AssertionError("Old shared pending-import field still referenced")
for needle in ("importGate.stage(result)","importGate.stage(preview)",
               "importPending(result,importReview)","importPending(preview,importReview)",
               "importGate.consume(preview,reviewToken)",
               "current.auditEvents().isEmpty()"):
 if needle not in s:raise AssertionError("Import protection not integrated: "+needle)
p.write_text(s,encoding="utf-8")
(root/"PremiumV2792ImportGate.java").write_text(
 Path("src/premium/v27_9_2/PremiumV2792ImportGate.java").read_text(encoding="utf-8"),encoding="utf-8")
print("PASS v27.9.2: exact-review, one-shot import confirmation; commit-time empty DB")

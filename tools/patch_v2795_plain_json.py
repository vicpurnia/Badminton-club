#!/usr/bin/env python3
"""v27.9.5 backup JSON Documents export: pin exact source and guard picker.

Integrates into the generated Android app after previous v27 overlays;
does not modify original Room schema, verified JSON codec or any payment.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text(encoding="utf-8")
def once(old,new):
 global s
 n=s.count(old)
 if n!=1:raise AssertionError(f"Plain JSON export anchor changed ({n}): {old[:100]}")
 s=s.replace(old,new,1)

once(" private File pendingExport,pendingReportPdf;",
     " private File pendingExport,pendingReportPdf;\n private final PremiumV2795JsonExportGuard jsonExportGate=new PremiumV2795JsonExportGuard();")

once(''' private void startVerifiedExport(){
  background(()->{
   String json=MigrationCodec.exportVerified(database);
   MigrationCodec.inspect(json); // Validate the verified export before offering it to Android Documents.
   byte[] payload=json.getBytes(StandardCharsets.UTF_8);
   if(payload.length==0||payload.length>BackupGuardian.MAX_BACKUP_BYTES)throw new IOException("Verified export exceeds 12 MB; avoid an incomplete backup.");
   File temp=new File(getCacheDir(),"Badminton_verified_export_pending.json");
   try(OutputStream os=new FileOutputStream(temp)){os.write(payload);}
   pendingExport=temp;
   runOnUiThread(()->{
    Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/json");
    String stamp=new java.text.SimpleDateFormat("yyyyMMdd_HHmm",Locale.ROOT).format(new Date());
    save.putExtra(Intent.EXTRA_TITLE,"Badminton_Verified_"+stamp+".json");
    startActivityForResult(save,SAVE_VERIFIED);
   });
  });
 }''',
     ''' private void startVerifiedExport(){
  if(!jsonExportGate.begin()){
   error(new IllegalStateException(
    "A verified JSON backup is already being prepared or saved. Finish or cancel its Documents picker first."));
   return;
  }
  background(()->{
   File temp=null;
   try{
    String json=MigrationCodec.exportVerified(database);
    MigrationCodec.inspect(json); // Keep full verified ledger validation.
    byte[] payload=json.getBytes(StandardCharsets.UTF_8);
    if(payload.length==0||payload.length>BackupGuardian.MAX_BACKUP_BYTES)
     throw new IOException("Verified export exceeds 12 MB; avoid an incomplete backup.");
    temp=new File(getCacheDir(),
     "Badminton_verified_export_"+UUID.randomUUID()+".json");
    try(OutputStream output=new FileOutputStream(temp)){
     output.write(payload);output.flush();
    }finally{Arrays.fill(payload,(byte)0);}
    jsonExportGate.stage(temp);
    pendingExport=temp;
    final File staged=temp;
    runOnUiThread(()->{
     Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
     save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/json");
     String stamp=new java.text.SimpleDateFormat("yyyyMMdd_HHmm",Locale.ROOT).format(new Date());
     save.putExtra(Intent.EXTRA_TITLE,"Badminton_Verified_"+stamp+".json");
     try{startActivityForResult(save,SAVE_VERIFIED);}
     catch(Exception pickerError){
      pendingExport=null;
      staged.delete();
      jsonExportGate.finish();
      error(new IOException("Android Documents picker unavailable for verified JSON export."));
     }
    });
   }catch(Exception failure){
    if(temp!=null)temp.delete();
    jsonExportGate.finish();
    throw failure;
   }
  });
 }''')

once('''  if(req==SAVE_VERIFIED){
   File file=pendingExport;pendingExport=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){if(file!=null)file.delete();return;}
   if(file==null){error(new IOException("Export session expired. Please choose Export again."));return;}
   Uri destination=data.getData();
   background(()->{
    try{
     try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(destination,"w")){
      if(out==null)throw new IOException("Unable to open the selected save location");
      byte[] b=new byte[16384];int n;
      while((n=in.read(b))!=-1)if(n>0)out.write(b,0,n);
      out.flush();
     }
     // Some document providers accept a write but fail to persist identical bytes.
     // Do not claim a usable backup until it has been read back and validated.
     BackupGuardian.verifySavedCopy(this,destination,file);
     runOnUiThread(()->new AlertDialog.Builder(this)
      .setTitle("Verified JSON saved and checked")
      .setMessage("Your exported account file was reopened, matched by SHA-256, and validated as a readable ledger. Keep a copy outside this app before uninstalling or changing phones.")
      .setPositiveButton("OK",null).show());
    }catch(Exception ex){
     throw new IOException("Exported JSON was not verified. Do not rely on this file as your only backup. "+ex.getMessage(),ex);
    }finally{file.delete();}
   });
   return;
  }''',
     '''  if(req==SAVE_VERIFIED){
   File file=pendingExport;pendingExport=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){
    if(file!=null)file.delete();
    jsonExportGate.finish();return;
   }
   if(file==null||!file.isFile()){
    jsonExportGate.finish();
    error(new IOException("Export session expired. Generate and verify your JSON backup again."));
    return;
   }
   Uri destination=data.getData();
   background(()->{
    try{
     // The file-picker may have been open while other app activity occurred.
     // The selected backup must still be exactly the staged verified bytes.
     jsonExportGate.requireUnchanged(file);
     try(InputStream in=new FileInputStream(file);
         OutputStream out=getContentResolver().openOutputStream(destination,"w")){
      if(out==null)throw new IOException("Unable to open the selected save location");
      byte[] buffer=new byte[16384];int n;
      while((n=in.read(buffer))!=-1)if(n>0)out.write(buffer,0,n);
      out.flush();
     }
     jsonExportGate.requireUnchanged(file);
     // A file picker success does not guarantee durable bytes. Always reopen
     // Android Documents, compare SHA-256 and validate complete ledger content.
     BackupGuardian.verifySavedCopy(this,destination,file);
     runOnUiThread(()->new AlertDialog.Builder(this)
      .setTitle("Verified JSON saved and checked")
      .setMessage("The selected backup was reopened, matched by SHA-256 and verified as a complete readable ledger. Store another copy outside this phone.")
      .setPositiveButton("OK",null).show());
    }catch(Exception ex){
     throw new IOException("Exported JSON was NOT verified. Do not rely on this file as your only backup. "+ex.getMessage(),ex);
    }finally{
     file.delete();
     jsonExportGate.finish();
    }
   });
   return;
  }''')

for check in ("jsonExportGate.begin()","jsonExportGate.stage(temp)",
              "jsonExportGate.requireUnchanged(file)","UUID.randomUUID()+\".json\"",
              "BackupGuardian.verifySavedCopy(this,destination,file)",
              "jsonExportGate.finish()"):
 if check not in s:raise AssertionError("JSON safety hook missing "+check)
p.write_text(s,encoding="utf-8")
(root/"PremiumV2795JsonExportGuard.java").write_text(
 Path("src/premium/v27_9_5/PremiumV2795JsonExportGuard.java").read_text(encoding="utf-8"),encoding="utf-8")
print("PASS v27.9.5 JSON export one-at-a-time, unique staged file and verified provider readback")

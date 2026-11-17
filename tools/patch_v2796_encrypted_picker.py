#!/usr/bin/env python3
"""v27.9.6: bind encrypted restore intention to exact Android picker session.

Run after v27.9.2 importGate patch and before any signed APK compilation.
No schema change or automatic import; preserves full in-memory dry-run verification.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text(encoding="utf-8")
def once(old,new):
 global s
 if s.count(old)!=1:
  raise AssertionError(f"Encrypted picker integration source changed ({s.count(old)}): {old[:95]}")
 s=s.replace(old,new,1)

once(''' private boolean encryptedImportAfterCheck=false;''',
     ''' private final PremiumV2796EncryptedPickerGate encryptedPickerGate=
  new PremiumV2796EncryptedPickerGate();''')

once('''  if(req==READ_ENCRYPTED_V26){
   if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)
    readEncryptedBackup(data.getData(),encryptedImportAfterCheck);
   return;
  }''',
     '''  if(req==READ_ENCRYPTED_V26){
   // Only the picker owning this request may determine TEST-ONLY vs IMPORT.
   PremiumV2796EncryptedPickerGate.Review request=encryptedPickerGate.pickerReturned();
   if(request==null){
    error(new IllegalStateException("Encrypted recovery picker result is stale. No backup imported."));
    return;
   }
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){
    encryptedPickerGate.finish(request.id);
    return;
   }
   try{readEncryptedBackup(data.getData(),request.allowImport,request.id);}
   catch(Exception ex){
    encryptedPickerGate.finish(request.id);
    error(new IOException("Cannot open the encrypted recovery review",ex));
   }
   return;
  }''')

once(''' private void selectEncryptedDocument(boolean wantsImport){
  encryptedImportAfterCheck=wantsImport;
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,READ_ENCRYPTED_V26);}
  catch(Exception ex){error(new IOException("No encrypted file picker available"));}
 }''',
     ''' private void selectEncryptedDocument(boolean wantsImport){
  if(!encryptedPickerGate.begin(wantsImport)){
   error(new IllegalStateException(
    "An encrypted backup picker or verification is already active. Complete or cancel it first."));
   return;
  }
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,READ_ENCRYPTED_V26);}
  catch(Exception ex){
   PremiumV2796EncryptedPickerGate.Review request=encryptedPickerGate.pickerReturned();
   if(request!=null)encryptedPickerGate.finish(request.id);
   error(new IOException("No encrypted file picker available",ex));
  }
 }''')

once(''' private void readEncryptedBackup(Uri uri,boolean allowImport){
  EditText pass=new EditText(this);pass.setInputType(129);''',
     ''' private void readEncryptedBackup(Uri uri,boolean allowImport,long reviewToken){
  EditText pass=new EditText(this);pass.setInputType(129);''')

once('''  new AlertDialog.Builder(this).setTitle("Unlock encrypted backup")
   .setView(pass).setNegativeButton("Cancel",null).setPositiveButton("Verify",(d,w)->{
    char[] password=pass.getText().toString().toCharArray();pass.setText("");
    background(()->{''',
     '''  new AlertDialog.Builder(this).setTitle("Unlock encrypted backup")
   .setView(pass)
   .setNegativeButton("Cancel",(d,w)->encryptedPickerGate.finish(reviewToken))
   .setOnCancelListener(d->encryptedPickerGate.finish(reviewToken))
   .setPositiveButton("Verify",(d,w)->{
    if(!encryptedPickerGate.beginVerification(reviewToken)){
     pass.setText("");
     error(new IllegalStateException("Encrypted recovery confirmation expired. No backup imported."));
     return;
    }
    char[] password=pass.getText().toString().toCharArray();pass.setText("");
    background(()->{''')

once(r'''     }finally{Arrays.fill(password,'\u0000');}
    });
   }).show();
 }

 private void showCashDateAudit(){''',
     r'''     }finally{
      Arrays.fill(password,'\u0000');
      encryptedPickerGate.finish(reviewToken);
     }
    });
   }).show();
 }

 private void showCashDateAudit(){''')

if "encryptedImportAfterCheck" in s:
 raise AssertionError("Shared restore-mode flag remains in generated Activity")
for check in ("encryptedPickerGate.begin(wantsImport)",
              "encryptedPickerGate.pickerReturned()",
              "readEncryptedBackup(data.getData(),request.allowImport,request.id)",
              "encryptedPickerGate.beginVerification(reviewToken)",
              "encryptedPickerGate.finish(reviewToken)",
              "importGate.stage(preview)"):
 if check not in s:raise AssertionError("Required verification hook missing "+check)
p.write_text(s,encoding="utf-8")
(root/"PremiumV2796EncryptedPickerGate.java").write_text(
 Path("src/premium/v27_9_6/PremiumV2796EncryptedPickerGate.java").read_text(encoding="utf-8"),encoding="utf-8")
print("PASS v27.9.6 encrypted restore mode tied to single picker and owned verification")

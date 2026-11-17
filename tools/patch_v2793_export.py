#!/usr/bin/env python3
"""v27.9.3: one encrypted backup export per Android Documents picker.

Protect the user-selected backup file from overlapping export sessions, and
create uniquely named private staging data instead of a shared cache filename.
No Room writes and no signing identity changes.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text(encoding="utf-8")
def once(old,new):
 global s
 if s.count(old)!=1:
  raise AssertionError(f"Encrypted export source anchor changed ({s.count(old)}): {old[:85]}")
 s=s.replace(old,new,1)

once(" private File pendingEncryptedExport;",
     " private File pendingEncryptedExport;\n private final PremiumV2793ExportGate encryptedExportGate=new PremiumV2793ExportGate();")

once('''  if(req==SAVE_ENCRYPTED_V26){
   File file=pendingEncryptedExport;pendingEncryptedExport=null;''',
     '''  if(req==SAVE_ENCRYPTED_V26){
   File file=pendingEncryptedExport;pendingEncryptedExport=null;
   encryptedExportGate.finish();''')

once(''' private void startEncryptedExport(){
  LinearLayout form=col();''',
     ''' private void startEncryptedExport(){
  if(!encryptedExportGate.begin()){
   error(new IllegalStateException(
    "Another encrypted backup is being prepared or saved. Finish or cancel that file picker first."));
   return;
  }
  LinearLayout form=col();''')

once('''.setView(form).setNegativeButton("Cancel",null).setPositiveButton("Encrypt & save",(d,w)->{''',
     '''.setView(form)
   .setNegativeButton("Cancel",(d,w)->encryptedExportGate.finish())
   .setOnCancelListener(d->encryptedExportGate.finish())
   .setPositiveButton("Encrypt & save",(d,w)->{''')

once(r'''     Arrays.fill(first,'\u0000');Arrays.fill(confirm,'\u0000');
     error(new IllegalArgumentException("Passwords must match and contain at least 12 characters."));return;''',
     r'''     Arrays.fill(first,'\u0000');Arrays.fill(confirm,'\u0000');
     encryptedExportGate.finish();
     error(new IllegalArgumentException("Passwords must match and contain at least 12 characters."));return;''')

once('''      File file=new File(getCacheDir(),"Badminton_Premium_Encrypted_Migration.bpe");''',
     '''      File file=new File(getCacheDir(),
       "Badminton_Premium_Encrypted_Migration_"+UUID.randomUUID()+".bpe");''')

once('''       catch(Exception ex){pendingEncryptedExport=null;error(new IOException("Secure file picker unavailable."));}''',
     '''       catch(Exception ex){
        pendingEncryptedExport=null;
        file.delete();
        encryptedExportGate.finish();
        error(new IOException("Secure file picker unavailable."));
       }''')

once(r'''     }finally{Arrays.fill(first,'\u0000');}
    });
   }).show();''',
     r'''     }catch(Exception ex){
      encryptedExportGate.finish();
      throw ex;
     }finally{Arrays.fill(first,'\u0000');}
    });
   }).show();''')

if s.count('pendingEncryptedExport=file;')!=1:
 raise AssertionError("Encrypted export should stage exactly one file")
for term in ("encryptedExportGate.begin()","encryptedExportGate.finish()",
             '"Badminton_Premium_Encrypted_Migration_"+UUID.randomUUID()+".bpe"',
             "setOnCancelListener(d->encryptedExportGate.finish())"):
 if term not in s:raise AssertionError("Missing encrypted export guard: "+term)
p.write_text(s,encoding="utf-8")
(root/"PremiumV2793ExportGate.java").write_text(
 Path("src/premium/v27_9_3/PremiumV2793ExportGate.java").read_text(encoding="utf-8"),encoding="utf-8")
print("PASS v27.9.3 encrypted export single-flight, unique encrypted stage, cancellation cleanup")

#!/usr/bin/env python3
"""Verify encrypted migration export in the cache before Android document sharing."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java";s=p.read_text()
old='''      String encrypted=PremiumBackupCrypto.encrypt(verified,first);
      File file=new File(getCacheDir(),"Badminton_Premium_Encrypted_Migration.bpe");
      try(OutputStream output=new FileOutputStream(file)){
       output.write(encrypted.getBytes(StandardCharsets.UTF_8));
      }
      pendingEncryptedExport=file;'''
new='''      String encrypted=PremiumBackupCrypto.encrypt(verified,first);
      byte[] ciphertext=encrypted.getBytes(StandardCharsets.UTF_8);
      File file=new File(getCacheDir(),"Badminton_Premium_Encrypted_Migration.bpe");
      try{
       try(OutputStream output=new FileOutputStream(file)){
        output.write(ciphertext);
       }
       PremiumV27BackupStaging.verify(file,ciphertext);
      }catch(Exception stagingError){
       file.delete();
       throw stagingError;
      }finally{Arrays.fill(ciphertext,(byte)0);}
      pendingEncryptedExport=file;'''
if s.count(old)!=1:raise AssertionError("Encrypted export source anchor changed")
s=s.replace(old,new,1)
p.write_text(s)
(root/"PremiumV27BackupStaging.java").write_text(Path("src/premium/v27_8/PremiumV27BackupStaging.java").read_text())
print("PASS v27.8 encrypted ciphertext byte-level staged-write verification")

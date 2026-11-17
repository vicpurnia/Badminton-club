#!/usr/bin/env python3
"""v27.9.8: reject non-PDF/truncated receipt ZIP payloads before Android share.

Runs after the original v27.3 ZIP integration and v27.9.1 index/CRC hardening.
No changes to receipt renderer, historical payment rows or Room schema.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
archive=root/"PremiumV27ReceiptArchive.java"
existing=archive.read_text(encoding="utf-8")
source=Path("src/premium/v27_9_8/PremiumV27ReceiptArchive.java").read_text(encoding="utf-8")
for expected in ("ZipFile index", "indexedCrc", "indexedSizes",
                 "Receipt ZIP central directory CRC or entry size"):
 if expected not in existing or expected not in source:
  raise AssertionError("Required hardened v27.9.1 ZIP validator is missing: "+expected)
for expected in ("pdfStart", "pdfEofPresent", "non-PDF or truncated PDF"):
 if expected not in source:
  raise AssertionError("Missing v27.9.8 PDF sanity guard: "+expected)
archive.write_text(source,encoding="utf-8")

activity=root/"MainActivity.java"
s=activity.read_text(encoding="utf-8")
old='''   PremiumV27ReceiptArchive.promote(staging,zip,matches.size());
   runOnUiThread(()->{
    try{
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",zip);'''
new='''   PremiumV27ReceiptArchive.promote(staging,zip,matches.size());
   // After promotion, ensure the final ZIP still contains actual PDF-like
   // payloads; run decompression off UI before Android share-sheet launch.
   PremiumV27ReceiptArchive.verify(zip,matches.size());
   final long checkedArchiveBytes=zip.length();
   runOnUiThread(()->{
    try{
     if(!zip.isFile()||zip.length()!=checkedArchiveBytes)
      throw new IOException("Verified receipt ZIP changed before sharing");
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",zip);'''
if s.count(old)!=1:
 raise AssertionError(f"Receipt bundle share hook changed; found {s.count(old)}")
s=s.replace(old,new,1)
old_error='''    }catch(Exception ex){error(new IOException("Could not share receipt ZIP: "+ex.getMessage()));}
   });
  });
 }'''
new_error='''    }catch(Exception ex){
     zip.delete();
     error(new IOException("Could not share verified receipt ZIP: "+ex.getMessage(),ex));
    }
   });
  });
 }'''
if s.count(old_error)!=1:
 raise AssertionError("Receipt bundle share error path changed")
s=s.replace(old_error,new_error,1)
activity.write_text(s,encoding="utf-8")
print("PASS v27.9.8 receipt ZIP checks PDF header/EOF, index, CRC and final share safety")

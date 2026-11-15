#!/usr/bin/env python3
"""v27.3 staged-and-verified member receipt ZIP; never rewrites cash rows."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=root/"MainActivity.java"
s=activity.read_text()
old='''   File zip=new File(shareCacheDir(),"Member_Receipts_"+member.id.replaceAll("[^A-Za-z0-9_-]","_")+".zip");
   long uncompressedBytes=0L;
   try(java.util.zip.ZipOutputStream output=new java.util.zip.ZipOutputStream(new FileOutputStream(zip))){'''
new='''   File zip=new File(shareCacheDir(),"Member_Receipts_"+
      member.id.replaceAll("[^A-Za-z0-9_-]","_")+"_"+
      System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,8)+".zip");
   File staging=new File(zip.getParentFile(),zip.getName()+".partial");
   long uncompressedBytes=0L;
   try(java.util.zip.ZipOutputStream output=new java.util.zip.ZipOutputStream(new FileOutputStream(staging))){'''
if s.count(old)!=1:raise AssertionError("Receipt ZIP creation anchor changed")
s=s.replace(old,new,1)
old='''   }catch(Exception exception){
    zip.delete(); // Never leave a broken ZIP masquerading as a successful export.
    throw exception;
   }
   if(!zip.isFile()||zip.length()<100||zip.length()>35_000_000L){
    zip.delete();
    throw new IOException("Member receipt ZIP could not be safely verified");
   }'''
new='''   }catch(Exception exception){
    staging.delete(); // Never leave an incomplete archive in the share folder.
    throw exception;
   }
   PremiumV27ReceiptArchive.promote(staging,zip,matches.size());'''
if s.count(old)!=1:raise AssertionError("Receipt ZIP finalization anchor changed")
s=s.replace(old,new,1)
activity.write_text(s)
(root/"PremiumV27ReceiptArchive.java").write_text(
 Path("src/premium/v27_3/PremiumV27ReceiptArchive.java").read_text())
print("PASS v27.3 staged verified receipt ZIP overlay")

#!/usr/bin/env python3
"""v27.9.7: monthly/annual CSV exports use independent verified attachments.

A report for the same month may be shared twice; never replace the earlier
FileProvider-backed CSV while an Android chooser or recipient is reading it.
This changes only read-only exports, never original cash/Room/fees.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text(encoding="utf-8")
old=''' private void shareAnalyticsCsv(List<PremiumV26Analytics.Month> data,String month){
  background(()->{
   File file=new File(shareCacheDir(),"Premium_Dashboard_"+month+".csv");
   try(OutputStream out=new FileOutputStream(file)){
    out.write(PremiumV26Analytics.csv(data).getBytes(StandardCharsets.UTF_8));
   }
   runOnUiThread(()->{
    try{
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
     Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/csv");
     share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(share,"Save or share verified dashboard CSV"));
    }catch(Exception ex){error(new IOException("Could not share monthly CSV: "+ex.getMessage()));}
   });
  });
 }
 private void shareAnnualCsv(List<PremiumV26Analytics.Month> data,String month){
  background(()->{
   File file=new File(shareCacheDir(),"Premium_Yearly_"+month+".csv");
   try(OutputStream out=new FileOutputStream(file)){
    out.write(PremiumV26Analytics.annualCsv(data).getBytes(StandardCharsets.UTF_8));
   }
   runOnUiThread(()->{
    try{
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
     Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/csv");
     send.putExtra(Intent.EXTRA_STREAM,uri);
     send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(send,"Save or share verified yearly CSV"));
    }catch(Exception ex){error(new IOException("Could not share yearly CSV."));}
   });
  });
 }
'''
new=''' private void shareAnalyticsCsv(List<PremiumV26Analytics.Month> data,String month){
  background(()->{
   final PremiumV2797CsvShare.Share attachment=PremiumV2797CsvShare.create(
    shareCacheDir(),"Premium_Dashboard_"+MigrationCodec.ym(month),
    PremiumV26Analytics.csv(data));
   runOnUiThread(()->{
    try{
     PremiumV2797CsvShare.verify(attachment);
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",attachment.file);
     Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/csv");
     share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(share,"Save or share verified dashboard CSV"));
    }catch(Exception ex){
     attachment.file.delete();
     error(new IOException("Could not share verified monthly CSV: "+ex.getMessage(),ex));
    }
   });
  });
 }
 private void shareAnnualCsv(List<PremiumV26Analytics.Month> data,String month){
  background(()->{
   final PremiumV2797CsvShare.Share attachment=PremiumV2797CsvShare.create(
    shareCacheDir(),"Premium_Yearly_"+MigrationCodec.ym(month),
    PremiumV26Analytics.annualCsv(data));
   runOnUiThread(()->{
    try{
     PremiumV2797CsvShare.verify(attachment);
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",attachment.file);
     Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/csv");
     send.putExtra(Intent.EXTRA_STREAM,uri);
     send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(send,"Save or share verified yearly CSV"));
    }catch(Exception ex){
     attachment.file.delete();
     error(new IOException("Could not share verified yearly CSV: "+ex.getMessage(),ex));
    }
   });
  });
 }
'''
# The earlier v27.7 build overlay already inserts an authoritative
# chronological/duplicate-period check in both entry methods. Keep it intact.
chronology='''   String[] checkedMonths=new String[data.size()];
   for(int i=0;i<data.size();i++)checkedMonths[i]=data.get(i).month;
   PremiumV27TrendAudit.requireOrdered(checkedMonths);
'''
base_start="  background(()->{\n   File file="
safe_start="  background(()->{\n   final PremiumV2797CsvShare.Share attachment="
if old.count(base_start)!=2 or new.count(safe_start)!=2:
 raise AssertionError("CSV overlay method count is unexpected")
old=old.replace(base_start,"  background(()->{\n"+chronology+"   File file=")
new=new.replace(safe_start,"  background(()->{\n"+chronology+"   final PremiumV2797CsvShare.Share attachment=")

if s.count(old)!=1:
 raise AssertionError(f"CSV share source contract changed; found {s.count(old)}")
s=s.replace(old,new,1)
for term in ("PremiumV2797CsvShare.create(","PremiumV2797CsvShare.verify(attachment)"):
 if term not in s:raise AssertionError("Missing CSV safety hook: "+term)
p.write_text(s,encoding="utf-8")
(root/"PremiumV2797CsvShare.java").write_text(
 Path("src/premium/v27_9_7/PremiumV2797CsvShare.java").read_text(encoding="utf-8"),
 encoding="utf-8")
print("PASS v27.9.7 immutable unique verified CSV share attachments, old ledger untouched")

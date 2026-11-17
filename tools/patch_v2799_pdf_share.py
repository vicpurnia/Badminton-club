#!/usr/bin/env python3
"""v27.9.9: share immutable verified PDF snapshots, not renderer cache filenames.

Never alter the existing document generators, Room v1 or stable 842x595 Classic
Master format. Keep accepted share files available to Android recipients.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=root/"MainActivity.java"
s=activity.read_text(encoding="utf-8")
old=''' private void sharePdf(String filename,RunnablePdf renderer){background(()->{
  if(database.dao().meta()==null)throw new IllegalStateException("Import your verified ledger first");
  File file=renderer.make();
  if(file==null||!file.isFile()||file.length()<100)throw new IOException("PDF could not be generated");
  verifyClassicPdf(file);
  runOnUiThread(()->{
   try{
    Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
    Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/pdf");send.putExtra(Intent.EXTRA_STREAM,uri);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(send,"Share PDF"));
   }catch(Exception ex){error(new IOException("Could not open Android sharing. Use Save PDF to phone instead."));}
  });
 });}'''
new=''' private void sharePdf(String filename,RunnablePdf renderer){background(()->{
  requireImportedLedger();
  File original=renderer.make();
  if(original==null||!original.isFile()||original.length()<100)
   throw new IOException("PDF could not be generated");
  // The original one-page Classic 842x595 contract stays authoritative.
  verifyClassicPdf(original);
  // Historic renderer names sometimes have millisecond granularity. Preserve
  // the attachment already handed to another app even if another report with
  // that renderer path is generated while its share sheet is still open.
  final PremiumV2799PdfShare.Share copy=
   PremiumV2799PdfShare.snapshot(shareCacheDir(),original);
  PremiumV2799PdfShare.verify(copy);
  runOnUiThread(()->{
   try{
    // Quick no-IO size guard on UI: the actual SHA-256 verification above ran
    // on the worker before opening the share chooser.
    if(!copy.file.isFile()||copy.file.length()!=copy.length)
     throw new IOException("Prepared PDF was removed before sharing");
    Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",copy.file);
    Intent send=new Intent(Intent.ACTION_SEND);
    send.setType("application/pdf");
    send.putExtra(Intent.EXTRA_STREAM,uri);
    send.setClipData(ClipData.newRawUri("Verified Badminton PDF",uri));
    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(send,"Share PDF"));
    // DO NOT delete the recipient's attachment on share-sheet return. Android
    // and WhatsApp may read a content URI asynchronously after selection.
   }catch(Exception ex){
    copy.file.delete();
    error(new IOException("Could not share verified PDF. Use Save PDF to phone instead: "+
     ex.getMessage(),ex));
   }
  });
 });}'''
if s.count(old)!=1:raise AssertionError(f"Original PDF share source anchor: {s.count(old)} expected 1")
s=s.replace(old,new,1)
for required in ("PremiumV2799PdfShare.snapshot","PremiumV2799PdfShare.verify(copy)",
                 "ClipData.newRawUri","verifyClassicPdf(original)"):
 if required not in s:raise AssertionError("Missing generated PDF share guard "+required)
activity.write_text(s,encoding="utf-8")
(root/"PremiumV2799PdfShare.java").write_text(
 Path("src/premium/v27_9_9/PremiumV2799PdfShare.java").read_text(encoding="utf-8"),
 encoding="utf-8")
print("PASS v27.9.9 PDF share snapshot unique, SHA-pinned, FileProvider clip grants and Classic unchanged")

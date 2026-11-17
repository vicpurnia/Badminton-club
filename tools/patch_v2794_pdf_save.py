#!/usr/bin/env python3
"""v27.9.4 PDF Documents Save: single-flight + source/readback SHA-256.

Patch the original v26 Activity only AFTER the prior v27 overlays.
The original PDF generation and Room ledger remain unchanged.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
p=root/"MainActivity.java"
s=p.read_text(encoding="utf-8")
def once(old,new):
 global s
 if s.count(old)!=1:
  raise AssertionError(f"PDF Save source anchor changed ({s.count(old)}): {old[:110]}")
 s=s.replace(old,new,1)

once(" private File pendingExport,pendingReportPdf;",
     " private File pendingExport,pendingReportPdf;\n private final PremiumV2794PdfSaveGuard pdfSaveGate=new PremiumV2794PdfSaveGuard();")

once('''  if(req==SAVE_PDF){
   File pdf=pendingReportPdf;pendingReportPdf=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){if(pdf!=null)pdf.delete();return;}
   if(pdf==null||!pdf.exists()){error(new IOException("PDF export expired. Please generate it again."));return;}
   Uri destination=data.getData();
   background(()->{try(InputStream in=new FileInputStream(pdf);OutputStream out=getContentResolver().openOutputStream(destination,"w")){
    if(out==null)throw new IOException("Cannot open the selected PDF destination");
    byte[] buffer=new byte[16384];int n;
    while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
    out.flush();runOnUiThread(()->Toast.makeText(this,"PDF saved successfully",Toast.LENGTH_LONG).show());
   }finally{pdf.delete();}});
   return;
  }''',
     '''  if(req==SAVE_PDF){
   File pdf=pendingReportPdf;pendingReportPdf=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){
    if(pdf!=null)pdf.delete();
    pdfSaveGate.finish();
    return;
   }
   if(pdf==null||!pdf.isFile()){
    pdfSaveGate.finish();
    error(new IOException("PDF export expired. Generate and verify it again."));
    return;
   }
   Uri destination=data.getData();
   background(()->{
    try{
     try(InputStream in=new FileInputStream(pdf);
         OutputStream out=getContentResolver().openOutputStream(destination,"w")){
      if(out==null)throw new IOException("Cannot open the selected PDF destination");
      byte[] buffer=new byte[16384];int n;
      while((n=in.read(buffer))!=-1){
       if(n>0)out.write(buffer,0,n);
      }
      out.flush();
     }
     // A successful Android Documents write is not proof the provider saved
     // the exact content. Reopen and compare to the original staged SHA-256.
     try(InputStream saved=getContentResolver().openInputStream(destination)){
      pdfSaveGate.verify(pdf,saved);
     }
     runOnUiThread(()->Toast.makeText(this,
      "PDF saved and SHA-256 verified",Toast.LENGTH_LONG).show());
    }catch(Exception failure){
     throw new IOException("PDF export was not verified. Recheck the saved document before sharing: "+
      failure.getMessage(),failure);
    }finally{
     pdf.delete();
     pdfSaveGate.finish();
    }
   });
   return;
  }''')

once(''' private void savePdf(String name,RunnablePdf renderer){background(()->{
  if(database.dao().meta()==null)throw new IllegalStateException("Import your verified ledger first");
  File pdf=renderer.make();
  if(pdf==null||!pdf.isFile()||pdf.length()<100)throw new IOException("PDF could not be generated");
  verifyClassicPdf(pdf);
  pendingReportPdf=pdf;
  runOnUiThread(()->{
    Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/pdf");
    save.putExtra(Intent.EXTRA_TITLE,name);
    try{startActivityForResult(save,SAVE_PDF);}
    catch(Exception e){pendingReportPdf=null;error(new IOException("No available app to save PDF"));}
  });
 });}''',
     ''' private void savePdf(String name,RunnablePdf renderer){
  if(!pdfSaveGate.begin()){
   error(new IllegalStateException("Another PDF Save picker is active. Finish or cancel that save first."));
   return;
  }
  background(()->{
   File pdf=null;
   try{
    if(database.dao().meta()==null)
     throw new IllegalStateException("Import your verified ledger first");
    pdf=renderer.make();
    if(pdf==null||!pdf.isFile()||pdf.length()<100)
     throw new IOException("PDF could not be generated");
    verifyClassicPdf(pdf);
    pdfSaveGate.stage(pdf);
    pendingReportPdf=pdf;
    final File staged=pdf;
    runOnUiThread(()->{
     Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
     save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/pdf");
     save.putExtra(Intent.EXTRA_TITLE,name);
     try{startActivityForResult(save,SAVE_PDF);}
     catch(Exception e){
      pendingReportPdf=null;
      staged.delete();
      pdfSaveGate.finish();
      error(new IOException("No available app to save PDF"));
     }
    });
   }catch(Exception e){
    if(pdf!=null)pdf.delete();
    pdfSaveGate.finish();
    throw e;
   }
  });
 }''')

p.write_text(s,encoding="utf-8")
(root/"PremiumV2794PdfSaveGuard.java").write_text(
 Path("src/premium/v27_9_4/PremiumV2794PdfSaveGuard.java").read_text(encoding="utf-8"),
 encoding="utf-8")
for anchor in ("pdfSaveGate.begin()","pdfSaveGate.stage(pdf)","pdfSaveGate.verify(pdf,saved)",
               "PDF saved and SHA-256 verified",
               "pdfSaveGate.finish()"):
 if anchor not in s:raise AssertionError("PDF save guard missing "+anchor)
print("PASS v27.9.4: PDF picker one-at-a-time and byte-verified saved copy")

package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.util.Locale;

/** v27.4 supplementary cash-expense voucher, not an external vendor invoice. */
final class PremiumV27ExpenseVoucher {
 private PremiumV27ExpenseVoucher(){}
 static File make(Context context,Expense e)throws IOException {
  if(context==null||e==null||e.id==null||e.id.trim().isEmpty()||
    e.month==null||!e.month.matches("[0-9]{4}-(0[1-9]|1[0-2])")||
    e.amount<=0||e.quantity<=0)
   throw new IOException("A saved expense with valid ID, month, quantity and cash amount is required");
  File dir=new File(context.getCacheDir(),"pdfs");
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Voucher folder unavailable");
  File file=new File(dir,"Shuttle_Expense_Voucher_"+e.id.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");
  PdfDocument pdf=new PdfDocument();
  try {
   PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
   Canvas canvas=page.getCanvas();
   Paint paint=new Paint(3);canvas.drawColor(Color.WHITE);
   paint.setColor(Color.rgb(20,75,59));paint.setTextSize(22);
   paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
   canvas.drawText("BADMINTON PREMIUM",42,60,paint);
   paint.setTextSize(16);canvas.drawText("INTERNAL EXPENSE VOUCHER",42,91,paint);
   paint.setStrokeWidth(1);canvas.drawLine(42,111,553,111,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setColor(Color.rgb(35,50,44));paint.setTextSize(13);
   int y=151;
   String[] fields={
    "Reference ID: "+e.id,
    "Expense category: "+(e.category==null?"Not specified":e.category),
    "Accounting month: "+e.month,
    "Purchase date: "+((e.date==null||e.date.trim().isEmpty())?"Not recorded":e.date),
    "Recorded quantity (packs): "+e.quantity,
    "Recorded cash expense (INR): "+String.format(Locale.ROOT,"%d",e.amount)
   };
   for(String row:fields){canvas.drawText(row,42,y,paint);y+=35;}
   paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
   paint.setColor(Color.rgb(132,55,45));paint.setTextSize(12);
   canvas.drawText("SELF-GENERATED RECORD - NOT A SUPPLIER TAX INVOICE",42,y+24,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setColor(Color.rgb(78,84,78));paint.setTextSize(11);
   canvas.drawText("Original expenditure ledger remains authoritative.",42,y+56,paint);
   canvas.drawText("No external bill was attached or independently authenticated.",42,y+75,paint);
   canvas.drawText("This document does not create or modify an expense.",42,y+94,paint);
   pdf.finishPage(page);
   try(FileOutputStream out=new FileOutputStream(file)){pdf.writeTo(out);}
  }catch(Exception ex){
   file.delete();
   if(ex instanceof IOException)throw (IOException)ex;
   throw new IOException("Could not generate internal voucher",ex);
  }finally{pdf.close();}
  if(!file.isFile()||file.length()<200)throw new IOException("Internal expense voucher verification failed");
  return file;
 }
}

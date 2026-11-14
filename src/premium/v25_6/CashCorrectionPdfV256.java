package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Clear correction/cancellation copy. Never represents the original payment as a new receipt. */
final class CashCorrectionPdfV256 {
 private static final Paint PEN=new Paint(Paint.ANTI_ALIAS_FLAG);
 private static final int GREEN=Color.rgb(14,107,77),DARK=Color.rgb(21,53,43),GRAY=Color.rgb(108,121,114),
  PALE=Color.rgb(230,245,237),RED=Color.rgb(155,45,43);
 private CashCorrectionPdfV256(){}
 private static void rect(Canvas c,int color,float x,float y,float w,float h){
  PEN.setStyle(Paint.Style.FILL);PEN.setColor(color);c.drawRect(x,y,x+w,y+h,PEN);
 }
 private static void line(Canvas c,String raw,float x,float y,int size,int color,boolean bold){
  PEN.setColor(color);PEN.setTextSize(size);
  PEN.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
  String s=raw==null?"Not recorded":raw;
  while(s.length()>3&&PEN.measureText(s)>505)s=s.substring(0,s.length()-2)+"…";
  c.drawText(s,x,y,PEN);
 }
 private static void pair(Canvas c,String label,String value,int y){
  line(c,label,45,y,11,GRAY,true);line(c,value,45,y+23,16,DARK,true);
 }
 static File make(Context ctx,Member member,Contribution p,LedgerMeta meta,CashCorrectionsV256.State status)
  throws IOException{
  if(member==null||p==null||meta==null||status==null||!"cash".equals(p.kind)||
    !p.memberId.equals(member.id)||!status.corrected())
   throw new IllegalArgumentException("An existing corrected cash payment is required.");
  PdfDocument doc=new PdfDocument();PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
  Canvas c=page.getCanvas();boolean canceled=status.effective==0;
  rect(c,GREEN,0,0,595,119);
  line(c,meta.club.toUpperCase(Locale.ROOT),39,54,21,Color.WHITE,true);
  line(c,canceled?"CANCELED PAYMENT RECORD":"CORRECTED PAYMENT RECEIPT COPY",39,90,12,Color.WHITE,true);
  rect(c,PALE,40,141,516,56);
  line(c,"READ-ONLY RECORD · NOT A NEW COLLECTION",55,174,11,GREEN,true);
  pair(c,"MEMBER",member.name,229);
  pair(c,"ORIGINAL RECORDED CASH", "INR "+status.original,292);
  pair(c,"CURRENT RECOGNIZED CASH", "INR "+status.effective,355);
  pair(c,"FEE MONTH",p.feeMonth,418);
  pair(c,"ORIGINAL PAYMENT DATE",p.approximate||p.receivedDate==null||p.receivedDate.isEmpty()?
   "Exact date not recorded":p.receivedDate,481);
  pair(c,"ORIGINAL PAYMENT ID",p.id,544);
  rect(c,PALE,40,593,516,122);
  line(c,"LATEST ACTION: "+status.action,53,618,12,canceled?RED:GREEN,true);
  line(c,"Reason: "+status.reason,53,646,10,DARK,false);
  line(c,"Previous corrections are retained in the verified audit history.",53,673,10,DARK,false);
  line(c,"No new cash payment was created for this document.",53,692,10,DARK,false);
  line(c,"Generated from local ledger: "+new SimpleDateFormat("dd MMM yyyy, HH:mm",Locale.ENGLISH).format(new Date()),
    43,760,10,GRAY,false);
  line(c,"Generated date is NOT the original payment date.",43,783,10,GRAY,false);
  doc.finishPage(page);
  File dir=new File(ctx.getCacheDir(),"pdfs");
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("PDF folder unavailable");
  File out=new File(dir,"Corrected_Receipt_"+System.currentTimeMillis()+".pdf");
  try(FileOutputStream stream=new FileOutputStream(out)){doc.writeTo(stream);}
  finally{doc.close();}
  return out;
 }
}

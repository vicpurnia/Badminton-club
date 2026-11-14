package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Read-only, shareable copies of already-recorded payments. Never creates ledger entries. */
final class HistoricalReceiptsV253 {
 private static final int GREEN=Color.rgb(14,107,77), DARK=Color.rgb(21,53,43),
   MUTED=Color.rgb(99,112,105),PALE=Color.rgb(234,246,239),LINE=Color.rgb(205,221,212);
 private HistoricalReceiptsV253(){}
 private static final class Pen {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  void box(Canvas c,int color,float x,float y,float w,float h){
   paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRect(x,y,x+w,y+h,paint);
  }
  void text(Canvas c,String raw,float x,float y,int size,int color,boolean bold,float maxWidth){
   String s=raw==null?"Not recorded":raw.trim();
   paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
   paint.setTextSize(size);paint.setColor(color);
   if(maxWidth>0){
    while(s.length()>3 && paint.measureText(s)>maxWidth)
     s=s.substring(0,s.length()-2)+"…";
   }
   c.drawText(s,x,y,paint);
  }
 }
 private static String digits(long value){
  return String.format(new Locale("en","IN"),"%,d",value);
 }
 private static String generated(){
  return new SimpleDateFormat("dd MMM yyyy, HH:mm",Locale.ENGLISH).format(new Date());
 }
 private static File save(Context context,PdfDocument doc) throws IOException{
  File dir=new File(context.getCacheDir(),"pdfs");
  if(!dir.exists()&&!dir.mkdirs()){doc.close();throw new IOException("Could not create PDF folder");}
  File output=new File(dir,"Historical_Contribution_"+System.currentTimeMillis()+".pdf");
  try(FileOutputStream out=new FileOutputStream(output)){
   doc.writeTo(out);out.flush();
  }finally{doc.close();}
  if(output.length()<100)throw new IOException("Historical PDF is empty");
  return output;
 }
 private static PdfDocument.Page start(PdfDocument doc,Pen p,String club,String kind){
  PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
  Canvas c=page.getCanvas();
  p.box(c,GREEN,0,0,595,125);
  p.text(c,club==null?"BADMINTON CLUB":club.toUpperCase(Locale.ROOT),38,54,19,Color.WHITE,true,500);
  p.text(c,kind,38,88,12,Color.WHITE,true,500);
  p.box(c,PALE,40,145,515,52);
  p.text(c,"READ-ONLY COPY OF EXISTING CLUB RECORD",54,174,11,GREEN,true,485);
  return page;
 }
 private static void field(Canvas c,Pen p,float top,String name,String value){
  p.text(c,name.toUpperCase(Locale.ROOT),48,top,10,MUTED,true,490);
  p.text(c,value,48,top+28,15,DARK,true,490);
 }
 private static void footer(Canvas c,Pen p){
  p.box(c,LINE,42,745,510,1);
  p.text(c,"Generated from the club's locally stored ledger on "+generated(),43,767,9,MUTED,false,505);
  p.text(c,"This copy does not record a new payment or change any balance.",43,785,9,MUTED,false,505);
 }
 static File payment(Context ctx,Member member,Contribution c,LedgerMeta meta) throws Exception {
  if(member==null||c==null||meta==null||!"cash".equals(c.kind)||c.amount<=0)
   throw new IllegalArgumentException("Only an existing positive cash payment can be reissued.");
  if(!member.id.equals(c.memberId))
   throw new IllegalArgumentException("The selected payment does not belong to this player.");
  PdfDocument doc=new PdfDocument();Pen p=new Pen();
  PdfDocument.Page page=start(doc,p,meta.club,"PAST PAYMENT  |  RECEIPT COPY");
  Canvas canvas=page.getCanvas();
  field(canvas,p,230,"Received from",member.name);
  field(canvas,p,295,"Cash contribution", "INR "+digits(c.amount));
  field(canvas,p,360,"Fee period",c.feeMonth==null?"Not recorded":c.feeMonth);
  String date=(c.approximate || c.receivedDate==null||c.receivedDate.trim().isEmpty())?
    "Exact payment date not recorded":c.receivedDate;
  field(canvas,p,425,"Payment date",date);
  String method=(c.method==null||c.method.trim().isEmpty())?"Not recorded":c.method;
  field(canvas,p,490,"Payment method",method);
  field(canvas,p,555,"Original ledger entry ID",c.id);
  p.box(canvas,PALE,41,611,513,105);
  p.text(canvas,"Record type: "+(c.historical||c.approximate?"Imported / historical entry":"Previously recorded payment"),
    52,633,11,GREEN,true,480);
  p.text(canvas,"This document reproduces an already-entered cash payment.",52,658,10,DARK,false,485);
  p.text(canvas,"Unknown dates or methods have not been invented.",52,681,10,DARK,false,485);
  footer(canvas,p);doc.finishPage(page);return save(ctx,doc);
 }
 static File legacy(Context ctx,Member member,LegacyEntry e,LedgerMeta meta) throws Exception{
  if(member==null||e==null||meta==null||!"cash".equals(e.kind)||e.amount<=0)
   throw new IllegalArgumentException("Only positive historical cash records can be shared.");
  if(!member.id.equals(e.memberId))
   throw new IllegalArgumentException("The selected historic entry does not belong to this player.");
  PdfDocument doc=new PdfDocument();Pen p=new Pen();
  PdfDocument.Page page=start(doc,p,meta.club,"HISTORICAL CONTRIBUTION STATEMENT");
  Canvas canvas=page.getCanvas();
  field(canvas,p,232,"Member",member.name);
  field(canvas,p,310,"Historical cash amount", "INR "+digits(e.amount));
  field(canvas,p,388,"Original ledger period code",e.period==null?"Not recorded":e.period);
  field(canvas,p,466,"Exact payment date","Not available in imported history");
  field(canvas,p,544,"Payment method","Not available in imported history");
  p.box(canvas,PALE,42,622,511,103);
  p.text(canvas,"HISTORICAL PERIOD RECORD  |  NOT A NEW RECEIPT",54,644,10,GREEN,true,480);
  p.text(canvas,"This statement reflects the original archived period total.",54,671,10,DARK,false,480);
  p.text(canvas,"It does not certify an individual payment date or payment method.",54,695,10,DARK,false,485);
  footer(canvas,p);doc.finishPage(page);return save(ctx,doc);
 }
}

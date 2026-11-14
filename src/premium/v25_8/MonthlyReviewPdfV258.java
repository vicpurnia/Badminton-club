package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.util.*;

/** Readable one-page A4 landscape month-end review; never replaces the Classic Master. */
final class MonthlyReviewPdfV258 {
 private static final Paint P=new Paint(Paint.ANTI_ALIAS_FLAG);
 private static final int GREEN=Color.rgb(14,107,77),DARK=Color.rgb(21,53,43),
  GRAY=Color.rgb(102,117,108),PALE=Color.rgb(234,246,239),RED=Color.rgb(166,49,49);
 private MonthlyReviewPdfV258(){}
 private static void box(Canvas c,int color,float x,float y,float w,float h){
  P.setStyle(Paint.Style.FILL);P.setColor(color);c.drawRoundRect(x,y,x+w,y+h,5,5,P);
 }
 private static void text(Canvas c,String raw,float x,float y,int size,int color,boolean bold,float max){
  String s=raw==null?"":raw;
  P.setColor(color);P.setTextSize(size);P.setTypeface(Typeface.create("sans-serif",
   bold?Typeface.BOLD:Typeface.NORMAL));P.setStyle(Paint.Style.FILL);
  if(max>0)while(s.length()>4&&P.measureText(s)>max)s=s.substring(0,s.length()-2)+"…";
  c.drawText(s,x,y,P);
 }
 private static String money(long n){
  return "INR "+(n<0?"-":"")+String.format(new Locale("en","IN"),"%,d",Math.abs(n));
 }
 private static void metric(Canvas c,float x,float y,String label,long number){
  box(c,PALE,x,y,149,65);
  text(c,label,x+9,y+20,10,GRAY,true,132);
  text(c,money(number),x+9,y+47,18,DARK,true,132);
 }
 static File make(Context ctx,LedgerDao dao,String selectedMonth)throws Exception{
  LedgerMeta meta=dao.meta();if(meta==null)throw new IllegalArgumentException("Import ledger first");
  String month=MigrationCodec.ym(selectedMonth);
  ReportMath.Result row=null;
  for(ReportMath.Result found:ReportMath.monthly(dao,month))if(month.equals(found.month))row=found;
  if(row==null)throw new IllegalArgumentException("Month predates the opening ledger");
  Map<String,MonthlyCloseV258.MonthState> status=MonthlyCloseV258.read(meta.inventoryStockJson);
  boolean closed=status.containsKey(month)&&status.get(month).closed();
  List<Contribution> payments=dao.contributions();
  Map<String,CashCorrectionsV256.State> corrected=CashCorrectionsV256.states(meta.inventoryStockJson,payments);
  Map<String,String> memberNames=new HashMap<>();
  for(Member member:dao.members())memberNames.put(member.id,member.name);
  List<String> receipts=new ArrayList<>();int totalCash=0,voided=0;
  for(Contribution c:payments)if(month.equals(c.feeMonth)&&"cash".equals(c.kind)){
   long effective=corrected.containsKey(c.id)?corrected.get(c.id).effective:c.amount;
   if(effective==0){voided++;continue;}
   totalCash++;
   receipts.add(memberNames.getOrDefault(c.memberId,"Member")+" · "+money(effective));
  }
  List<String> spending=new ArrayList<>();long packs=0;
  for(Expense e:dao.expenses())if(month.equals(e.month)){
   spending.add(e.category+" · "+money(e.amount));
   if(e.category!=null&&e.category.toLowerCase(Locale.ROOT).contains("shuttle"))packs+=e.quantity;
  }
  long donated=0,used=0;
  for(Contribution c:payments)if(month.equals(c.feeMonth)&&"packs".equals(c.kind))donated+=c.amount;
  for(StockUsage item:dao.stockUsages())if(month.equals(item.month))used+=item.shuttles;
  StockReconciliationV257.Position stock=StockReconciliationV257.calculate(meta.inventoryStockJson,month,
    dao.expenses(),payments,dao.stockUsages());

  PdfDocument document=new PdfDocument();
  PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());
  Canvas canvas=page.getCanvas();
  box(canvas,GREEN,0,0,842,75);
  text(canvas,meta.club.toUpperCase(Locale.ROOT),22,27,13,Color.WHITE,true,750);
  text(canvas,"MONTH-END FINANCIAL REVIEW · "+month,22,55,22,Color.WHITE,true,670);
  box(canvas,closed?0xFF155F4C:0xFF496C5D,690,22,124,31);
  text(canvas,closed?"CLOSED / AUDITED":"OPEN / REVIEW",699,42,11,Color.WHITE,true,104);
  metric(canvas,21,98,"OPENING BALANCE",row.opening);
  metric(canvas,186,98,"COLLECTIONS",row.collections);
  metric(canvas,351,98,"EXPENDITURE",row.expenses);
  metric(canvas,516,98,"CLOSING BALANCE",row.closing);
  box(canvas,PALE,682,98,140,65);
  text(canvas,"RECONCILIATION",690,119,10,GRAY,true,125);
  text(canvas,money(row.difference),690,146,16,row.difference==0?GREEN:RED,true,125);
  if(row.difference!=0)
   text(canvas,"Unresolved official-minus-member difference: "+money(row.difference),
    21,185,11,RED,true,795);
  else text(canvas,"Official collection and member-entry total currently reconcile.",21,185,11,GREEN,true,795);
  text(canvas,"RECORDED CASH CONTRIBUTIONS ("+totalCash+")",22,213,13,GREEN,true,372);
  text(canvas,"RECORDED EXPENDITURES ("+spending.size()+")",430,213,13,GREEN,true,380);
  box(canvas,PALE,18,224,392,228);box(canvas,PALE,425,224,399,228);
  int limit=9;
  for(int i=0;i<Math.min(limit,receipts.size());i++)
   text(canvas,(i+1)+". "+receipts.get(i),28,250+i*21,10,DARK,false,369);
  for(int i=0;i<Math.min(limit,spending.size());i++)
   text(canvas,(i+1)+". "+spending.get(i),437,250+i*21,10,DARK,false,374);
  if(receipts.size()>limit)text(canvas,"+"+(receipts.size()-limit)+" more receipts in source ledger",
    28,440,10,GRAY,true,369);
  if(spending.size()>limit)text(canvas,"+"+(spending.size()-limit)+" more expenses in source ledger",
    437,440,10,GRAY,true,374);
  box(canvas,PALE,18,470,807,59);
  text(canvas,"SHUTTLE ACTIVITY",28,487,10,GREEN,true,175);
  text(canvas,packs+" packs purchased · "+donated+" packs donated · "+used+" individual shuttles used",
    28,505,11,DARK,false,510);
  text(canvas,stock==null?"Remaining stock: no confirmed checkpoint":
    "Calculated remaining stock: "+stock.remainingShuttles+" individual shuttles",
    540,505,10,DARK,true,267);
  text(canvas,"Canceled cash entries (retained in audit): "+voided+
   " · This one-page summary does not replace the complete cashbook or Classic Master.",
    21,554,9,GRAY,false,792);
  text(canvas,"Printed from existing verified ledger. Missing historical payment dates are not invented.",
    21,577,9,GRAY,false,793);
  document.finishPage(page);
  File folder=new File(ctx.getCacheDir(),"pdfs");
  if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Cannot create PDF directory");
  File output=new File(folder,"Month_End_Review_"+month+"_"+System.currentTimeMillis()+".pdf");
  try(FileOutputStream stream=new FileOutputStream(output)){document.writeTo(stream);}
  finally{document.close();}
  return output;
 }
}

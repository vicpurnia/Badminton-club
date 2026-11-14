package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Independent analytics; never modifies the Classic Master Ledger or accounting rows. */
final class PremiumV26Analytics {
 static final class Month {
  final String month;
  final long collections,expenses,closing,difference,usage,acquired;
  Month(String m,long c,long e,long end,long d,long u,long a){
   month=m;collections=c;expenses=e;closing=end;difference=d;usage=u;acquired=a;
  }
 }
 private PremiumV26Analytics(){}
 static List<Month> timeline(LedgerDao dao,String through){
  LedgerMeta meta=dao.meta();
  if(meta==null)throw new IllegalStateException("Verified ledger required");
  String month=MigrationCodec.ym(through);
  List<ReportMath.Result> statements=ReportMath.monthly(dao,month);
  ArrayList<Month> result=new ArrayList<>();
  List<StockUsage> usage=dao.stockUsages();
  List<Expense> expenses=dao.expenses();
  List<Contribution> payments=dao.contributions();
  InventoryCheckpoint.Config config=InventoryCheckpoint.read(meta.inventoryStockJson);
  for(ReportMath.Result r:statements){
   if(r.month.compareTo(month)>0)continue;
   long count=0,acquired=0;
   for(StockUsage u:usage)if(r.month.equals(u.month))count=Math.addExact(count,u.shuttles);
   if(config!=null){
    for(Expense e:expenses)if(r.month.equals(e.month)&&e.category!=null&&
       e.category.toLowerCase(Locale.ROOT).contains("shuttle"))
     acquired=Math.addExact(acquired,Math.multiplyExact(e.quantity,
       StockReconciliationV257.packSize(e.note,config.perPack)));
    for(Contribution c:payments)if(r.month.equals(c.feeMonth)&&"packs".equals(c.kind))
     acquired=Math.addExact(acquired,Math.multiplyExact(c.amount,
       StockReconciliationV257.packSize(c.note,config.perPack)));
   }
   result.add(new Month(r.month,r.collections,r.expenses,r.closing,r.difference,count,acquired));
  }
  return result;
 }
 static String csv(List<Month> months){
  StringBuilder s=new StringBuilder("Month,Collections_INR,Expenses_INR,Closing_INR,Unresolved_Difference_INR,Recorded_Shuttles_Used,Recorded_Shuttles_Acquired\r\n");
  for(Month m:months)s.append(m.month).append(',').append(m.collections).append(',')
    .append(m.expenses).append(',').append(m.closing).append(',')
    .append(m.difference).append(',').append(m.usage).append(',')
    .append(m.acquired).append("\r\n");
  return s.toString();
 }
 static File pdf(Context context,List<Month> data,String through)throws Exception{
  if(data.isEmpty())throw new IllegalArgumentException("No verified reporting months");
  PdfDocument document=new PdfDocument();
  Paint p=new Paint(3);
  PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());
  Canvas c=page.getCanvas();c.drawColor(Color.WHITE);
  p.setColor(Color.rgb(21,75,60));p.setTextSize(20);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
  c.drawText("BADMINTON PREMIUM  |  FINANCIAL DASHBOARD",36,43,p);
  p.setColor(Color.rgb(100,112,105));p.setTextSize(11);p.setTypeface(Typeface.DEFAULT);
  c.drawText("Verified recorded amounts through "+through+"  •  This report does not replace the Classic Master Ledger",36,66,p);
  p.setColor(Color.rgb(15,83,60));p.setTextSize(11);p.setTypeface(Typeface.DEFAULT_BOLD);
  c.drawText("MONTH",36,99,p);c.drawText("RECEIVED",185,99,p);c.drawText("EXPENSE",295,99,p);
  c.drawText("CLOSING",402,99,p);c.drawText("DIFFERENCE",507,99,p);
  c.drawText("USED",650,99,p);c.drawText("ACQUIRED",730,99,p);
  int start=Math.max(0,data.size()-12);
  long totalIn=0,totalOut=0,totalUsed=0,totalAcq=0;
  float y=119;
  for(int i=start;i<data.size();i++){
   Month m=data.get(i);
   if((i-start)%2==0){p.setColor(Color.rgb(244,248,246));c.drawRect(30,y-15,812,y+12,p);}
   p.setTextSize(11);p.setTypeface(Typeface.DEFAULT);
   p.setColor(Color.rgb(35,53,46));
   c.drawText(m.month,36,y,p);c.drawText(String.valueOf(m.collections),185,y,p);
   c.drawText(String.valueOf(m.expenses),295,y,p);c.drawText(String.valueOf(m.closing),402,y,p);
   if(m.difference!=0)p.setColor(Color.rgb(150,44,45));
   c.drawText(String.valueOf(m.difference),507,y,p);
   p.setColor(Color.rgb(35,53,46));c.drawText(String.valueOf(m.usage),650,y,p);
   c.drawText(String.valueOf(m.acquired),730,y,p);
   totalIn=Math.addExact(totalIn,m.collections);totalOut=Math.addExact(totalOut,m.expenses);
   totalUsed=Math.addExact(totalUsed,m.usage);totalAcq=Math.addExact(totalAcq,m.acquired);
   y+=31;
  }
  p.setColor(Color.rgb(17,78,62));p.setTextSize(12);p.setTypeface(Typeface.DEFAULT_BOLD);
  c.drawText("Displayed totals",36,527,p);c.drawText(String.valueOf(totalIn),185,527,p);
  c.drawText(String.valueOf(totalOut),295,527,p);c.drawText(String.valueOf(totalUsed),650,527,p);
  c.drawText(String.valueOf(totalAcq),730,527,p);
  p.setTextSize(9);p.setTypeface(Typeface.DEFAULT);p.setColor(Color.rgb(80,95,87));
  c.drawText("Usage and acquisitions are recorded quantities only. Unknown historical quantities are not estimated.",36,561,p);
  c.drawText("Any unresolved reconciliation difference remains visible and is not silently settled.",36,576,p);
  document.finishPage(page);
  File file=new File(context.getCacheDir(),"Premium_Analytics_"+through+".pdf");
  try(FileOutputStream out=new FileOutputStream(file)){document.writeTo(out);}
  finally{document.close();}
  return file;
 }
}

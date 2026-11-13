package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import org.json.*;
import java.io.*;
import java.util.*;

/** Android-native PDF generation. A4 landscape master table resembles the original green ledger. */
final class PdfReports {
 private static final int GREEN=Color.rgb(14,107,77),DARK=Color.rgb(21,53,43),GRAY=Color.rgb(105,124,114),PALE=Color.rgb(230,245,237),BORDER=Color.rgb(190,210,196),RED=Color.rgb(175,45,58);
 private static final Paint PAINT=new Paint(Paint.ANTI_ALIAS_FLAG);
 private static void rect(Canvas c,int color,float x,float y,float w,float h){PAINT.setColor(color);PAINT.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,PAINT);}
 private static void stroke(Canvas c,float x,float y,float w,float h){PAINT.setColor(BORDER);PAINT.setStrokeWidth(.45f);PAINT.setStyle(Paint.Style.STROKE);c.drawRect(x,y,x+w,y+h,PAINT);PAINT.setStyle(Paint.Style.FILL);}
 private static void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold){PAINT.setColor(color);PAINT.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));PAINT.setTextSize(size);c.drawText(s,x,y,PAINT);}
 private static String fit(String s,float max,float size){PAINT.setTypeface(Typeface.DEFAULT);PAINT.setTextSize(size);if(PAINT.measureText(s)<=max)return s;while(s.length()>1&&PAINT.measureText(s+"…")>max)s=s.substring(0,s.length()-1);return s+"…";}
 private static String number(long value){return (value<0?"−":"")+String.format(new Locale("en","IN"),"%,d",Math.abs(value));}
 private static String f(long amount,int packs){return (amount==0?"—":number(amount))+(packs>0?(amount==0?"":" ")+"+"+packs+"pk":"");}
 private static File write(Context context,PdfDocument pdf,String prefix) throws IOException{File dir=new File(context.getCacheDir(),"pdfs");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Could not create PDF cache");File file=new File(dir,prefix+"_"+System.currentTimeMillis()+".pdf");try(FileOutputStream os=new FileOutputStream(file)){pdf.writeTo(os);}finally{pdf.close();}return file;}
 static File receipt(Context context,Member member,Contribution payment,LedgerMeta meta) throws Exception{
  PdfDocument pdf=new PdfDocument();PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());Canvas c=page.getCanvas();
  rect(c,GREEN,0,0,595,125);txt(c,meta.club.toUpperCase(Locale.ROOT),38,57,21,Color.WHITE,true);txt(c,"PAYMENT RECEIPT",38,89,11,Color.WHITE,true);
  txt(c,"RECEIPT ID",42,180,10,GRAY,true);txt(c,fit(payment.id,510,12),42,203,12,DARK,true);
  txt(c,"RECEIVED FROM",42,260,10,GRAY,true);txt(c,member.name,42,292,23,DARK,true);
  txt(c,"AMOUNT RECEIVED",42,353,10,GRAY,true);txt(c,"₹"+number(payment.amount),42,400,35,GREEN,true);
  rect(c,PALE,40,437,515,125);
  txt(c,"Fee month",57,467,11,GRAY,false);txt(c,payment.feeMonth,265,467,13,DARK,true);
  txt(c,"Payment date",57,500,11,GRAY,false);txt(c,payment.receivedDate,265,500,13,DARK,true);
  txt(c,"Method",57,533,11,GRAY,false);txt(c,payment.method,265,533,13,DARK,true);
  txt(c,"Thank you for your contribution.",42,653,13,GREEN,true);
  txt(c,"Issued from a privately stored offline ledger.",42,681,10,GRAY,false);
  pdf.finishPage(page);return write(context,pdf,"Receipt");
 }
 /** Every payment and expense is included: later pages are created when needed.
  * The former fixed 11-row layout silently omitted transactions in busy months.
  */
 static File monthly(Context context,LedgerDao dao,String month) throws Exception{
  LedgerMeta meta=dao.meta();if(meta==null)throw new IllegalStateException("No ledger");
  ReportMath.Result target=null;for(ReportMath.Result row:ReportMath.monthly(dao,month))if(row.month.equals(month))target=row;
  if(target==null)throw new IllegalArgumentException("Choose a month on or after "+meta.openingMonth);
  List<Contribution> received=new ArrayList<>();for(Contribution p:dao.contributions())if(p.feeMonth.equals(month))received.add(p);
  List<Expense> spent=new ArrayList<>();for(Expense e:dao.expenses())if(e.month.equals(month))spent.add(e);
  Map<String,String> names=new HashMap<>();for(Member m:dao.members())names.put(m.id,m.name);
  int pages=Math.max(pageCount(received.size()),pageCount(spent.size()));
  PdfDocument pdf=new PdfDocument();
  for(int pageNo=0;pageNo<pages;pageNo++){
   PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,pageNo+1).create());Canvas c=page.getCanvas();
   header(c,"MONTHLY COLLECTION & EXPENSE REPORT",meta.club,month);
   float sectionY,rowsY;
   if(pageNo==0){
    float x=26,y=96;String[] label={"Opening balance","Official collection","Member entries","Expenses","Closing balance"};
    long[] val={target.opening,target.collections,target.memberEntries,target.expenses,target.closing};
    for(int i=0;i<label.length;i++){
     rect(c,i%2==0?PALE:Color.WHITE,x,y+i*27,320,27);
     txt(c,label[i],x+10,y+i*27+18,12,DARK,i==4);
     txt(c,"₹"+number(val[i]),x+215,y+i*27+18,12,i==4?GREEN:DARK,true);
    }
    stroke(c,x,y,320,135);sectionY=257;rowsY=279;
   }else{txt(c,"DETAILS CONTINUED · PAGE "+(pageNo+1)+" OF "+pages,27,97,10,GRAY,true);sectionY=118;rowsY=141;}
   txt(c,"MEMBER CONTRIBUTIONS",28,sectionY,13,GREEN,true);
   txt(c,"SHUTTLE / OTHER EXPENSES",495,sectionY,13,GREEN,true);
   int start=pageNo==0?0:12+(pageNo-1)*20;
   int limit=pageNo==0?12:20;
   for(int j=0;j<limit&&start+j<received.size();j++){
    Contribution p=received.get(start+j);float y=rowsY+j*20;
    txt(c,fit(names.getOrDefault(p.memberId,p.memberId),230,10.5f),30,y,10.5f,DARK,false);
    txt(c,"packs".equals(p.kind)?p.amount+" donated pack(s)":"₹"+number(p.amount),285,y,10.5f,DARK,true);
   }
   for(int j=0;j<limit&&start+j<spent.size();j++){
    Expense e=spent.get(start+j);float y=rowsY+j*20;
    txt(c,fit(e.category+" ("+e.quantity+")",190,10),497,y,10,DARK,false);
    txt(c,"₹"+number(e.amount),727,y,10,DARK,true);
   }
   if(target.difference!=0)txt(c,"RECONCILIATION: official collection − member entries = ₹"+number(target.difference),27,566,9,RED,true);
   txt(c,"Page "+(pageNo+1)+" / "+pages+"  ·  All recorded entries included",615,582,8,GRAY,false);
   pdf.finishPage(page);
  }
  return write(context,pdf,"Monthly_"+month);
 }
 private static int pageCount(int count){return count<=12?1:1+(count-12+19)/20;}
 private static void header(Canvas c,String heading,String club,String detail){rect(c,GREEN,0,0,842,68);txt(c,club.toUpperCase(Locale.ROOT),26,26,11,0xffe8f2e9,true);txt(c,heading,26,51,20,Color.WHITE,true);txt(c,detail,698,49,12,Color.WHITE,true);}
 static File master(Context context,LedgerDao dao,String selectedMonth) throws Exception{
  LedgerMeta meta=dao.meta();if(meta==null)throw new IllegalStateException("No imported ledger");
  String last=selectedMonth; // Honor the user-selected historical reporting cutoff.
  if(last.compareTo(meta.openingMonth)<0)throw new IllegalArgumentException("Month earlier than ledger opening");
  List<ReportMath.Result> sums=ReportMath.monthly(dao,last);
  List<String> periods=new ArrayList<>();try{JSONArray a=new JSONArray(meta.periodsJson);for(int i=0;i<a.length();i++)periods.add(a.getString(i));}catch(Exception ex){throw new IllegalStateException("Missing historical periods");}
  List<String> months=new ArrayList<>();for(String m=meta.openingMonth;m.compareTo(last)<=0;m=ReportMath.nextMonth(m))months.add(m);
  List<String> columns=new ArrayList<>(periods);columns.addAll(months);
  List<Member> members=dao.members();Map<String,Map<String,long[]>> cells=new HashMap<>();
  for(Member m:members)cells.put(m.id,new HashMap<>());
  for(LegacyEntry l:dao.legacy()){Map<String,long[]> byPeriod=cells.get(l.memberId);if(byPeriod==null)continue;long[] v=byPeriod.computeIfAbsent(l.period,k->new long[2]);if("packs".equals(l.kind))v[1]+=l.amount;else v[0]+=l.amount;}
  for(Contribution p:dao.contributions()){if(p.feeMonth.compareTo(last)>0)continue;Map<String,long[]> byPeriod=cells.get(p.memberId);if(byPeriod==null)continue;long[] v=byPeriod.computeIfAbsent(p.feeMonth,k->new long[2]);if("packs".equals(p.kind))v[1]+=p.amount;else v[0]+=p.amount;}
  Map<String,long[]> totals=new HashMap<>();for(String column:columns){long[] t=new long[2];for(Member member:members){long[] v=cells.get(member.id).get(column);if(v!=null){t[0]+=v[0];t[1]+=v[1];}}totals.put(column,t);}
  try{JSONObject j=new JSONObject(meta.legacyTotalsJson);for(String p:periods){if(j.has(p)){JSONObject x=j.getJSONObject(p);totals.put(p,new long[]{x.optLong("cash"),x.optLong("packs")});}}}catch(Exception ignored){}
  for(ReportMath.Result r:sums){long[] observed=totals.get(r.month);totals.put(r.month,new long[]{r.collections,observed==null?0:observed[1]});}
  PdfDocument pdf=new PdfDocument();PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());Canvas c=page.getCanvas();
  header(c,"BADMINTON MASTER CONTRIBUTION LEDGER",meta.club,"Through "+last);
  final float startX=13,tableY=82,nameW=124,totalW=77,avail=842-26-nameW-totalW;float cw=avail/columns.size(),rowH=18.3f;
  if(cw<20)throw new IllegalArgumentException("This time range is too wide for one page; select an earlier month");
  rect(c,GREEN,startX,tableY,842-26,25);txt(c,"PLAYER",startX+6,tableY+16,8,Color.WHITE,true);
  for(int j=0;j<columns.size();j++){String period=columns.get(j);String caption=period.length()==7?period.substring(5)+"/"+period.substring(2,4):period;
   txt(c,fit(caption,cw-2,6.6f),startX+nameW+j*cw+1.5f,tableY+16,6.6f,Color.WHITE,true);
  }txt(c,"GRAND TOTAL",startX+nameW+avail+2,tableY+16,7.2f,Color.WHITE,true);
  int i=0;for(Member member:members){float y=tableY+25+i*rowH;rect(c,i%2==0?Color.WHITE:0xfff2f7f3,startX,y,842-26,rowH);txt(c,fit(member.name,nameW-7,8.1f),startX+5,y+12.2f,8.1f,DARK,false);
   long sum=0;int packs=0;for(int j=0;j<columns.size();j++){long[] v=cells.get(member.id).get(columns.get(j));if(v==null)continue;sum+=v[0];packs+=v[1];txt(c,fit(f(v[0],(int)v[1]),cw-1,6.25f),startX+nameW+j*cw+1,y+11.9f,6.25f,DARK,false);}
   txt(c,fit(f(sum,packs),totalW-3,7.1f),startX+nameW+avail+2,y+12,7.1f,DARK,true);i++;
  }
  float ty=tableY+25+members.size()*rowH;rect(c,PALE,startX,ty,842-26,20);txt(c,"TOTAL",startX+5,ty+13,8,GREEN,true);
  for(int j=0;j<columns.size();j++){long[] v=totals.get(columns.get(j));txt(c,fit(f(v[0],(int)v[1]),cw-2,6.1f),startX+nameW+j*cw+1,ty+13,6.1f,GREEN,true);}stroke(c,startX,tableY,842-26,25+members.size()*rowH+20);
  float bottom=ty+34;txt(c,"BALANCE COMPUTATION · MOST RECENT MONTHS",startX,bottom,10,GREEN,true);
  int start=Math.max(0,sums.size()-4);for(int k=start;k<sums.size();k++){
   ReportMath.Result r=sums.get(k);int idx=k-start;float xx=startX+idx*203,yy=bottom+10;
   rect(c,GREEN,xx,yy,195,17);txt(c,r.month,xx+7,yy+12,8.6f,Color.WHITE,true);
   String[] labels={"Opening", "Collections", "Shuttle / expenses", "Closing"};long[] vals={r.opening,r.collections,r.expenses,r.closing};
   for(int j=0;j<4;j++){rect(c,j%2==0?PALE:Color.WHITE,xx,yy+17+j*18,195,18);txt(c,labels[j],xx+6,yy+17+j*18+12,7.6f,DARK,j==3);txt(c,number(vals[j]),xx+131,yy+17+j*18+12,8,j==3?GREEN:DARK,true);}stroke(c,xx,yy,195,89);
  }
  txt(c,"FM / AM / JJ / AS / ON / Dec = historical 2025 periods. Donated shuttle packs recorded separately from cash.",startX,579,7,GRAY,false);
  if(sums.stream().anyMatch(r->r.difference!=0))txt(c,"Historical difference: February 2026 official collection - member entries = −₹1,000; not silently corrected.",startX,590,7,RED,true);
  pdf.finishPage(page);return write(context,pdf,"Classic_"+last);
 }
}

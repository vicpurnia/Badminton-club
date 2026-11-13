package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** Premium v23 single-page A4 landscape classic ledger.
 * Preserves historical monthly controls and reports donations as non-cash.
 */
final class ClassicLedgerV23 {
 private static final int GREEN=0xff08694b, DARK=0xff173c31, GRAY=0xff64736b,
   PALE=0xffeaf5ed, BORDER=0xffc3d6ca, RED=0xffaa3240, GOLD=0xfff0d79b;
 private static final Paint P=new Paint(Paint.ANTI_ALIAS_FLAG);
 private static void box(Canvas c,int color,float x,float y,float width,float height){
  P.setColor(color);P.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+width,y+height,P);
 }
 private static void outline(Canvas c,float x,float y,float w,float h){
  P.setColor(BORDER);P.setStrokeWidth(.55f);P.setStyle(Paint.Style.STROKE);
  c.drawRect(x,y,x+w,y+h,P);P.setStyle(Paint.Style.FILL);
 }
 private static void txt(Canvas c,String value,float x,float baseline,float size,int color,boolean bold){
  P.setColor(color);P.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
  P.setTextSize(size);c.drawText(value,x,baseline,P);
 }
 private static String shorten(String value,float width,float size,boolean bold){
  P.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
  P.setTextSize(size);if(P.measureText(value)<=width)return value;
  String candidate=value;while(candidate.length()>1&&P.measureText(candidate+"…")>width)candidate=candidate.substring(0,candidate.length()-1);
  return candidate+"…";
 }
 private static void centered(Canvas c,String value,float x,float baseline,float width,float size,int color,boolean bold){
  P.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));P.setTextSize(size);
  while(size>5.65f&&P.measureText(value)>width-2){size-=.25f;P.setTextSize(size);}
  String safe=shorten(value,width-1,size,bold);
  txt(c,safe,x+Math.max(0,(width-P.measureText(safe))/2),baseline,size,color,bold);
 }
 private static String number(long n){
  return (n<0?"−":"")+String.format(new Locale("en","IN"),"%,d",Math.abs(n));
 }
 private static void valueCell(Canvas c,long cash,long packs,float x,float y,float w,float h,boolean strong){
  int ink=strong?GREEN:DARK;float fs=strong?7.0f:7.3f;
  if(packs>0){
   if(cash!=0){
    centered(c,number(cash),x,y+9.0f,w,fs,ink,strong);
    centered(c,"+"+packs+"pk",x,y+17.2f,w,6.55f,ink,true);
   }else centered(c,"+"+packs+"pk",x,y+h*.64f,w,6.95f,ink,strong);
  }else if(cash!=0)centered(c,number(cash),x,y+h*.68f,w,fs,ink,strong);
  else if(strong)centered(c,"—",x,y+h*.68f,w,6.6f,ink,false);
 }
 private static String expenseCompact(Expense expense){
  String amount=number(expense.amount);
  String date=expense.date==null?"":expense.date;
  if(date.length()>=10&&date.charAt(4)=='-')return date.substring(8,10)+": "+amount;
  return amount;
 }
 private static String[] wrapExpenseLines(List<Expense> entries){
  ArrayList<String> pieces=new ArrayList<>();
  int count=0;
  for(Expense e:entries){
   if(!"Shuttle pack".equalsIgnoreCase(e.category))continue;
   count+=(int)Math.max(1,e.quantity);
   pieces.add(expenseCompact(e)+(e.quantity>1?" ("+e.quantity+"pk)":""));
  }
  if(pieces.isEmpty())return new String[]{"No shuttle purchases recorded",""};
  String label="Shuttles: "+count+" pack"+(count==1?"":"s");
  String joined=String.join(" · ",pieces);
  return new String[]{label,joined};
 }
 static File master(Context context,LedgerDao dao,String selectedMonth) throws Exception {
  LedgerMeta meta=dao.meta();if(meta==null)throw new IllegalStateException("Please import your ledger first");
  String cutoff=MigrationCodec.ym(selectedMonth);
  if(cutoff.compareTo(meta.openingMonth)<0)throw new IllegalArgumentException("Month is before the ledger opening period");
  List<String> historical=new ArrayList<>();
  JSONArray p=new JSONArray(meta.periodsJson);
  for(int i=0;i<p.length();i++)historical.add(p.getString(i));
  List<String> months=new ArrayList<>();
  for(String m=meta.openingMonth;m.compareTo(cutoff)<=0;m=ReportMath.nextMonth(m)){
   months.add(m);if(months.size()>240)throw new IllegalArgumentException("Reporting period is too long");
  }
  List<String> columns=new ArrayList<>(historical);columns.addAll(months);
  if(columns.size()>23)throw new IllegalArgumentException("This report contains too many months for a readable one-page PDF. Choose a shorter period.");
  List<Member> players=dao.members();
  List<Contribution> contributions=dao.contributions();
  List<Expense> expenses=dao.expenses();
  List<ReportMath.Result> summaries=ReportMath.monthly(dao,cutoff);
  Map<String,Map<String,long[]>> grid=new HashMap<>();
  for(Member m:players)grid.put(m.id,new HashMap<>());
  for(LegacyEntry l:dao.legacy()){
   Map<String,long[]> row=grid.get(l.memberId);if(row==null)continue;
   long[] pair=row.computeIfAbsent(l.period,k->new long[2]);
   pair["packs".equals(l.kind)?1:0]+=l.amount;
  }
  for(Contribution entry:contributions){
   if(entry.feeMonth.compareTo(cutoff)>0)continue;
   Map<String,long[]> row=grid.get(entry.memberId);if(row==null)continue;
   long[] pair=row.computeIfAbsent(entry.feeMonth,k->new long[2]);
   pair["packs".equals(entry.kind)?1:0]+=entry.amount;
  }
  Map<String,long[]> totals=new HashMap<>();
  for(String col:columns){
   long[] pair=new long[2];
   for(Member player:players){
    long[] v=grid.get(player.id).get(col);if(v!=null){pair[0]+=v[0];pair[1]+=v[1];}
   }
   totals.put(col,pair);
  }
  JSONObject official2025=new JSONObject(meta.legacyTotalsJson);
  for(String col:historical)if(official2025.has(col)){
   JSONObject o=official2025.getJSONObject(col);
   totals.put(col,new long[]{o.optLong("cash"),o.optLong("packs")});
  }
  for(ReportMath.Result r:summaries){
   long[] observed=totals.get(r.month);
   totals.put(r.month,new long[]{r.collections,observed==null?0:observed[1]});
  }
  boolean missingHistoricPack=false;
  for(Member player:players)if("p11".equals(player.id)&&cutoff.compareTo("2026-02")>=0){
   long[] v=grid.get(player.id).get("2026-02");
   missingHistoricPack=v==null||v[1]==0;
  }
  PdfDocument pdf=new PdfDocument();
  PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());
  Canvas c=page.getCanvas();
  box(c,GREEN,0,0,842,66);
  txt(c,meta.club.toUpperCase(Locale.ROOT),17,20,10,GOLD,true);
  txt(c,"MASTER CONTRIBUTION LEDGER",17,47,19,Color.WHITE,true);
  txt(c,"THROUGH "+cutoff,693,44,9.6f,Color.WHITE,true);
  float x=13,y=77,tableW=816,nameW=127,totalW=87,hHeader=24;
  float cw=(tableW-nameW-totalW)/columns.size();
  float rowH=19.15f,totalH=22;
  box(c,GREEN,x,y,tableW,hHeader);
  txt(c,"PLAYER",x+7,y+15.8f,8.3f,Color.WHITE,true);
  for(int j=0;j<columns.size();j++){
   String col=columns.get(j);
   String label=col.length()==7?col.substring(5)+"/"+col.substring(2,4):col;
   centered(c,label,x+nameW+j*cw,y+15.9f,cw,7.2f,Color.WHITE,true);
  }
  centered(c,"GRAND TOTAL",x+tableW-totalW,y+15.9f,totalW,7.65f,Color.WHITE,true);
  for(int i=0;i<players.size();i++){
   Member player=players.get(i);float top=y+hHeader+i*rowH;
   box(c,i%2==0?Color.WHITE:0xfff3f8f5,x,top,tableW,rowH);
   txt(c,shorten(player.name,nameW-11,8.65f,false),x+6,top+13.0f,8.65f,DARK,false);
   long cash=0,packs=0;
   for(int j=0;j<columns.size();j++){
    long[] v=grid.get(player.id).get(columns.get(j));if(v==null)continue;
    cash+=v[0];packs+=v[1];
    valueCell(c,v[0],v[1],x+nameW+j*cw,top,cw,rowH,false);
   }
   valueCell(c,cash,packs,x+tableW-totalW,top,totalW,rowH,true);
  }
  float totalY=y+hHeader+players.size()*rowH;
  box(c,PALE,x,totalY,tableW,totalH);
  txt(c,"TOTAL",x+6,totalY+14.8f,9,GREEN,true);
  for(int j=0;j<columns.size();j++){
   long[] v=totals.get(columns.get(j));
   valueCell(c,v[0],v[1],x+nameW+j*cw,totalY,cw,totalH,true);
  }
  outline(c,x,y,tableW,hHeader+players.size()*rowH+totalH);
  float section=totalY+totalH+18;
  txt(c,"BALANCE COMPUTATION · FOUR MOST RECENT MONTHS",x,section,10.3f,GREEN,true);
  int start=Math.max(0,summaries.size()-4);
  Map<String,List<Expense>> expensesByMonth=new HashMap<>();
  for(Expense e:expenses)if(e.month.compareTo(cutoff)<=0)expensesByMonth.computeIfAbsent(e.month,k->new ArrayList<>()).add(e);
  for(int idx=start;idx<summaries.size();idx++){
   ReportMath.Result r=summaries.get(idx);
   int index=idx-start;float left=x+index*204,top=section+10;
   box(c,GREEN,left,top,196,18);
   txt(c,r.month,left+8,top+12.5f,8.8f,Color.WHITE,true);
   String[] labels={"Opening","Collections","Shuttle / expenses","Closing"};
   long[] amounts={r.opening,r.collections,r.expenses,r.closing};
   for(int j=0;j<labels.length;j++){
    float yy=top+18+j*16.5f;
    box(c,j%2==0?PALE:Color.WHITE,left,yy,196,16.5f);
    txt(c,labels[j],left+7,yy+11.5f,8,DARK,j==3);
    txt(c,number(amounts[j]),left+134,yy+11.5f,8.2f,j==3?GREEN:DARK,true);
   }
   String[] lines=wrapExpenseLines(expensesByMonth.getOrDefault(r.month,Collections.emptyList()));
   float lower=top+18+66;
   box(c,0xfff6faf7,left,lower,196,29);
   txt(c,shorten(lines[0],186,7.6f,true),left+7,lower+11.4f,7.6f,GREEN,true);
   if(!lines[1].isEmpty()){
    float size=6.8f;
    P.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));P.setTextSize(size);
    while(P.measureText(lines[1])>187&&size>5.9f){size-=.25f;P.setTextSize(size);}
    txt(c,shorten(lines[1],186,size,false),left+7,lower+23.7f,size,DARK,false);
   }
   outline(c,left,top,196,18+66+29);
  }
  txt(c,"FM / AM / JJ / AS / ON / Dec: 2025 historical periods. Donated shuttle packs do not affect cash balance.",x,576,7.15f,GRAY,false);
  String warning="";
  if(missingHistoricPack)warning="February 2026 donated-pack entry is absent from the imported database; use the in-app historical correction.";
  else if(summaries.stream().anyMatch(r->r.difference!=0))warning="February 2026 cash reconciliation difference of −₹1,000 remains unresolved; official totals unchanged.";
  if(!warning.isEmpty())txt(c,shorten(warning,806,7.0f,false),x,589,7.0f,RED,true);
  pdf.finishPage(page);
  File dir=new File(context.getCacheDir(),"pdfs");if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("Unable to create PDF folder");
  File result=new File(dir,"Classic_v23_"+cutoff+"_"+System.currentTimeMillis()+".pdf");
  try(FileOutputStream out=new FileOutputStream(result)){pdf.writeTo(out);}finally{pdf.close();}
  return result;
 }
}
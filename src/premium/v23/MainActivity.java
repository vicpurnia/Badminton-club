package com.vinayak.badminton.nativepreview;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.view.*;
import android.widget.*;
import androidx.room.Room;
import androidx.core.content.FileProvider;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Native Android + Room proof-of-conversion. Separate app ID protects v17 local records.
 *  First import is read-only for the original JSON. No app-to-app private storage access.
 */
public final class MainActivity extends Activity {
 private static final int GREEN=0xFF12644E,DARK=0xFF102E29,LIGHT=0xFFF6F8F4,GRAY=0xFF72847C,GOLD=0xFFE6C88A,LINE=0xFFE6EBE5,PALE=0xFFEAF3ED;
 private static final int PICK_LEDGER=2001, SAVE_VERIFIED=2002, SAVE_PDF=2003;
 private LedgerDatabase database;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private LinearLayout root,content,nav,header;private String tab="Home",reportMonth="2026-11",memberFilter="All";private String searchText="";
 private MigrationCodec.Preview pendingImport;
 private File pendingExport,pendingReportPdf;
 private final int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 private final long receiptStamp(){return System.currentTimeMillis();}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(GREEN);
  database=Room.databaseBuilder(getApplicationContext(),LedgerDatabase.class,"badminton_room_v18.db").build();
  reportMonth=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());layout();selectTab("Home");}
 @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
 @FunctionalInterface interface CheckedJob {void run() throws Exception;}
 private void background(CheckedJob job){io.execute(()->{try{job.run();}catch(Exception e){runOnUiThread(()->error(e));}});}
 private void error(Exception e){new AlertDialog.Builder(this).setTitle("Action not completed").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
 private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private GradientDrawable border(int color,int stroke,int radius){GradientDrawable d=bg(color,radius);d.setStroke(dp(1),stroke);return d;}
 private GradientDrawable gradient(int c1,int c2,int r){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{c1,c2});d.setCornerRadius(dp(r));return d;}
 private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return t;}
 private LinearLayout col(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
 private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 private void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
 private LinearLayout.LayoutParams fill(){return new LinearLayout.LayoutParams(-1,-2);}
 private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
 private void panel(LinearLayout parent,View child){LinearLayout wrap=col();wrap.setBackground(border(Color.WHITE,LINE,18));pad(wrap,17,16);wrap.setElevation(dp(1));wrap.addView(child);parent.addView(wrap,fill());gap(parent,12);}
 private Button button(String s,Runnable task){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(50));b.setBackground(gradient(GREEN,0xFF0A513F,14));b.setOnClickListener(v->task.run());return b;}
 private TextView inlineButton(String s,int color,Runnable run){TextView b=text(s,12,color,true);pad(b,12,9);b.setGravity(Gravity.CENTER);b.setBackground(border(PALE,LINE,10));b.setOnClickListener(v->run.run());return b;}
 private TextView pill(String label,int fg,int bgc){TextView t=text(label,11,fg,true);t.setGravity(Gravity.CENTER);pad(t,12,8);t.setBackground(bg(bgc,100));return t;}
 private void pressable(View v,Runnable action){v.setOnClickListener(x->action.run());}
 private LinearLayout blockHeader(String head,String sub){LinearLayout c=col();c.addView(text(head,18,DARK,true));gap(c,4);c.addView(text(sub,12,GRAY,false));return c;}
 private void sectionHeading(String head,String subtitle){LinearLayout r=row();r.addView(blockHeader(head,subtitle),new LinearLayout.LayoutParams(0,-2,1));content.addView(r);gap(content,13);}
 private LinearLayout infoRow(String lead,String caption,String value,int accent){LinearLayout r=row();TextView badge=text(lead,15,accent,true);badge.setGravity(Gravity.CENTER);badge.setBackground(bg(PALE,14));r.addView(badge,new LinearLayout.LayoutParams(dp(45),dp(45)));LinearLayout l=col();pad(l,12,0);l.addView(text(caption,13,DARK,true));r.addView(l,new LinearLayout.LayoutParams(0,-2,1));r.addView(text(value,12,accent,true));return r;}
 private String dateLabel(String ym){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT);f.setLenient(false);return new java.text.SimpleDateFormat("MMMM yyyy",Locale.ENGLISH).format(f.parse(ym));}catch(Exception e){return ym;}}
 private String rupee(long x){return (x<0?"−":"")+"₹"+String.format(new Locale("en","IN"),"%,d",Math.abs(x));}
 private String sign(long x){return x<0?"Deficit":"Available";}
 private LinearLayout metric(String label,String value,String symbol){LinearLayout c=col();c.setBackground(border(Color.WHITE,LINE,16));pad(c,13,15);c.addView(text(symbol,18,GREEN,true));gap(c,13);c.addView(text(label,11,GRAY,false));gap(c,5);c.addView(text(value,20,DARK,true));return c;}
 private void shiftMonth(int n){
  try{
   String[] parts=reportMonth.split("-");
   java.util.Calendar calendar=java.util.Calendar.getInstance();
   calendar.clear();
   calendar.set(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])-1,1);
   calendar.add(java.util.Calendar.MONTH,n);
   String value=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(calendar.getTime());
   if(value.compareTo("2026-01")<0){Toast.makeText(this,"The balance ledger begins in 2026",Toast.LENGTH_SHORT).show();return;}
   reportMonth=value;selectTab(tab);
  }catch(Exception ex){error(new IllegalArgumentException("Could not switch month"));}
 }
 private void chooseMonth(String current,java.util.function.Consumer<String> selected){
  try{
   String valid=MigrationCodec.ym(current);String[] parts=valid.split("-");
   int year=Integer.parseInt(parts[0]),month=Integer.parseInt(parts[1]);
   DatePickerDialog chooser=new DatePickerDialog(this,(picker,y,m,day)->{
    String v=String.format(Locale.ROOT,"%04d-%02d",y,m+1);
    if(v.compareTo("2026-01")<0){Toast.makeText(this,"The balance ledger begins in 2026",Toast.LENGTH_SHORT).show();return;}
    selected.accept(v);
   },year,month-1,1);
   chooser.setTitle("Choose contribution month");chooser.show();
  }catch(Exception ex){error(new IllegalArgumentException("Choose a valid month"));}
 }
 private void monthPicker(){
  LinearLayout holder=row();holder.setBackground(border(Color.WHITE,LINE,16));pad(holder,11,7);
  TextView prev=inlineButton("‹",GREEN,()->shiftMonth(-1));prev.setTextSize(27);
  holder.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(46)));
  LinearLayout middle=col();middle.setGravity(Gravity.CENTER);
  middle.addView(text("VIEWING MONTH",10,GRAY,true));
  TextView month=text(dateLabel(reportMonth)+"  ▾",16,DARK,true);month.setGravity(Gravity.CENTER);middle.addView(month);
  pressable(middle,()->chooseMonth(reportMonth,v->{reportMonth=v;selectTab(tab);}));
  holder.addView(middle,new LinearLayout.LayoutParams(0,-2,1));
  TextView next=inlineButton("›",GREEN,()->shiftMonth(1));next.setTextSize(27);
  holder.addView(next,new LinearLayout.LayoutParams(dp(48),dp(46)));
  content.addView(holder,fill());gap(content,16);
 }
 private void layout(){root=col();root.setBackgroundColor(LIGHT);setContentView(root);if(Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(0,bars.top,0,bars.bottom);return insets;});}getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(0xFFFFFFFF);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  header=col();header.setBackground(gradient(DARK,0xFF154E3E,0));pad(header,21,15);LinearLayout top=row();LinearLayout brand=col();brand.addView(text("BADMINTON CLUB",10,GOLD,true));gap(brand,5);brand.addView(text("Smart Manager",23,Color.WHITE,true));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView offline=pill("●  OFFLINE",0xFFBDEBCB,0xFF235A49);top.addView(offline);header.addView(top);root.addView(header,fill());
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);content=col();pad(content,17,20);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  nav=row();nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(10));pad(nav,7,7);root.addView(nav,new LinearLayout.LayoutParams(-1,dp(76)));}
 private void drawNav(){nav.removeAllViews();for(String item:new String[]{"Home","Collect","Shuttles","Members","Reports"}){
   boolean active=tab.equals(item);boolean center=item.equals("Collect");LinearLayout tile=col();tile.setGravity(Gravity.CENTER);tile.setBackground(bg(center?GREEN:active?PALE:Color.WHITE,14));
   String icon=item.equals("Home")?"⌂":item.equals("Collect")?"＋":item.equals("Shuttles")?"◈":item.equals("Members")?"♙":"▤";
   TextView glyph=text(icon,center?25:22,center?Color.WHITE:active?GREEN:GRAY,true);glyph.setGravity(Gravity.CENTER);tile.addView(glyph);
   TextView caption=text(item,10,center?Color.WHITE:active?GREEN:GRAY,active||center);caption.setGravity(Gravity.CENTER);tile.addView(caption);
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);params.setMargins(dp(2),0,dp(2),0);nav.addView(tile,params);pressable(tile,()->selectTab(item));
  }}
 private void selectTab(String name){tab=name;drawNav();switch(name){case "Home":showHome();break;case "Collect":showPayments();break;case "Shuttles":showExpenses();break;case "Members":showMembers();break;default:showReports();}}
 private void title(String heading,String sub){content.removeAllViews();content.addView(text(heading,26,DARK,true));gap(content,5);content.addView(text(sub,13,GRAY,false));gap(content,19);}
 private void notice(String header,String details){LinearLayout p=col();p.addView(text(header,15,DARK,true));gap(p,7);p.addView(text(details,12,GRAY,false));panel(content,p);}
 private void showHome(){title("Overview","Your club at a glance");monthPicker();background(()->{
   LedgerDao dao=database.dao();LedgerMeta meta=dao.meta();if(meta==null){runOnUiThread(()->{
    LinearLayout welcome=col();welcome.setBackground(gradient(GREEN,DARK,22));pad(welcome,20,22);welcome.addView(text("WELCOME TO THE CLUB",11,GOLD,true));gap(welcome,12);welcome.addView(text("Your accounts, beautifully organised.",27,Color.WHITE,true));gap(welcome,10);welcome.addView(text("Import the verified backup from Native v19 to bring over your players and financial records safely.",13,0xFFD6F0E1,false));gap(welcome,20);welcome.addView(button("Import verified ledger",this::pickMigration));content.addView(welcome,fill());gap(content,16);
    notice("Already using Native v19?","Open v19 → Home → Export verified ledger backup. Select that JSON file here. Keep the older app installed until you have checked every balance.");});return;}
   String display=reportMonth;List<ReportMath.Result> summaries=ReportMath.monthly(dao,display);if(summaries.isEmpty()){runOnUiThread(()->notice("No closing data","This month is before the opening-balance period."));return;}ReportMath.Result latest=summaries.get(summaries.size()-1);
   List<Member> members=dao.members();List<Contribution> payments=dao.contributions();int count=0;for(Contribution c:payments)if(display.equals(c.feeMonth)&&!"packs".equals(c.kind))count++;
   final int paymentCount=count;runOnUiThread(()->{
    LinearLayout card=col();card.setBackground(gradient(0xFF165E4A,0xFF07352D,24));pad(card,22,24);
    LinearLayout header=row();header.addView(text("CLUB BALANCE",11,0xFFD7F3E0,true),new LinearLayout.LayoutParams(0,-2,1));header.addView(pill(dateLabel(display),Color.WHITE,0xFF2C735B));card.addView(header);gap(card,14);
    card.addView(text(rupee(latest.closing),39,Color.WHITE,true));gap(card,7);card.addView(text((latest.closing>=0?"● Credit available":"● Balance in deficit")+"  ·  Offline ledger",12,0xFFD2E7DA,false));gap(card,22);
    View divider=new View(this);divider.setBackgroundColor(0xFF376C5B);card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));gap(card,16);
    LinearLayout totals=row();LinearLayout col1=col();col1.addView(text("COLLECTED",10,0xFFB6D4C3,true));gap(col1,7);col1.addView(text(rupee(latest.collections),20,Color.WHITE,true));LinearLayout col2=col();col2.addView(text("EXPENSES",10,0xFFB6D4C3,true));gap(col2,7);col2.addView(text(rupee(latest.expenses),20,Color.WHITE,true));totals.addView(col1,new LinearLayout.LayoutParams(0,-2,1));totals.addView(col2,new LinearLayout.LayoutParams(0,-2,1));card.addView(totals);content.addView(card,fill());gap(content,14);
    LinearLayout stats=row();LinearLayout a=metric("Players",String.valueOf(members.size()),"♙");LinearLayout b=metric("Receipts",String.valueOf(paymentCount),"▤");LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(0,0,dp(6),0);stats.addView(a,half);LinearLayout.LayoutParams other=new LinearLayout.LayoutParams(0,-2,1);other.setMargins(dp(6),0,0,0);stats.addView(b,other);content.addView(stats);gap(content,23);
    sectionHeading("Quick actions","Everything you use every day");LinearLayout actions=row();actions.addView(action("＋","Collect fee",()->openPayment(null)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(action("◈","Shuttle pack",this::openExpense),new LinearLayout.LayoutParams(0,-2,1));actions.addView(action("▤","Master PDF",()->sharePdf("Classic_Master_Ledger.pdf",()->ClassicLedgerV23.master(this,dao,display))),new LinearLayout.LayoutParams(0,-2,1));content.addView(actions);gap(content,23);
    sectionHeading("Recent activity","Latest recorded contributions");Map<String,String> names=new HashMap<>();for(Member m:members)names.put(m.id,m.name);ArrayList<Contribution> recent=new ArrayList<>(payments);Collections.reverse(recent);int n=0;
    for(Contribution p:recent){if(!"cash".equals(p.kind))continue;LinearLayout item=row();TextView av=avatar(names.getOrDefault(p.memberId,"?"));item.addView(av);LinearLayout texts=col();pad(texts,12,0);texts.addView(text(names.getOrDefault(p.memberId,"Member"),14,DARK,true));gap(texts,4);texts.addView(text(dateLabel(p.feeMonth)+(p.approximate?" · Imported":" · "+p.method),11,GRAY,false));item.addView(texts,new LinearLayout.LayoutParams(0,-2,1));item.addView(text("+"+rupee(p.amount),14,GREEN,true));panel(content,item);if(++n==4)break;}
    if(n==0)notice("No payments recorded","Tap Collect fee to add the first contribution.");
    if(summaries.stream().anyMatch(x->x.difference!=0))notice("Historical reconciliation","The February 2026 collection difference is retained. Historical totals have not been altered.");
   });
 });}
 private LinearLayout action(String symbol,String caption,Runnable runnable){LinearLayout box=col();box.setGravity(Gravity.CENTER);box.setBackground(border(Color.WHITE,LINE,17));pad(box,7,17);TextView t=text(symbol,25,GREEN,true);t.setGravity(Gravity.CENTER);box.addView(t);gap(box,10);TextView t2=text(caption,11,DARK,true);t2.setGravity(Gravity.CENTER);box.addView(t2);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),0,dp(4),0);pressable(box,runnable);return box;}
 private TextView avatar(String name){String initials="?";if(name!=null&&!name.isEmpty()){String[] bits=name.replace("Dr.","").trim().split("\\s+");initials=bits.length>1?bits[0].substring(0,1)+bits[1].substring(0,1):bits[0].substring(0,1);}TextView a=text(initials.toUpperCase(Locale.ROOT),13,GREEN,true);a.setGravity(Gravity.CENTER);a.setBackground(bg(PALE,16));a.setWidth(dp(43));a.setHeight(dp(43));return a;}
 private void startVerifiedExport(){
  background(()->{
   String json=MigrationCodec.exportVerified(database);
   File temp=new File(getCacheDir(),"Badminton_verified_export_pending.json");
   try(OutputStream os=new FileOutputStream(temp)){os.write(json.getBytes(StandardCharsets.UTF_8));}
   pendingExport=temp;
   runOnUiThread(()->{
    Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/json");
    String stamp=new java.text.SimpleDateFormat("yyyyMMdd_HHmm",Locale.ROOT).format(new Date());
    save.putExtra(Intent.EXTRA_TITLE,"Badminton_Verified_"+stamp+".json");
    startActivityForResult(save,SAVE_VERIFIED);
   });
  });
 }
 private void pickMigration(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,PICK_LEDGER);}
 @Override protected void onActivityResult(int req,int resultCode,Intent data){super.onActivityResult(req,resultCode,data);
  if(req==SAVE_PDF){
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
  }
  if(req==SAVE_VERIFIED){
   File file=pendingExport;pendingExport=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){if(file!=null)file.delete();return;}
   if(file==null){error(new IOException("Export session expired. Please choose Export again."));return;}
   Uri destination=data.getData();
   background(()->{try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(destination,"w")){
    if(out==null)throw new IOException("Unable to open the selected save location");
    byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();
    runOnUiThread(()->Toast.makeText(this,"Verified ledger saved",Toast.LENGTH_LONG).show());
   }finally{file.delete();}});
   return;
  }
  if(req!=PICK_LEDGER||resultCode!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;
  background(()->{
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Could not open file");byte[] buf=new byte[4096];int n;while((n=in.read(buf))>=0){out.write(buf,0,n);if(out.size()>6_000_000)throw new IOException("Migration file exceeds 6 MB limit");}}
   MigrationCodec.Preview result=MigrationCodec.inspect(out.toString(StandardCharsets.UTF_8.name()));
   if(database.dao().metaCount()>0)throw new IllegalStateException("The native ledger already contains data. Replacement is blocked for your safety.");pendingImport=result;
   runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Verified ledger ready")
    .setMessage("SHA-256 verified\n\n"+result.players+" members · "+result.payments+" payments · "+result.expenses+" expenses\nOpening "+rupee(result.openingBalance)+"\nClosing "+rupee(result.closingBalance)+" through "+result.lastMonth+"\n\nHistorical differences: "+result.differences.size()+"\n\nImport into this EMPTY app? Your original app will remain unchanged.")
    .setNegativeButton("Cancel",(d,w)->pendingImport=null)
    .setPositiveButton("Import securely",(d,w)->importPending()).show());
  });
 }
 private void importPending(){MigrationCodec.Preview preview=pendingImport;pendingImport=null;if(preview==null)return;
  background(()->{MigrationCodec.importChecked(database,preview);runOnUiThread(()->{Toast.makeText(this,"Ledger imported successfully",Toast.LENGTH_LONG).show();showHome();});});}
 private void showMembers(){title("Members","Your club, one player at a time");monthPicker();
  LinearLayout searchPanel=col();EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHint("Search all players");search.setCompoundDrawablePadding(dp(10));search.setBackground(border(Color.WHITE,LINE,14));pad(search,15,13);search.setText(searchText);searchPanel.addView(search);content.addView(searchPanel);gap(content,13);
  LinearLayout filters=row();for(String option:new String[]{"All","Received","Partial","No entry"}){
   TextView c=pill(option,option.equals(memberFilter)?Color.WHITE:GREEN,option.equals(memberFilter)?GREEN:PALE);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),0,dp(2),0);filters.addView(c,lp);pressable(c,()->{memberFilter=option;searchText=search.getText().toString();showMembers();});}
  content.addView(filters);gap(content,14);
  LinearLayout results=col();content.addView(results);gap(content,10);content.addView(button("＋  Add new player",this::addMember),fill());
  Runnable render=()->background(()->{
   List<Member> members=database.dao().members();List<Contribution> payments=database.dao().contributions();List<LegacyEntry> legacy=database.dao().legacy();
   runOnUiThread(()->{results.removeAllViews();int shown=0;for(Member member:members){if(!member.name.toLowerCase(Locale.ROOT).contains(search.getText().toString().trim().toLowerCase(Locale.ROOT)))continue;
    long current=0,lifetime=0,donated=0;for(Contribution c:payments)if(member.id.equals(c.memberId)){if("packs".equals(c.kind))donated+=c.amount;else {lifetime+=c.amount;if(reportMonth.equals(c.feeMonth))current+=c.amount;}}
    for(LegacyEntry e:legacy)if(member.id.equals(e.memberId)&&"cash".equals(e.kind))lifetime+=e.amount;
    String status=current>=2000?"Received":current>0?"Partial":"No entry";if(!memberFilter.equals("All")&&!status.equals(memberFilter))continue;
    final long cTotal=current,lTotal=lifetime,dPacks=donated;LinearLayout card=row();card.addView(avatar(member.name));LinearLayout labels=col();pad(labels,11,0);labels.addView(text(member.name,14,DARK,true));gap(labels,5);labels.addView(text(reportMonth+"  ·  "+(current>0?rupee(current):"No contribution recorded"),11,GRAY,false));card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
    TextView bubble=pill(status,status.equals("Received")?GREEN:status.equals("Partial")?0xFF9B6B18:GRAY,status.equals("Received")?PALE:status.equals("Partial")?0xFFFFF2D7:0xFFF1F3F1);
    card.addView(bubble);pressable(card,()->memberWallet(member,cTotal,lTotal,dPacks));panel(results,card);shown++;}
    if(shown==0){LinearLayout n=blockHeader("No matching members","Try another name or filter.");panel(results,n);}
   });
  });
  render.run();search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence v,int st,int c,int a){}public void onTextChanged(CharSequence v,int st,int b,int c){searchText=v.toString();render.run();}public void afterTextChanged(android.text.Editable e){}});
 }
 private void memberWallet(Member m,long monthly,long lifetime,long donated){background(()->{ArrayList<Contribution> entries=new ArrayList<>();for(Contribution c:database.dao().contributions())if(m.id.equals(c.memberId))entries.add(c);LedgerMeta meta=database.dao().meta();Collections.reverse(entries);
  runOnUiThread(()->{LinearLayout body=col();pad(body,20,17);LinearLayout head=row();head.addView(avatar(m.name));LinearLayout n=col();pad(n,12,0);n.addView(text(m.name,19,DARK,true));gap(n,5);n.addView(text("Individual account",12,GRAY,false));head.addView(n);body.addView(head);gap(body,20);
   LinearLayout hero=col();hero.setBackground(gradient(GREEN,DARK,18));pad(hero,17,20);hero.addView(text("TOTAL RECORDED CONTRIBUTIONS",11,0xFFD4E9DF,true));gap(hero,10);hero.addView(text(rupee(lifetime),30,Color.WHITE,true));gap(hero,8);hero.addView(text("This month "+rupee(monthly)+"    ·    Donated "+donated+" pack(s)",12,Color.WHITE,false));body.addView(hero);gap(body,18);
   body.addView(text("Contribution history",16,DARK,true));gap(body,12);int shown=0;for(Contribution c:entries){LinearLayout r=row();LinearLayout l=col();l.addView(text(dateLabel(c.feeMonth),13,DARK,true));gap(l,3);l.addView(text(c.approximate?"Imported record":"Received "+c.receivedDate,11,GRAY,false));r.addView(l,new LinearLayout.LayoutParams(0,-2,1));r.addView(text("packs".equals(c.kind)?c.amount+" pack(s)":rupee(c.amount),13,GREEN,true));body.addView(r);gap(body,12);if(++shown>=12)break;}
   if(entries.isEmpty())body.addView(text("No entries yet.",12,GRAY,false));gap(body,10);
   body.addView(button("＋ Collect from this player",()->{memberDialog.dismiss();openPayment(m.id);}),fill());
   ScrollView sv=new ScrollView(this);sv.addView(body);memberDialog=new Dialog(this);memberDialog.setContentView(sv);Window w=memberDialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-2);w.setGravity(Gravity.BOTTOM);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=.45f;w.setAttributes(p);}memberDialog.show();if(w!=null)w.setLayout(-1,Math.min(dp(680),(int)(getResources().getDisplayMetrics().heightPixels*.88f)));
  });
 });}
 private Dialog memberDialog;
 private void addMember(){EditText e=new EditText(this);e.setHint("Full player name");new AlertDialog.Builder(this).setTitle("New player").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->background(()->{String n=e.getText().toString().trim();if(n.isEmpty())throw new IllegalArgumentException("Name required");Member m=new Member();m.id="native-"+UUID.randomUUID();m.name=n;database.dao().insertMember(m);runOnUiThread(this::showMembers);})).show();}
 private void showPayments(){title("Collect fees","Simple, quick and ready to share");monthPicker();
  LinearLayout headline=col();headline.setBackground(gradient(GREEN,DARK,22));pad(headline,20,22);headline.addView(text("ONE-TAP COLLECTION",11,GOLD,true));gap(headline,10);headline.addView(text("Record a contribution",22,Color.WHITE,true));gap(headline,8);headline.addView(text("Choose a player, confirm the amount, then share a professional PDF receipt.",12,0xFFE1F1E9,false));gap(headline,15);headline.addView(button("＋  Collect ₹2,000",()->openPayment(null)));content.addView(headline);gap(content,22);
  sectionHeading("Monthly receipts","Recorded for "+dateLabel(reportMonth));final String viewedMonth=reportMonth;
  background(()->{List<Contribution> rows=database.dao().contributions();Map<String,Member> names=new HashMap<>();for(Member m:database.dao().members())names.put(m.id,m);runOnUiThread(()->{int n=0;for(int i=rows.size()-1;i>=0;i--){Contribution c=rows.get(i);if("packs".equals(c.kind)||!viewedMonth.equals(c.feeMonth))continue;Member m=names.get(c.memberId);if(m==null)continue;
   LinearLayout card=row();card.addView(avatar(m.name));LinearLayout info=col();pad(info,11,0);info.addView(text(m.name,14,DARK,true));gap(info,5);info.addView(text(dateLabel(c.feeMonth)+(c.approximate?" · Imported":" · "+c.method),11,GRAY,false));card.addView(info,new LinearLayout.LayoutParams(0,-2,1));LinearLayout right=col();right.setGravity(Gravity.END);right.addView(text(rupee(c.amount),15,GREEN,true));gap(right,6);right.addView(inlineButton("Receipt ▤",GREEN,()->sharePdf("Receipt_"+c.id+".pdf",()->PdfReports.receipt(this,m,c,database.dao().meta()))));card.addView(right);panel(content,card);if(++n==16)break;}
   if(n==0)notice("No receipts this month","Choose another month above or collect a contribution.");});});
 }
 private void openPayment(String chosen){background(()->{List<Member> members=database.dao().members();runOnUiThread(()->{
   if(members.isEmpty()){error(new IllegalStateException("Import your verified ledger first"));return;}
   Dialog dialog=new Dialog(this);LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,21,23);
   LinearLayout cap=row();LinearLayout headline=col();headline.addView(text("New contribution",22,DARK,true));gap(headline,5);headline.addView(text("Securely record and share",12,GRAY,false));cap.addView(headline,new LinearLayout.LayoutParams(0,-2,1));TextView close=inlineButton("✕",GRAY,dialog::dismiss);cap.addView(close);form.addView(cap);gap(form,25);
   form.addView(text("PLAYER",11,GRAY,true));gap(form,9);AutoCompleteTextView who=new AutoCompleteTextView(this);who.setSingleLine(true);who.setTextSize(15);who.setThreshold(1);String[] options=new String[members.size()];for(int i=0;i<members.size();i++)options[i]=members.get(i).name;
   who.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,options));who.setHint("Search player name");who.setBackground(border(LIGHT,LINE,12));pad(who,14,12);if(chosen!=null)for(Member m:members)if(m.id.equals(chosen))who.setText(m.name,false);form.addView(who);gap(form,16);
   form.addView(text("AMOUNT RECEIVED",11,GRAY,true));gap(form,9);EditText amount=new EditText(this);amount.setSingleLine(true);amount.setText("2000");amount.setTextSize(23);amount.setTypeface(Typeface.DEFAULT,Typeface.BOLD);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);amount.setBackground(border(LIGHT,LINE,12));pad(amount,14,11);form.addView(amount);gap(form,9);
   LinearLayout presets=row();for(int a:new int[]{500,1000,2000,4000}){TextView c=pill("₹"+a,GREEN,PALE);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(dp(3),0,dp(3),0);presets.addView(c,cp);pressable(c,()->amount.setText(String.valueOf(a)));}form.addView(presets);gap(form,16);
   form.addView(text("CONTRIBUTION MONTH",11,GRAY,true));gap(form,9);
   EditText month=new EditText(this);month.setSingleLine(true);month.setText(reportMonth);
   month.setFocusable(false);month.setClickable(true);
   month.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.ic_menu_my_calendar,0);
   month.setCompoundDrawablePadding(dp(10));month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);
   pressable(month,()->chooseMonth(month.getText().toString(),month::setText));form.addView(month);gap(form,16);
   form.addView(text("PAYMENT METHOD",11,GRAY,true));gap(form,8);Spinner via=new Spinner(this);via.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Cash","UPI","Bank transfer","Other"}));form.addView(via);gap(form,20);
   Button save=button("Save & share receipt  →",()->{
    String name=who.getText().toString().trim();Member selected=null;for(Member m:members)if(m.name.equalsIgnoreCase(name)){selected=m;break;}
    if(selected==null){who.setError("Choose a player from the list");return;}final Member m=selected;
    String fee=month.getText().toString().trim();long value;try{MigrationCodec.ym(fee);value=Long.parseLong(amount.getText().toString());if(value<=0)throw new NumberFormatException();}catch(Exception e){error(new IllegalArgumentException("Enter a valid YYYY-MM month and positive amount"));return;}
    String method=via.getSelectedItem().toString();background(()->{int existing=database.dao().existingCash(m.id,fee);runOnUiThread(()->{
     if(existing>0)new AlertDialog.Builder(this).setTitle("Contribution already recorded").setMessage(m.name+" has "+existing+" cash entry/entries for "+fee+". Record an additional payment?")
       .setNegativeButton("Keep editing",null).setPositiveButton("Add payment",(d,w)->{dialog.dismiss();savePayment(m,fee,value,method);}).show();
     else {dialog.dismiss();savePayment(m,fee,value,method);}
    });});
   });form.addView(save);gap(form,9);TextView disclaimer=text("Existing payments are checked before saving.",11,GRAY,false);disclaimer.setGravity(Gravity.CENTER);form.addView(disclaimer);
   ScrollView scroll=new ScrollView(this);scroll.addView(form);dialog.setContentView(scroll);dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,Math.min(dp(740),(int)(getResources().getDisplayMetrics().heightPixels*.91f)));}who.clearFocus();
  });});
 }
 private void savePayment(Member m,String fee,long amount,String method){
  background(()->{
   boolean historicalControl=false;
   for(MonthlyControl control:database.dao().controls())if(control.month.equals(fee)){historicalControl=true;break;}
   if(historicalControl)runOnUiThread(()->new AlertDialog.Builder(this)
     .setTitle("Historical control month")
     .setMessage("The historical collection base for "+fee+" is preserved. Your new payment will be added as a separate adjustment, increasing the balance without erasing any old discrepancy. Continue?")
     .setNegativeButton("Cancel",null)
     .setPositiveButton("Add with reconciliation flag",(d,w)->persistPayment(m,fee,amount,method)).show());
   else persistPayment(m,fee,amount,method);
  });
 }
 private void persistPayment(Member m,String fee,long amount,String method){background(()->{
  Contribution c=new Contribution();c.id="pay-"+UUID.randomUUID();c.memberId=m.id;c.feeMonth=fee;c.receivedDate=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());c.amount=amount;c.kind="cash";c.method=method;
  database.runInTransaction(()->{database.dao().insertPayment(c);AuditEvent a=new AuditEvent();a.id="audit-"+UUID.randomUUID();a.createdAt=new Date().toString();a.action="Contribution added";a.details=c.id+" / "+rupee(amount);database.dao().audit(a);});
  runOnUiThread(()->{Toast.makeText(this,"Payment saved",Toast.LENGTH_SHORT).show();showPayments();sharePdf("Receipt_"+c.id+".pdf",()->PdfReports.receipt(this,m,c,database.dao().meta()));});
 });}
 private void showExpenses(){title("Shuttle desk","Purchases, spending and stock records");monthPicker();
  LinearLayout hero=col();hero.setBackground(gradient(GREEN,DARK,22));pad(hero,20,22);hero.addView(text("SHUTTLE EXPENDITURE",11,0xFFD6EBDD,true));gap(hero,12);hero.addView(text("Keep every pack accounted for",21,Color.WHITE,true));gap(hero,15);hero.addView(button("＋  Record purchase",this::openExpense));content.addView(hero);gap(content,20);
  background(()->{List<Expense> entries=database.dao().expenses();long amount=0,packs=0;for(Expense e:entries)if(reportMonth.equals(e.month)){amount+=e.amount;if(e.category.equalsIgnoreCase("Shuttle pack"))packs+=e.quantity;}long total=amount,qty=packs;
   runOnUiThread(()->{LinearLayout stats=row();stats.addView(metric("Spent this month",rupee(total),"◈"),new LinearLayout.LayoutParams(0,-2,1));stats.addView(metric("Packs purchased",String.valueOf(qty),"▥"),new LinearLayout.LayoutParams(0,-2,1));content.addView(stats);gap(content,24);sectionHeading("Recent purchases","Records from the club ledger");int n=0;
    for(int i=entries.size()-1;i>=0;i--){Expense e=entries.get(i);LinearLayout card=row();TextView badge=avatar("Shuttle");card.addView(badge);LinearLayout labels=col();pad(labels,12,0);labels.addView(text(e.category,14,DARK,true));gap(labels,5);labels.addView(text(dateLabel(e.month)+(e.date.isEmpty()?" · Exact date not recorded":" · "+e.date),11,GRAY,false));card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));LinearLayout v=col();v.setGravity(Gravity.END);v.addView(text("−"+rupee(e.amount),14,DARK,true));gap(v,5);v.addView(text(e.quantity+" pack(s)",11,GRAY,false));card.addView(v);panel(content,card);if(++n>=15)break;}
   });
  });}
 private void openExpense(){Dialog dialog=new Dialog(this);LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,22,24);form.addView(text("New shuttle purchase",22,DARK,true));gap(form,5);form.addView(text("Update expenses and running balance",12,GRAY,false));gap(form,24);
  form.addView(text("MONTH (YYYY-MM)",11,GRAY,true));gap(form,8);EditText month=new EditText(this);month.setText(reportMonth);month.setSingleLine(true);month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);form.addView(month);gap(form,17);
  form.addView(text("NUMBER OF PACKS",11,GRAY,true));gap(form,8);EditText count=new EditText(this);count.setInputType(2);count.setText("1");count.setBackground(border(LIGHT,LINE,12));pad(count,14,12);form.addView(count);gap(form,17);
  form.addView(text("PRICE PER PACK",11,GRAY,true));gap(form,8);EditText price=new EditText(this);price.setInputType(2);price.setText("4000");price.setBackground(border(LIGHT,LINE,12));pad(price,14,12);form.addView(price);gap(form,8);
  LinearLayout presets=row();for(int q:new int[]{3200,3500,4000}){TextView chip=pill("₹"+q,GREEN,PALE);presets.addView(chip,new LinearLayout.LayoutParams(0,-2,1));pressable(chip,()->price.setText(String.valueOf(q)));}form.addView(presets);gap(form,20);
  form.addView(button("Save purchase  →",()->{
   try{String mm=MigrationCodec.ym(month.getText().toString().trim());long qty=Long.parseLong(count.getText().toString()),unit=Long.parseLong(price.getText().toString());if(qty<=0||unit<=0)throw new NumberFormatException();long total=Math.multiplyExact(qty,unit);dialog.dismiss();
    background(()->{Expense e=new Expense();e.id="expense-"+UUID.randomUUID();e.month=mm;e.quantity=qty;e.amount=total;database.dao().insertExpense(e);runOnUiThread(()->{Toast.makeText(this,"Purchase recorded",Toast.LENGTH_LONG).show();showExpenses();});});
   }catch(Exception ex){error(new IllegalArgumentException("Enter a valid month, quantity and price"));}
  }));ScrollView sv=new ScrollView(this);sv.addView(form);dialog.setContentView(sv);dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}
 }
 private void restoreHistoricalPack(){
  background(()->{
   Member chosen=null;for(Member m:database.dao().members())if("p11".equals(m.id)){chosen=m;break;}
   if(chosen==null)throw new IllegalStateException("The historical member for this entry was not found. No changes made.");
   for(Contribution c:database.dao().contributions()){
    if("p11".equals(c.memberId)&&"2026-02".equals(c.feeMonth)&&"packs".equals(c.kind)){
     runOnUiThread(()->Toast.makeText(this,"February donated pack is already recorded",Toast.LENGTH_LONG).show());return;
    }
   }
   final String person=chosen.name;
   runOnUiThread(()->new AlertDialog.Builder(this)
    .setTitle("Restore documented shuttle donation")
    .setMessage(person+" donated one shuttle pack in February 2026 according to the archived ledger. The imported database omitted it.\n\nRestore ONE noncash donated pack? Cash collections and closing balances will not change. The correction is recorded in the audit history.")
    .setNegativeButton("Cancel",null)
    .setPositiveButton("Restore one pack",(d,w)->applyHistoricalPack())
    .show());
  });
 }
 private void applyHistoricalPack(){
  background(()->{
   final boolean[] restored={false};
   database.runInTransaction(()->{
    LedgerDao dao=database.dao();
    if(dao.meta()==null)throw new IllegalStateException("No imported ledger found");
    boolean memberFound=false;for(Member m:dao.members())if("p11".equals(m.id)){memberFound=true;break;}
    if(!memberFound)throw new IllegalStateException("Historical member is missing");
    for(Contribution c:dao.contributions())
     if("p11".equals(c.memberId)&&"2026-02".equals(c.feeMonth)&&"packs".equals(c.kind))return;
    Contribution donation=new Contribution();
    donation.id="historic-2026-02-p11-donation-1";
    donation.memberId="p11";donation.feeMonth="2026-02";
    donation.receivedDate="2026-02-01";donation.approximate=true;
    donation.kind="packs";donation.amount=1;donation.historical=true;
    donation.method="Historical donated pack";donation.note="Exact date unknown; correction verified against archived February 2026 PDF.";
    dao.insertPayment(donation);
    AuditEvent event=new AuditEvent();event.id="audit-"+java.util.UUID.randomUUID();
    event.action="historic_noncash_restoration";
    event.createdAt=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.ROOT).format(new Date());
    event.details="Restored one missing Feb 2026 donated shuttle pack for member p11. No cash adjustment.";
    dao.audit(event);restored[0]=true;
   });
   runOnUiThread(()->{
    Toast.makeText(this,restored[0]?"Historical shuttle donation restored. Cash balance unchanged.":"Donation already recorded; no changes made.",Toast.LENGTH_LONG).show();
    if(tab.equals("Reports"))showReports();
   });
  });
 }
 private void showReports(){title("Reports","Your club finances, beautifully presented");
   LinearLayout selector=col();selector.addView(text("REPORTING MONTH",11,GRAY,true));gap(selector,9);
   EditText month=new EditText(this);month.setSingleLine(true);month.setText(reportMonth);
   month.setFocusable(false);month.setClickable(true);
   month.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.ic_menu_my_calendar,0);
   month.setCompoundDrawablePadding(dp(10));month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);
   pressable(month,()->chooseMonth(month.getText().toString(),v->{month.setText(v);reportMonth=v;}));
   selector.addView(month);panel(content,selector);gap(content,13);
   sectionHeading("Choose a report","Preview and share through Android");
   reportTile("▤","Classic Master Ledger","Original green A4 landscape · all members and month-wise history",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;sharePdf("Badminton_Classic_"+m+".pdf",()->ClassicLedgerV23.master(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("≡","Monthly Cashbook","Collection, expenses and running balance for the selected month",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;sharePdf("Badminton_Monthly_"+m+".pdf",()->PdfReports.monthly(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("↓","Save Classic PDF to phone","Choose a folder and save the green A4 landscape master ledger",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;savePdf("Badminton_Classic_"+m+".pdf",()->ClassicLedgerV23.master(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("↓","Save Monthly PDF to phone","Choose where to store the monthly cashbook PDF",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;savePdf("Badminton_Monthly_"+m+".pdf",()->PdfReports.monthly(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("◫","Complete history","Classic master ledger through the latest recorded month",()->background(()->{String last=ReportMath.lastMonth(database.dao());runOnUiThread(()->sharePdf("Badminton_Complete_"+last+".pdf",()->ClassicLedgerV23.master(this,database.dao(),last)));}));
   reportTile("✦","Restore February donated pack","One-time confirmed noncash correction from the archived ledger",this::restoreHistoricalPack);
   reportTile("⇩","Verified account export","Save your complete ledger to a JSON file for migration or safekeeping",this::startVerifiedExport);
   gap(content,9);notice("Private and offline","PDFs are generated on your phone. Share them using Android's file-sharing options. Your records are not uploaded automatically.");
 }
 private void reportTile(String symbol,String heading,String detail,Runnable action){LinearLayout card=row();TextView mark=text(symbol,23,GREEN,true);mark.setGravity(Gravity.CENTER);mark.setBackground(bg(PALE,15));card.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(52)));LinearLayout body=col();pad(body,13,0);body.addView(text(heading,15,DARK,true));gap(body,6);body.addView(text(detail,11,GRAY,false));card.addView(body,new LinearLayout.LayoutParams(0,-2,1));card.addView(text("›",26,GREEN,true));pressable(card,action);panel(content,card);}
 
 interface RunnablePdf{File make() throws Exception;}
 private void savePdf(String name,RunnablePdf renderer){background(()->{
  if(database.dao().meta()==null)throw new IllegalStateException("Import your verified ledger first");
  File pdf=renderer.make();
  if(pdf==null||!pdf.isFile()||pdf.length()<100)throw new IOException("PDF could not be generated");
  pendingReportPdf=pdf;
  runOnUiThread(()->{
    Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    save.addCategory(Intent.CATEGORY_OPENABLE);save.setType("application/pdf");
    save.putExtra(Intent.EXTRA_TITLE,name);
    try{startActivityForResult(save,SAVE_PDF);}
    catch(Exception e){pendingReportPdf=null;error(new IOException("No available app to save PDF"));}
  });
 });}
 private void sharePdf(String filename,RunnablePdf renderer){background(()->{
  if(database.dao().meta()==null)throw new IllegalStateException("Import your verified ledger first");
  File file=renderer.make();
  if(file==null||!file.isFile()||file.length()<100)throw new IOException("PDF could not be generated");
  runOnUiThread(()->{
   try{
    Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
    Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/pdf");send.putExtra(Intent.EXTRA_STREAM,uri);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(send,"Share PDF"));
   }catch(Exception ex){error(new IOException("Could not open Android sharing. Use Save PDF to phone instead."));}
  });
 });}
}

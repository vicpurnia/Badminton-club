package com.vinayak.badminton.nativepreview;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PageRange;
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
 private static final int PICK_LEDGER=2001, SAVE_VERIFIED=2002, SAVE_PDF=2003, PICK_BACKUP_TREE=2004, VERIFY_BACKUP=2005, TEST_RESTORE=2006;
 private LedgerDatabase database;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private LinearLayout root,content,nav,header;private String tab="Home",reportMonth="2026-11",memberFilter="All";private String searchText="";
 private MigrationCodec.Preview pendingImport;
 private File pendingExport,pendingReportPdf;
 private final int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 private final long receiptStamp(){return System.currentTimeMillis();}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(DARK);getWindow().setNavigationBarColor(GREEN);
  database=Room.databaseBuilder(getApplicationContext(),LedgerDatabase.class,"badminton_room_v18.db").build();
  reportMonth=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());layout();selectTab("Home");
  background(()->BackupGuardian.maybeDaily(this,database));}
 @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
 @FunctionalInterface interface CheckedJob {void run() throws Exception;}
 private void background(CheckedJob job){io.execute(()->{try{job.run();}catch(Exception e){runOnUiThread(()->error(e));}});}
 private void requireOpenMonth(String month){
  LedgerMeta meta=database.dao().meta();
  if(meta==null)throw new IllegalStateException("Import the verified ledger before changes.");
  MonthlyCloseV258.requireOpen(meta.inventoryStockJson,MigrationCodec.ym(month));
 }
 private void requireEditableStockMonth(String month){
  LedgerMeta meta=database.dao().meta();
  if(meta==null)throw new IllegalStateException("Verified club ledger must be imported first");
  PremiumV26StockGuard.requireEditableStockMonth(meta.inventoryStockJson,month);
 }
 private void requireImportedLedger(){
  if(database.dao().meta()==null)
   throw new IllegalStateException("Import your verified club ledger before recording new entries. No data was changed.");
 }
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
 private Button button(String s,Runnable task){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(50));b.setBackground(gradient(GREEN,0xFF0A513F,14));bindGuardedClick(b,task);return b;}
 private void bindGuardedClick(View view,Runnable task){
  final long[] last={0L};
  view.setOnClickListener(v->{
   long now=android.os.SystemClock.elapsedRealtime();
   if(now-last[0]<700L)return;
   last[0]=now;
   task.run();
  });
 }
 private TextView inlineButton(String s,int color,Runnable run){TextView b=text(s,12,color,true);pad(b,12,9);b.setGravity(Gravity.CENTER);b.setBackground(border(PALE,LINE,10));bindGuardedClick(b,run);return b;}
 private TextView pill(String label,int fg,int bgc){TextView t=text(label,11,fg,true);t.setGravity(Gravity.CENTER);pad(t,12,8);t.setBackground(bg(bgc,100));return t;}
 private void pressable(View v,Runnable action){bindGuardedClick(v,action);}
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
  header=col();header.setBackground(gradient(DARK,0xFF154E3E,0));pad(header,21,15);LinearLayout top=row();LinearLayout brand=col();brand.addView(text("BADMINTON CLUB",10,GOLD,true));gap(brand,5);brand.addView(text("Smart Manager",23,Color.WHITE,true));gap(brand,3);brand.addView(text("v26.9 DEVELOPMENT CANDIDATE",10,0xFFD1EBDC,true));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView offline=pill("●  OFFLINE",0xFFBDEBCB,0xFF235A49);top.addView(offline);header.addView(top);root.addView(header,fill());
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
    LinearLayout welcome=col();welcome.setBackground(gradient(GREEN,DARK,22));pad(welcome,20,22);welcome.addView(text("WELCOME TO THE CLUB",11,GOLD,true));gap(welcome,12);welcome.addView(text("Your accounts, beautifully organised.",27,Color.WHITE,true));gap(welcome,10);welcome.addView(text("Welcome to Badminton Premium Stable. If this is your first stable installation, import a verified JSON backup from your previous Badminton app. Later stable updates signed with the same permanent key can preserve these accounts automatically.",13,0xFFD6F0E1,false));gap(welcome,20);welcome.addView(button("Import verified ledger",this::pickMigration));content.addView(welcome,fill());gap(content,16);
    notice("Keep your existing Badminton app","Before importing, save a verified JSON export from your previous app. Keep that app and its records until all balances are confirmed here. Existing stable installs should update without reimporting.");});return;}
   String display=reportMonth;List<ReportMath.Result> summaries=ReportMath.monthly(dao,display);if(summaries.isEmpty()){runOnUiThread(()->notice("No closing data","This month is before the opening-balance period."));return;}ReportMath.Result latest=summaries.get(summaries.size()-1);
   List<Member> members=dao.members();List<Contribution> payments=dao.contributions();
   Map<String,CashCorrectionsV256.State> effective=CashCorrectionsV256.states(meta.inventoryStockJson,payments);
   int count=0;for(Contribution c:payments)if(display.equals(c.feeMonth)&&!"packs".equals(c.kind)&&
    (!effective.containsKey(c.id)||effective.get(c.id).effective>0))count++;
   final int paymentCount=count;runOnUiThread(()->{
    LinearLayout card=col();card.setBackground(gradient(0xFF165E4A,0xFF07352D,24));pad(card,22,24);
    LinearLayout header=row();header.addView(text("CLUB BALANCE",11,0xFFD7F3E0,true),new LinearLayout.LayoutParams(0,-2,1));header.addView(pill(dateLabel(display),Color.WHITE,0xFF2C735B));card.addView(header);gap(card,14);
    card.addView(text(rupee(latest.closing),39,Color.WHITE,true));gap(card,7);card.addView(text((latest.closing>=0?"● Credit available":"● Balance in deficit")+"  ·  Offline ledger",12,0xFFD2E7DA,false));gap(card,22);
    View divider=new View(this);divider.setBackgroundColor(0xFF376C5B);card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));gap(card,16);
    LinearLayout totals=row();LinearLayout col1=col();col1.addView(text("COLLECTED",10,0xFFB6D4C3,true));gap(col1,7);col1.addView(text(rupee(latest.collections),20,Color.WHITE,true));LinearLayout col2=col();col2.addView(text("EXPENSES",10,0xFFB6D4C3,true));gap(col2,7);col2.addView(text(rupee(latest.expenses),20,Color.WHITE,true));totals.addView(col1,new LinearLayout.LayoutParams(0,-2,1));totals.addView(col2,new LinearLayout.LayoutParams(0,-2,1));card.addView(totals);content.addView(card,fill());gap(content,14);
    LinearLayout stats=row();LinearLayout a=metric("Players",String.valueOf(members.size()),"♙");LinearLayout b=metric("Receipts",String.valueOf(paymentCount),"▤");LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(0,0,dp(6),0);stats.addView(a,half);LinearLayout.LayoutParams other=new LinearLayout.LayoutParams(0,-2,1);other.setMargins(dp(6),0,0,0);stats.addView(b,other);content.addView(stats);gap(content,15);
    sectionHeading("Backup health","Verified copies help protect your records");
    notice("Your backup status",BackupGuardian.status(this));
    notice("Recovery readiness",BackupGuardian.recoveryReadiness(this));
    LinearLayout safetyActions=row();
    safetyActions.addView(action("▣","Back up now",this::backupNow),new LinearLayout.LayoutParams(0,-2,1));
    safetyActions.addView(action("✓","Verify backup",this::verifyPrivateBackup),new LinearLayout.LayoutParams(0,-2,1));
    content.addView(safetyActions);gap(content,20);
    sectionHeading("Quick actions","Everything you use every day");LinearLayout actions=row();actions.addView(action("＋","Collect fee",()->openPayment(null)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(action("◈","Shuttle pack",this::openExpense),new LinearLayout.LayoutParams(0,-2,1));actions.addView(action("▤","Master PDF",()->sharePdf("Classic_Master_Ledger.pdf",()->ClassicLedgerV231.master(this,dao,display))),new LinearLayout.LayoutParams(0,-2,1));content.addView(actions);gap(content,23);
    sectionHeading("Recent activity","Latest recorded contributions");Map<String,String> names=new HashMap<>();for(Member m:members)names.put(m.id,m.name);ArrayList<Contribution> recent=new ArrayList<>(payments);Collections.reverse(recent);int n=0;
    for(Contribution p:recent){if(!"cash".equals(p.kind))continue;LinearLayout item=row();TextView av=avatar(names.getOrDefault(p.memberId,"?"));item.addView(av);LinearLayout texts=col();pad(texts,12,0);texts.addView(text(names.getOrDefault(p.memberId,"Member"),14,DARK,true));gap(texts,4);texts.addView(text(dateLabel(p.feeMonth)+(p.approximate?" · Imported":" · "+p.method),11,GRAY,false));item.addView(texts,new LinearLayout.LayoutParams(0,-2,1));long recognized=effective.containsKey(p.id)?effective.get(p.id).effective:p.amount;
    item.addView(text(recognized==0?"VOID":("+"+rupee(recognized)),14,recognized==0?GRAY:GREEN,true));panel(content,item);if(++n==4)break;}
    if(n==0)notice("No payments recorded","Tap Collect fee to add the first contribution.");
    if(summaries.stream().anyMatch(x->x.difference!=0))notice("Historical reconciliation","The February 2026 collection difference is retained. Historical totals have not been altered.");
   });
 });}
 private LinearLayout action(String symbol,String caption,Runnable runnable){LinearLayout box=col();box.setGravity(Gravity.CENTER);box.setBackground(border(Color.WHITE,LINE,17));pad(box,7,17);TextView t=text(symbol,25,GREEN,true);t.setGravity(Gravity.CENTER);box.addView(t);gap(box,10);TextView t2=text(caption,11,DARK,true);t2.setGravity(Gravity.CENTER);box.addView(t2);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(4),0,dp(4),0);pressable(box,runnable);return box;}
 private TextView avatar(String name){String initials="?";if(name!=null&&!name.isEmpty()){String[] bits=name.replace("Dr.","").trim().split("\\s+");initials=bits.length>1?bits[0].substring(0,1)+bits[1].substring(0,1):bits[0].substring(0,1);}TextView a=text(initials.toUpperCase(Locale.ROOT),13,GREEN,true);a.setGravity(Gravity.CENTER);a.setBackground(bg(PALE,16));a.setWidth(dp(43));a.setHeight(dp(43));return a;}
 private void startVerifiedExport(){
  background(()->{
   String json=MigrationCodec.exportVerified(database);
   MigrationCodec.inspect(json); // Validate the verified export before offering it to Android Documents.
   byte[] payload=json.getBytes(StandardCharsets.UTF_8);
   if(payload.length==0||payload.length>BackupGuardian.MAX_BACKUP_BYTES)throw new IOException("Verified export exceeds 12 MB; avoid an incomplete backup.");
   File temp=new File(getCacheDir(),"Badminton_verified_export_pending.json");
   try(OutputStream os=new FileOutputStream(temp)){os.write(payload);}
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
 private void pickBackupFolder(){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
  try{startActivityForResult(intent,PICK_BACKUP_TREE);}
  catch(Exception ex){error(new IOException("Android cannot open the folder chooser: "+ex.getMessage()));}
 }
 private void verifyPrivateBackup(){
  background(()->{
   String result=BackupGuardian.verifyLatestPrivate(this);
   runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Backup integrity check")
    .setMessage(result)
    .setPositiveButton("OK",null).show());
  });
 }

 private static String checkpointSummary(String raw){
  InventoryCheckpoint.Config config=InventoryCheckpoint.read(raw);
  if(config==null)return "not configured";
  return config.month+" / "+config.openingShuttles+" shuttles / "+config.perPack+" per pack";
 }

 private void verifyExternalBackup(){
  background(()->{
   String summary=BackupGuardian.verifyLatestExternal(this);
   runOnUiThread(()->new AlertDialog.Builder(this)
    .setTitle("External backup integrity verified")
    .setMessage(summary)
    .setPositiveButton("OK",null).show());
  });
 }
 private void testRestoreWithoutChanges(){
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("application/json");
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,TEST_RESTORE);}
  catch(Exception ex){error(new IOException("Android cannot open the verified JSON picker."));}
 }

 private void chooseBackupToVerify(){
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);
  pick.setType("application/json");
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,VERIFY_BACKUP);}
  catch(Exception ex){error(new IOException("Unable to open the JSON picker: "+ex.getMessage()));}
 }
 private void warnOnBackupFailure(){
  String warning=BackupGuardian.lastError(this);
  if(warning!=null&&!warning.trim().isEmpty()){
   final String display=warning.length()>160?warning.substring(0,160):warning;
   runOnUiThread(()->Toast.makeText(this,"Ledger saved. Backup needs attention: "+display,Toast.LENGTH_LONG).show());
  }
 }
 private void backupNow(){
  background(()->{
   if(database.dao().meta()==null)throw new IllegalStateException("Import a ledger before creating backups");
   BackupGuardian.save(this,database,"manual",true);
   String status=BackupGuardian.status(this);
   runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Verified backup status")
    .setMessage(status+"\n\n"+BackupGuardian.recoveryReadiness(this)+"\n\nKeep a trusted external backup before uninstalling the app.")
    .setPositiveButton("OK",null).show());
  });
 }
 private void disableBackupFolder(){
  new AlertDialog.Builder(this).setTitle("Disconnect backup folder")
   .setMessage("Stop automatic external backup copies? Existing backup files in the folder will not be deleted. Private app backups continue.")
   .setNegativeButton("Cancel",null)
   .setPositiveButton("Disconnect",(d,w)->{BackupGuardian.clearExternalFolder(this);showReports();})
   .show();
 }
 private void pickMigration(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,PICK_LEDGER);}
 @Override protected void onActivityResult(int req,int resultCode,Intent data){super.onActivityResult(req,resultCode,data);

  if(req==SAVE_ENCRYPTED_V26){
   File file=pendingEncryptedExport;pendingEncryptedExport=null;
   if(resultCode!=RESULT_OK||data==null||data.getData()==null){
    if(file!=null)file.delete();return;
   }
   if(file==null||!file.isFile()){error(new IOException("Encrypted backup export expired"));return;}
   Uri destination=data.getData();
   background(()->{
    try{
     byte[] original;
     try(InputStream src=new FileInputStream(file)){
      ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[16384];int n;
      while((n=src.read(buf))!=-1){if(n>0)b.write(buf,0,n);}original=b.toByteArray();
     }
     try(OutputStream output=getContentResolver().openOutputStream(destination,"w")){
      if(output==null)throw new IOException("Could not create encrypted backup");
      output.write(original);output.flush();
     }
     byte[] readback=readDocumentLimited(destination,PremiumBackupCrypto.MAX_ENVELOPE);
     byte[] a=java.security.MessageDigest.getInstance("SHA-256").digest(original);
     byte[] b=java.security.MessageDigest.getInstance("SHA-256").digest(readback);
     if(!java.security.MessageDigest.isEqual(a,b))
      throw new IOException("Encrypted backup read-back differs; do not rely on this file");
     runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Encrypted backup saved & byte-verified")
      .setMessage("The selected file was reopened and checked. Keep the password in a separate secure location. Rehearse recovery on the new phone before discarding any original.")
      .setPositiveButton("OK",null).show());
    }finally{file.delete();}
   });return;
  }
  if(req==READ_ENCRYPTED_V26){
   if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)
    readEncryptedBackup(data.getData(),encryptedImportAfterCheck);
   return;
  }
  if(req==TEST_RESTORE){
   if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
   final Uri selected=data.getData();
   background(()->{
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    try(InputStream in=getContentResolver().openInputStream(selected)){
     if(in==null)throw new IOException("Selected restore-test JSON cannot be opened.");
     byte[] block=new byte[16384];int n;
     while((n=in.read(block))!=-1){
      if(n==0)continue;
      if(bytes.size()+n>BackupGuardian.MAX_BACKUP_BYTES)
       throw new IOException("Restore-test JSON exceeds the 12 MB verified export limit.");
      bytes.write(block,0,n);
     }
    }
    String report=BackupRecoveryV259.dryRun(this,bytes.toString(StandardCharsets.UTF_8.name()));
    runOnUiThread(()->new AlertDialog.Builder(this)
      .setTitle("Restore rehearsal — no account changes")
      .setMessage(report)
      .setPositiveButton("OK",null).show());
   });
   return;
  }
  if(req==VERIFY_BACKUP){
   if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
   final Uri selected=data.getData();
   background(()->{
    ByteArrayOutputStream jsonBytes=new ByteArrayOutputStream();
    try(InputStream in=getContentResolver().openInputStream(selected)){
     if(in==null)throw new IOException("Backup file cannot be opened.");
     byte[] block=new byte[16384];int n;
     while((n=in.read(block))!=-1){
      if(n<=0)continue;
      if(jsonBytes.size()+n>BackupGuardian.MAX_BACKUP_BYTES)throw new IOException("Backup exceeds the 12 MB verified-export limit.");
      jsonBytes.write(block,0,n);
     }
    }
    MigrationCodec.Preview result=MigrationCodec.inspect(jsonBytes.toString(StandardCharsets.UTF_8.name()));
    MigrationCodec.Preview current=null;
    if(database.dao().meta()!=null){
     current=MigrationCodec.inspect(MigrationCodec.exportVerified(database));
    }
    String comparison="\n\nNo current ledger in this app; the selected backup can be imported only into an empty database.";
    if(current!=null){
     comparison="\n\nCURRENT APP vs SELECTED BACKUP"
      +"\nMembers: "+current.players+" vs "+result.players
      +"\nContributions: "+current.payments+" vs "+result.payments
      +"\nExpenses: "+current.expenses+" vs "+result.expenses
      +"\nShuttle usage entries: "+current.stockUsage+" vs "+result.stockUsage
      +"\nConfirmed stock checkpoint: "+
      checkpointSummary(database.dao().meta().inventoryStockJson)+" vs "+
      checkpointSummary(result.ledger.optJSONObject("inventoryStock")==null?"":result.ledger.optJSONObject("inventoryStock").toString())
      +"\nLast month: "+current.lastMonth+" vs "+result.lastMonth
      +"\nClosing balance: "+rupee(current.closingBalance)+" vs "+rupee(result.closingBalance)
      +"\nThese are summary checks; matching totals alone do not prove every entry is identical.";
    }
    String message="SHA-256 envelope and ledger structure validated.\n\n"
      +result.players+" players · "+result.payments+" contributions · "+result.expenses+" expenses"
      +" · "+result.stockUsage+" shuttle usage entries"
      +"\nOpening "+rupee(result.openingBalance)
      +"\nClosing "+rupee(result.closingBalance)+" through "+result.lastMonth
      +"\nUnresolved historical differences: "+result.differences.size()
      +comparison
      +"\n\nRead-only verification: no records were imported or changed.";
    runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Verified backup — read only")
     .setMessage(message).setPositiveButton("OK",null).show());
   });
   return;
  }
  if(req==PICK_BACKUP_TREE){
   if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
   try{
    BackupGuardian.setExternalFolder(this,data.getData(),data.getFlags());
    background(()->{
     BackupGuardian.save(this,database,"external-folder-selected",true);
     runOnUiThread(()->{
      new AlertDialog.Builder(this).setTitle("Backup folder configured")
       .setMessage(BackupGuardian.status(this)+"\n\nFuture recorded changes will be backed up here automatically.")
       .setPositiveButton("OK",null).show();
      if(tab.equals("Reports"))showReports();
     });
    });
   }catch(Exception ex){error(new IOException("Cannot connect the selected folder: "+ex.getMessage()));}
   return;
  }
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
   background(()->{
    try{
     try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(destination,"w")){
      if(out==null)throw new IOException("Unable to open the selected save location");
      byte[] b=new byte[16384];int n;
      while((n=in.read(b))!=-1)if(n>0)out.write(b,0,n);
      out.flush();
     }
     // Some document providers accept a write but fail to persist identical bytes.
     // Do not claim a usable backup until it has been read back and validated.
     BackupGuardian.verifySavedCopy(this,destination,file);
     runOnUiThread(()->new AlertDialog.Builder(this)
      .setTitle("Verified JSON saved and checked")
      .setMessage("Your exported account file was reopened, matched by SHA-256, and validated as a readable ledger. Keep a copy outside this app before uninstalling or changing phones.")
      .setPositiveButton("OK",null).show());
    }catch(Exception ex){
     throw new IOException("Exported JSON was not verified. Do not rely on this file as your only backup. "+ex.getMessage(),ex);
    }finally{file.delete();}
   });
   return;
  }
  if(req!=PICK_LEDGER||resultCode!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;
  background(()->{
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Could not open file");byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(n==0)continue;if(out.size()+n>BackupGuardian.MAX_BACKUP_BYTES)throw new IOException("Migration file exceeds the 12 MB verified-export limit");out.write(buf,0,n);}}
   MigrationCodec.Preview result=MigrationCodec.inspect(out.toString(StandardCharsets.UTF_8.name()));
   if(database.dao().metaCount()>0)throw new IllegalStateException("The native ledger already contains data. Replacement is blocked for your safety.");pendingImport=result;
   runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Verified ledger ready")
    .setMessage("SHA-256 verified\n\n"+result.players+" members · "+result.payments+
      " payments · "+result.expenses+" expenses · "+result.stockUsage+" usage entries"+
      "\nOpening "+rupee(result.openingBalance)+
      "\nClosing "+rupee(result.closingBalance)+" through "+result.lastMonth+
      "\n\nHistorical differences: "+result.differences.size()+
      "\n\nImport into this EMPTY app? Your original app will remain unchanged.")
    .setNegativeButton("Cancel",(d,w)->pendingImport=null)
    .setPositiveButton("Import securely",(d,w)->importPending()).show());
  });
 }
 private void importPending(){MigrationCodec.Preview preview=pendingImport;pendingImport=null;if(preview==null)return;
  background(()->{MigrationCodec.importChecked(database,preview);BackupGuardian.save(this,database,"import",true);warnOnBackupFailure();runOnUiThread(()->{Toast.makeText(this,"Ledger imported. Review backups in Reports.",Toast.LENGTH_LONG).show();showHome();});});}

 private int memberRenderGeneration=0;
 private static final class MemberCardData {
  final Member member;
  final String name,status,detail;
  final long monthly,lifetime,donated;
  final boolean active;
  MemberCardData(Member person,String display,String state,String caption,
                 long cash,long allCash,long packs,boolean enabled){
   member=person;name=display;status=state;detail=caption;
   monthly=cash;lifetime=allCash;donated=packs;active=enabled;
  }
 }
 private void showMembers(){
  title("Members","Actual payments, assessed dues and immutable member histories");
  monthPicker();
  final String viewedMonth=reportMonth;
  EditText search=new EditText(this);
  search.setSingleLine(true);search.setTextSize(14);search.setHint("Search members by name");
  search.setBackground(border(Color.WHITE,LINE,14));pad(search,15,13);
  search.setText(searchText);content.addView(search);gap(content,12);

  // Six or more filters must scroll, not compress into unreadable tiny chips.
  LinearLayout filters=row();
  for(String option:new String[]{"All","Paid","Partial","Unpaid","Settled","Not due","Unconfigured"}){
   TextView chip=pill(option,option.equals(memberFilter)?Color.WHITE:GREEN,
     option.equals(memberFilter)?GREEN:PALE);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
   lp.setMargins(dp(3),0,dp(5),0);
   filters.addView(chip,lp);
   pressable(chip,()->{
    memberFilter=option;searchText=search.getText().toString();showMembers();
   });
  }
  HorizontalScrollView strip=new HorizontalScrollView(this);
  strip.setHorizontalScrollBarEnabled(false);strip.addView(filters);
  content.addView(strip);gap(content,14);
  content.addView(button("Monthly fee rules, dues & concessions",this::showFeeCentre));
  gap(content,13);
  LinearLayout results=col();content.addView(results);gap(content,10);
  content.addView(button("＋ Add new player",this::addMember),fill());

  Runnable refresh=()->{
   final String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
   final String filter=memberFilter;
   final int generation=++memberRenderGeneration;
   background(()->{
    if(generation!=memberRenderGeneration)return;
    LedgerDao dao=database.dao();LedgerMeta meta=dao.meta();
    if(meta==null){
     runOnUiThread(()->{
      if(generation==memberRenderGeneration)notice("No verified ledger","Import your saved club ledger first.");
     });return;
    }
    List<Member> members=dao.members();
    List<Contribution> payments=dao.contributions();
    List<LegacyEntry> legacy=dao.legacy();
    HashSet<String> known=new HashSet<>();
    for(Member member:members)known.add(member.id);
    String raw=meta.inventoryStockJson;
    Map<String,CashCorrectionsV256.State> corrections=CashCorrectionsV256.states(raw,payments);
    Map<String,PremiumV26State.Profile> profiles=PremiumV26State.profiles(raw,known);
    // All financial arithmetic is done on the worker before constructing Android views.
    Map<String,long[]> sums=new HashMap<>();
    for(Member member:members)sums.put(member.id,new long[3]); // month, lifetime cash, donated packs
    for(Contribution item:payments){
     long[] amount=sums.get(item.memberId);
     if(amount==null)continue;
     if("packs".equals(item.kind))amount[2]=Math.addExact(amount[2],item.amount);
     else if("cash".equals(item.kind)){
      long effective=corrections.containsKey(item.id)?corrections.get(item.id).effective:item.amount;
      amount[1]=Math.addExact(amount[1],effective);
      if(viewedMonth.equals(item.feeMonth))amount[0]=Math.addExact(amount[0],effective);
     }
    }
    for(LegacyEntry old:legacy){
     long[] total=sums.get(old.memberId);
     if(total==null)continue;
     if("cash".equals(old.kind))total[1]=Math.addExact(total[1],old.amount);
     else if("packs".equals(old.kind))total[2]=Math.addExact(total[2],old.amount);
    }
    ArrayList<MemberCardData> rows=new ArrayList<>();
    for(Member member:members){
     String name=effectiveName(member,profiles);
     if(!name.toLowerCase(Locale.ROOT).contains(query))continue;
     long[] totals=sums.get(member.id);
     boolean active=!profiles.containsKey(member.id)||profiles.get(member.id).active;
     boolean billable=PremiumV26State.billableMonth(raw,member.id,viewedMonth);
     PremiumV26State.FeeSummary assessment=PremiumV26State.feeSummary(
       raw,member.id,viewedMonth,viewedMonth,payments);
     String status=PremiumV26State.monthStatus(assessment,billable);
     if(!"All".equals(filter)&&!filter.equals(status))continue;
     long assessed=Math.subtractExact(assessment.billed,assessment.waived);
     String detail="Cash "+rupee(totals[0])+
       (status.equals("Unconfigured")?" · Fee not configured":
        status.equals("Not due")?" · No fee assessed":
        " · Assessed "+rupee(assessed)+(assessment.waived>0?
           " · Concession "+rupee(assessment.waived):""));
     rows.add(new MemberCardData(member,name,status,detail,
       totals[0],totals[1],totals[2],active));
    }
    runOnUiThread(()->{
     if(generation!=memberRenderGeneration||!"Members".equals(tab))return;
     results.removeAllViews();
     for(MemberCardData item:rows){
      LinearLayout card=row();
      card.addView(avatar(item.name));
      LinearLayout labels=col();pad(labels,11,0);
      labels.addView(text(item.name+(item.active?"":" · Archived"),14,DARK,true));gap(labels,5);
      labels.addView(text(viewedMonth+" · "+item.detail,11,GRAY,false));
      card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
      boolean settled="Paid".equals(item.status)||"Settled".equals(item.status);
      boolean partial="Partial".equals(item.status);
      card.addView(pill(item.status,settled?GREEN:partial?0xFF9B6B18:GRAY,
        settled?PALE:partial?0xFFFFF2D7:0xFFF1F3F1));
      pressable(card,()->memberWallet(item.member,item.monthly,item.lifetime,item.donated));
      panel(results,card);
     }
     if(rows.isEmpty())panel(results,blockHeader("No matching members",
       "Choose another payment status or try another name."));
    });
   });
  };
  refresh.run();
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence value,int start,int before,int count){
    searchText=value.toString();refresh.run();
   }
   public void afterTextChanged(android.text.Editable ignored){}
  });
 }

 private void memberWallet(Member m,long monthly,long lifetime,long donated){
  background(()->{
   ArrayList<Contribution> entries=new ArrayList<>();
   List<Contribution> allPayments=database.dao().contributions();
   Map<String,CashCorrectionsV256.State> corrections=CashCorrectionsV256.states(
    database.dao().meta().inventoryStockJson,allPayments);
   for(Contribution c:allPayments)if(m.id.equals(c.memberId))entries.add(c);
   entries.sort((a,b)->{
    String am=a.feeMonth==null?"":a.feeMonth,bm=b.feeMonth==null?"":b.feeMonth;
    int month=bm.compareTo(am);if(month!=0)return month;
    String ad=a.receivedDate==null?"":a.receivedDate,bd=b.receivedDate==null?"":b.receivedDate;
    return bd.compareTo(ad);
   });
   ArrayList<LegacyEntry> historical=new ArrayList<>();
   for(LegacyEntry e:database.dao().legacy())if(m.id.equals(e.memberId))historical.add(e);
   String displayName=effectiveName(m,PremiumV26State.profiles(
    database.dao().meta().inventoryStockJson,knownMembers()));
   runOnUiThread(()->{
    LinearLayout body=col();pad(body,20,17);
    LinearLayout head=row();head.addView(avatar(m.name));
    LinearLayout label=col();pad(label,12,0);
    label.addView(text(displayName,19,DARK,true));gap(label,5);
    label.addView(text("Complete recorded member history",12,GRAY,false));
    head.addView(label);body.addView(head);gap(body,18);
    LinearLayout hero=col();hero.setBackground(gradient(GREEN,DARK,18));pad(hero,17,20);
    hero.addView(text("TOTAL RECORDED CASH CONTRIBUTIONS",11,0xFFD4E9DF,true));gap(hero,10);
    hero.addView(text(rupee(lifetime),30,Color.WHITE,true));gap(hero,7);
    hero.addView(text("Viewing month: "+rupee(monthly)+" cash  ·  Donated: "+donated+" pack(s)",12,Color.WHITE,false));
    body.addView(hero);gap(body,17);
    body.addView(text("Recorded monthly entries ("+entries.size()+")",16,DARK,true));gap(body,11);
    for(Contribution c:entries){
     LinearLayout item=row();LinearLayout info=col();
     String period=c.feeMonth==null?"Month not recorded":dateLabel(c.feeMonth);
     info.addView(text(period,13,DARK,true));gap(info,3);
     String date=(c.approximate||c.receivedDate==null||c.receivedDate.isEmpty())?"Exact day not recorded":"Recorded "+c.receivedDate;
     CashCorrectionsV256.State state=corrections.get(c.id);
     long recognized="packs".equals(c.kind)?c.amount:state==null?c.amount:state.effective;
     String type="packs".equals(c.kind)?"Donated shuttle pack · noncash":
       state==null?"Cash contribution":state.effective==0?"CANCELED cash payment":
       "Corrected cash contribution";
     info.addView(text(type+" · "+date,10,GRAY,false));
     item.addView(info,new LinearLayout.LayoutParams(0,-2,1));
     LinearLayout right=col();right.setGravity(Gravity.END);
     right.addView(text("packs".equals(c.kind)?"+"+c.amount+" pk":rupee(recognized),13,
       recognized==0?GRAY:GREEN,true));
     if("cash".equals(c.kind)&&c.amount>0){
      gap(right,5);
      right.addView(inlineButton(state!=null&&recognized==0?"Share void record":"Share receipt",
       GREEN,()->{
       memberDialog.dismiss();
       shareRecordedReceipt(m,c);
      }));
      gap(right,4);
      right.addView(inlineButton(recognized>0?"Correct":"Restore audited",GREEN,()->{
       memberDialog.dismiss();openCashCorrection(m,c);
      }));
     }
     item.addView(right);body.addView(item);gap(body,11);
    }
    if(entries.isEmpty())body.addView(text("No separate monthly entries yet.",12,GRAY,false));
    gap(body,12);
    body.addView(text("Imported historical periods ("+historical.size()+")",16,DARK,true));gap(body,9);
    body.addView(text("Original 2025 period codes are preserved. Share Statement provides a historical cash record, not a newly dated receipt. Donated packs are never treated as cash.",11,GRAY,false));gap(body,10);
    for(LegacyEntry e:historical){
     LinearLayout item=row();LinearLayout info=col();
     info.addView(text(e.period==null?"Historical period":e.period,13,DARK,true));gap(info,3);
     info.addView(text("packs".equals(e.kind)?"Donated shuttle pack · historical":"Cash contribution · historical",10,GRAY,false));
     item.addView(info,new LinearLayout.LayoutParams(0,-2,1));
     LinearLayout right=col();right.setGravity(Gravity.END);
     right.addView(text("packs".equals(e.kind)?"+"+e.amount+" pk":rupee(e.amount),13,GREEN,true));
     if("cash".equals(e.kind)&&e.amount>0){
      gap(right,5);
      right.addView(inlineButton("Share statement",GREEN,()->{
       memberDialog.dismiss();
       shareHistoricalStatement(m,e);
      }));
     }
     item.addView(right);body.addView(item);gap(body,11);
    }
    if(historical.isEmpty())body.addView(text("No imported historical entries.",12,GRAY,false));
    gap(body,16);
    body.addView(button("Edit member profile / archive safely",()->{memberDialog.dismiss();openMemberProfile(m);}),fill());gap(body,8);
    body.addView(button("Export this member's recorded receipt PDFs",()->{memberDialog.dismiss();exportMemberReceiptsZip(m);}),fill());gap(body,8);
    body.addView(button("＋ Collect from this player",()->{memberDialog.dismiss();openPayment(m.id);}),fill());
    ScrollView scroll=new ScrollView(this);scroll.addView(body);memberDialog=new Dialog(this);
    memberDialog.setContentView(scroll);Window w=memberDialog.getWindow();
    if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(-1,-2);w.setGravity(Gravity.BOTTOM);
     w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=.45f;w.setAttributes(p);}
    memberDialog.show();
    if(w!=null)w.setLayout(-1,Math.min(dp(770),(int)(getResources().getDisplayMetrics().heightPixels*.90f)));
   });
  });
 }
 private Dialog memberDialog;
 private File pendingEncryptedExport;
 private static final int SAVE_ENCRYPTED_V26=2010,READ_ENCRYPTED_V26=2011;
 private boolean encryptedImportAfterCheck=false;
 private void addMember(){EditText e=new EditText(this);e.setHint("Full player name");
  new AlertDialog.Builder(this).setTitle("New player").setView(e).setNegativeButton("Cancel",null)
   .setPositiveButton("Add",(d,w)->background(()->{
    String n=e.getText().toString().trim();
    if(n.isEmpty()||n.length()>100)throw new IllegalArgumentException("Name must be 1–100 characters");
    database.runInTransaction(()->{
     requireImportedLedger();
     Map<String,PremiumV26State.Profile> profiles=PremiumV26State.profiles(database.dao().meta().inventoryStockJson,knownMembers());
     String normal=n.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
     for(Member other:database.dao().members())if(
      effectiveName(other,profiles).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").equals(normal))
      throw new IllegalArgumentException("Possible duplicate member: "+n+"; original ID protected");
     Member m=new Member();m.id="native-"+UUID.randomUUID();m.name=n;database.dao().insertMember(m);
     AuditEvent a=new AuditEvent();a.id="audit-"+UUID.randomUUID();
     a.createdAt=new Date().toString();a.action="New member";
     a.details="Created member "+m.id+" with display name "+n;database.dao().audit(a);
    });
    BackupGuardian.save(this,database,"member-added",true);warnOnBackupFailure();
    runOnUiThread(this::showMembers);
   })).show();
 }


 private File renderCashReceipt(Member member,Contribution payment) throws Exception {
  LedgerDao dao=database.dao();
  if(dao.meta()==null)throw new IllegalStateException("Ledger not imported");
  Contribution found=null;
  List<Contribution> payments=dao.contributions();
  for(Contribution row:payments)if(row.id.equals(payment.id)){
   found=row;break;
  }
  if(found==null||!"cash".equals(found.kind)||found.amount<=0||
    !member.id.equals(found.memberId))
   throw new IllegalArgumentException("The original recorded cash payment could not be verified.");
  CashCorrectionsV256.State state=CashCorrectionsV256.stateFor(
   dao.meta().inventoryStockJson,payments,found);
  return state.corrected()?
   CashCorrectionPdfV256.make(this,member,found,dao.meta(),state):
   HistoricalReceiptsV253.payment(this,member,found,dao.meta());
 }
 private void shareRecordedReceipt(Member member,Contribution payment){
  if(member==null||payment==null||!"cash".equals(payment.kind)||payment.amount<=0){
   error(new IllegalArgumentException("Only an already-recorded cash contribution can be shared."));
   return;
  }
  sharePdf("Recorded_Receipt_"+payment.id+".pdf",
   ()->renderCashReceipt(member,payment));
 }
 private void shareHistoricalStatement(Member member,LegacyEntry historical){
  if(member==null||historical==null||!"cash".equals(historical.kind)||historical.amount<=0){
   error(new IllegalArgumentException("Only a positive historical cash contribution can be shared."));
   return;
  }
  sharePdf("Historical_Statement_"+member.id+"_"+historical.period+".pdf",
   ()->HistoricalReceiptsV253.legacy(this,member,historical,database.dao().meta()));
 }

 private static final class ReceiptIndexLine {
  final Member member;
  final Contribution payment;
  final LegacyEntry historical;
  final CashCorrectionsV256.State correction;
  ReceiptIndexLine(Member m,Contribution c,LegacyEntry e,CashCorrectionsV256.State state){
   member=m;payment=c;historical=e;correction=state;
  }
  boolean isHistorical(){return historical!=null;}
  String period(){return isHistorical()?(historical.period==null?"Unknown period":historical.period):
    (payment.feeMonth==null?"Unknown month":payment.feeMonth);}
  long amount(){return isHistorical()?historical.amount:correction==null?payment.amount:correction.effective;}
 }

 private void openCashCorrection(Member member,Contribution payment){
  background(()->{
   requireImportedLedger();
   LedgerDao dao=database.dao();LedgerMeta meta=dao.meta();
   List<Contribution> all=dao.contributions();
   Contribution target=null;
   for(Contribution c:all)if(c.id.equals(payment.id)){target=c;break;}
   if(target==null||!"cash".equals(target.kind)||!member.id.equals(target.memberId))
    throw new IllegalArgumentException("Recorded cash payment no longer matches the selected member.");
   CashCorrectionsV256.State state=CashCorrectionsV256.stateFor(meta.inventoryStockJson,all,target);
   // An explicitly audited RESTORE is permitted for a previously canceled original.
   final Contribution original=target;
   final long reviewedEffective=state.effective;
   ReportMath.Result last=null;
   for(ReportMath.Result item:ReportMath.monthly(dao,ReportMath.lastMonth(dao)))last=item;
   final long reviewedClubClosing=last==null?0:last.closing;
   runOnUiThread(()->{
    Dialog dialog=new Dialog(this);
    LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,20,21);
    form.addView(text("Correct an existing cash payment",20,DARK,true));gap(form,8);
    form.addView(text("Original payment "+original.id+" remains preserved. This action adds an audited adjustment and recalculates balances.",12,GRAY,false));gap(form,16);
    form.addView(text(member.name+" · "+original.feeMonth,16,DARK,true));gap(form,5);
    form.addView(text("Original: "+rupee(original.amount)+" · Currently recognized: "+rupee(reviewedEffective),13,GREEN,true));gap(form,16);
    form.addView(text("ACTION",11,GRAY,true));gap(form,7);
    Spinner choice=new Spinner(this);
    choice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
      reviewedEffective==0?new String[]{"Restore canceled original"}:new String[]{"Correct amount","Cancel incorrect payment","Refund cash actually returned"}));
    form.addView(choice);gap(form,15);
    form.addView(text("NEW RECOGNIZED CASH AMOUNT (₹)",11,GRAY,true));gap(form,7);
    EditText amount=new EditText(this);amount.setSingleLine(true);
    amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    amount.setText(String.valueOf(reviewedEffective==0?original.amount:reviewedEffective));
    amount.setBackground(border(LIGHT,LINE,12));pad(amount,14,12);
    form.addView(amount);gap(form,10);
    form.addView(text("For a refund, enter the amount retained after the actual refund. For cancellation, the recognized amount becomes zero.",11,GRAY,false));gap(form,15);
    form.addView(text("REASON (REQUIRED, 8-200 CHARACTERS)",11,GRAY,true));gap(form,7);
    EditText reason=new EditText(this);reason.setMinLines(2);reason.setHint("Explain the correction or actual refund");
    reason.setBackground(border(LIGHT,LINE,12));pad(reason,14,12);form.addView(reason);gap(form,15);
    form.addView(button("Review financial correction",()->{
     try{
      int selected=choice.getSelectedItemPosition();
      final String action=reviewedEffective==0?"RESTORE":selected==1?"CANCEL":selected==2?"REFUND":"CORRECT";
      final long next=(reviewedEffective>0&&selected==1)?0:Long.parseLong(amount.getText().toString().trim());
      final String why=reason.getText().toString().trim();
      if(next<0||next>1000000000L||next==reviewedEffective)
       throw new IllegalArgumentException("Enter a different recognized amount up to ₹100 crore.");
      if("REFUND".equals(action)&&next>=reviewedEffective)
       throw new IllegalArgumentException("Refund must reduce recognized cash. Never use this for a promised but unpaid refund.");
      if("RESTORE".equals(action)&&(next<=0||next>original.amount))
       throw new IllegalArgumentException("Restoration amount must be positive and no greater than the original payment.");
      if(why.length()<8||why.length()>200)
       throw new IllegalArgumentException("Explain the change in 8-200 characters.");
      long currentClosing=reviewedClubClosing;
      long nextClosing=Math.addExact(currentClosing,Math.subtractExact(next,reviewedEffective));
      String explanation="Member: "+member.name+"\nPayment ID: "+original.id+
       "\nMonth: "+original.feeMonth+
       "\nPrevious recognized cash: "+rupee(reviewedEffective)+
       "\nNew recognized cash: "+rupee(next)+
       "\nImpact on club balance: "+(next-reviewedEffective>0?"+":"")+rupee(next-reviewedEffective)+
       "\nProjected latest closing: "+rupee(nextClosing)+
       " (currently "+rupee(currentClosing)+")"+
       "\nAction: "+action+
       "\nReason: "+why+
       "\n\nOriginal entry and all past corrections remain in audit history. Confirm financial adjustment?";
      new AlertDialog.Builder(this).setTitle("Confirm balance-changing correction")
       .setMessage(explanation).setNegativeButton("Keep editing",null)
       .setPositiveButton("Commit audited correction",(d,w)->{
        dialog.dismiss();
        background(()->{
         database.runInTransaction(()->{
          LedgerDao liveDao=database.dao();LedgerMeta live=liveDao.meta();
          if(live==null)throw new IllegalStateException("No imported ledger");
          requireOpenMonth(original.feeMonth);
          List<Contribution> records=liveDao.contributions();
          Contribution current=null;
          for(Contribution c:records)if(c.id.equals(original.id)){current=c;break;}
          if(current==null||!current.memberId.equals(member.id)||current.amount!=original.amount)
           throw new IllegalStateException("Original payment identity changed. Correction blocked.");
          CashCorrectionsV256.State now=CashCorrectionsV256.stateFor(live.inventoryStockJson,records,current);
          if(now.effective!=reviewedEffective)throw new IllegalStateException("Payment was changed while reviewing. Reopen before correcting.");
          String event="correction-"+UUID.randomUUID();
          String at=new Date().toString();
          String json=CashCorrectionsV256.append(live.inventoryStockJson,records,current,next,action,why,event,at);
          if(liveDao.updateInventoryJson(json)!=1)
           throw new IllegalStateException("Correction was not stored.");
          AuditEvent audit=new AuditEvent();
          audit.id=event;audit.createdAt=at;
          audit.action="CASH "+action+" on original "+original.id;
          audit.details="Member "+member.id+"; "+original.feeMonth+"; recognized "+
           reviewedEffective+" -> "+next+"; reason: "+why;
          liveDao.audit(audit);
         });
         BackupGuardian.save(this,database,"cash-correction",true);
         warnOnBackupFailure();
         runOnUiThread(()->{
          Toast.makeText(this,"Audited correction saved",Toast.LENGTH_LONG).show();
          showReceiptLibrary();
         });
        });
       }).show();
     }catch(Exception ex){error(new IllegalArgumentException(ex.getMessage()==null?"Check correction values.":ex.getMessage()));}
    }));
    ScrollView scroll=new ScrollView(this);scroll.addView(form);
    dialog.setContentView(scroll);dialog.show();
    Window window=dialog.getWindow();if(window!=null){
     window.setBackgroundDrawableResource(android.R.color.transparent);
     window.setGravity(Gravity.BOTTOM);
     window.setLayout(-1,Math.min(dp(790),(int)(getResources().getDisplayMetrics().heightPixels*.92f)));
    }
   });
  });
 }

 private void showReceiptLibrary(){
  title("Receipt Library","Search existing cash payments across every month");
  content.addView(button("‹ Back to Collect",this::showPayments),fill());gap(content,12);
  notice("Reissue, not a new collection","Sharing or saving a PDF does not add a payment. Imported historical 2025 period totals are shared as statements; missing dates or methods are not invented.");
  LinearLayout filterCard=col();
  filterCard.addView(text("SEARCH MEMBER, PERIOD OR AMOUNT",11,GRAY,true));gap(filterCard,9);
  EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);
  search.setHint("Player, month, receipt ID, amount, payment method");
  search.setBackground(border(LIGHT,LINE,12));pad(search,14,12);
  filterCard.addView(search);gap(filterCard,12);
  LinearLayout filters=row();
  filterCard.addView(filters);panel(content,filterCard);
  TextView count=text("Reading recorded payments…",12,GRAY,false);
  content.addView(count);gap(content,9);
  LinearLayout results=col();content.addView(results);
  background(()->{
   requireImportedLedger();
   LedgerDao dao=database.dao();
   Map<String,Member> members=new HashMap<>();
   for(Member m:dao.members())members.put(m.id,m);
   ArrayList<ReceiptIndexLine> all=new ArrayList<>();
   List<Contribution> indexedPayments=dao.contributions();
   Map<String,CashCorrectionsV256.State> corrections=CashCorrectionsV256.states(
     dao.meta().inventoryStockJson,indexedPayments);
   for(Contribution c:indexedPayments){
    Member m=members.get(c.memberId);
    if(m!=null&&"cash".equals(c.kind)&&c.amount>0)all.add(new ReceiptIndexLine(m,c,null,corrections.get(c.id)));
   }
   for(LegacyEntry item:dao.legacy()){
    Member m=members.get(item.memberId);
    if(m!=null&&"cash".equals(item.kind)&&item.amount>0)all.add(new ReceiptIndexLine(m,null,item,null));
   }
   all.sort((a,b)->{
    if(a.isHistorical()!=b.isHistorical())return a.isHistorical()?1:-1;
    if(!a.isHistorical()){
     int byMonth=b.period().compareTo(a.period());
     if(byMonth!=0)return byMonth;
     String aDate=a.payment.receivedDate==null?"":a.payment.receivedDate;
     String bDate=b.payment.receivedDate==null?"":b.payment.receivedDate;
     return bDate.compareTo(aDate);
    }
    return 0; // Preserve original order for archive periods without known calendar dates.
   });
   runOnUiThread(()->{
    final String[] mode={"All"};
    final int[] shownLimit={25};
    final Runnable[] repaint=new Runnable[1];
    repaint[0]=()->{
     results.removeAllViews();
     String query=search.getText().toString().trim().toLowerCase(Locale.ROOT).replace("₹","").replace(",","").replace("inr","").trim();
     int matches=0,rendered=0,recorded=0,archived=0;
     for(ReceiptIndexLine item:all){
      if(item.isHistorical())archived++;else recorded++;
      if("Recorded".equals(mode[0])&&item.isHistorical())continue;
      if("Historical".equals(mode[0])&&!item.isHistorical())continue;
      String haystack=(item.member.name+" "+item.period()+" "+item.amount()+
       " "+(item.payment==null?"":item.payment.id+" "+
         (item.payment.receivedDate==null?"":item.payment.receivedDate)+" "+
         (item.payment.method==null?"":item.payment.method))).toLowerCase(Locale.ROOT);
      if(!query.isEmpty()&&!haystack.contains(query))continue;
      matches++;
      if(rendered>=shownLimit[0])continue;
      rendered++;
      LinearLayout card=col();
      LinearLayout main=row();
      main.addView(avatar(item.member.name));
      LinearLayout labels=col();pad(labels,11,0);
      labels.addView(text(item.member.name,15,DARK,true));gap(labels,4);
      String period=item.period();
      String detail;
      if(item.isHistorical()){
       detail="Archived period "+period+" · exact payment date unavailable";
      }else{
       Contribution c=item.payment;
       boolean dateKnown=!c.approximate&&c.receivedDate!=null&&!c.receivedDate.isEmpty();
       detail="Fee month "+period+" · "+(dateKnown?c.receivedDate:"exact payment date unavailable")+
        (item.correction==null?"":item.correction.effective==0?" · CANCELED":
          " · ADJUSTED / "+item.correction.action);
      }
      labels.addView(text(detail,11,GRAY,false));
      main.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
      card.addView(main);gap(card,11);
      LinearLayout money=row();
      money.addView(text(item.isHistorical()?"Historical contribution statement":item.correction==null?"Previously recorded payment":item.amount()==0?"CANCELED record":"Corrected payment",11,GRAY,false),
        new LinearLayout.LayoutParams(0,-2,1));
      money.addView(text(rupee(item.amount()),16,item.amount()==0?GRAY:GREEN,true));
      card.addView(money);gap(card,11);
      LinearLayout actions=row();
      if(item.isHistorical()){
       actions.addView(inlineButton("Share",GREEN,()->shareHistoricalStatement(item.member,item.historical)),
        new LinearLayout.LayoutParams(0,-2,1));
       actions.addView(inlineButton("Preview",GREEN,()->previewPdf(()->HistoricalReceiptsV253.legacy(
        this,item.member,item.historical,database.dao().meta()))),
        new LinearLayout.LayoutParams(0,-2,1));
       actions.addView(inlineButton("Save PDF",GREEN,()->savePdf(
        "Historical_Statement_"+item.member.id+"_"+item.period()+".pdf",
        ()->HistoricalReceiptsV253.legacy(this,item.member,item.historical,database.dao().meta()))),
        new LinearLayout.LayoutParams(0,-2,1));
      }else{
       actions.addView(inlineButton("Share",GREEN,()->shareRecordedReceipt(item.member,item.payment)),
        new LinearLayout.LayoutParams(0,-2,1));
       actions.addView(inlineButton("Preview",GREEN,()->previewPdf(()->renderCashReceipt(item.member,item.payment))),
        new LinearLayout.LayoutParams(0,-2,1));
       actions.addView(inlineButton("Save PDF",GREEN,()->savePdf(
        "Recorded_Receipt_"+item.payment.id+".pdf",
        ()->renderCashReceipt(item.member,item.payment))),
        new LinearLayout.LayoutParams(0,-2,1));
      }
      card.addView(actions);
      if(!item.isHistorical()){
       gap(card,8);
       card.addView(inlineButton(item.amount()>0?
         "Correct payment / refund / cancel":"Audited restoration of canceled original",GREEN,
         ()->openCashCorrection(item.member,item.payment)));
      }
      panel(results,card);
     }
     count.setText("Matching cash records: "+matches+" · Showing "+rendered+
      " · Recorded entries "+recorded+" · Archived periods "+archived);
     if(matches==0)panel(results,blockHeader("No matching cash records","Try a member name or period. Donated shuttle packs are intentionally excluded."));
     if(matches>shownLimit[0]){
      results.addView(button("Show next 25 receipts",()->{shownLimit[0]+=25;repaint[0].run();}),fill());
     }
    };
    for(String option:new String[]{"All","Recorded","Historical"}){
     TextView chip=pill(option,GREEN,PALE);
     filters.addView(chip,new LinearLayout.LayoutParams(0,-2,1));
     pressable(chip,()->{
      mode[0]=option;shownLimit[0]=25;repaint[0].run();
     });
    }
    search.addTextChangedListener(new android.text.TextWatcher(){
     public void beforeTextChanged(CharSequence v,int start,int count,int after){}
     public void onTextChanged(CharSequence value,int start,int before,int count){
      shownLimit[0]=25;repaint[0].run();
     }
     public void afterTextChanged(android.text.Editable editable){}
    });
    repaint[0].run();
   });
  });
 }

 private void showPayments(){title("Collect fees","Simple, quick and ready to share");monthPicker();
  LinearLayout headline=col();headline.setBackground(gradient(GREEN,DARK,22));pad(headline,20,22);headline.addView(text("ONE-TAP COLLECTION",11,GOLD,true));gap(headline,10);headline.addView(text("Record a contribution",22,Color.WHITE,true));gap(headline,8);headline.addView(text("Choose a player, confirm the amount, then share a professional PDF receipt.",12,0xFFE1F1E9,false));gap(headline,15);headline.addView(button("＋  Record cash actually received",()->openPayment(null)));gap(headline,9);
  headline.addView(button("▤  Find or share any past receipt",this::showReceiptLibrary));
  content.addView(headline);gap(content,16);
  notice("Previously paid?", "Use the Receipt Library to search all recorded months by member and share or save the PDF. Imported 2025 period totals are labeled Historical Contribution Statements.");
  sectionHeading("Monthly receipts","Recorded for "+dateLabel(reportMonth));final String viewedMonth=reportMonth;
  background(()->{List<Contribution> rows=database.dao().contributions();
  Map<String,CashCorrectionsV256.State> corrections=CashCorrectionsV256.states(database.dao().meta().inventoryStockJson,rows);
  Map<String,Member> names=new HashMap<>();for(Member m:database.dao().members())names.put(m.id,m);runOnUiThread(()->{int n=0;for(int i=rows.size()-1;i>=0;i--){Contribution c=rows.get(i);if("packs".equals(c.kind)||!viewedMonth.equals(c.feeMonth))continue;Member m=names.get(c.memberId);if(m==null)continue;
   LinearLayout card=row();card.addView(avatar(m.name));LinearLayout info=col();pad(info,11,0);info.addView(text(m.name,14,DARK,true));gap(info,5);info.addView(text(dateLabel(c.feeMonth)+(c.approximate?" · Imported":" · "+c.method),11,GRAY,false));card.addView(info,new LinearLayout.LayoutParams(0,-2,1));LinearLayout right=col();right.setGravity(Gravity.END);long net=corrections.containsKey(c.id)?corrections.get(c.id).effective:c.amount;
   right.addView(text(net==0?"VOID":rupee(net),15,net==0?GRAY:GREEN,true));gap(right,6);
   right.addView(inlineButton(net==0?"Void record":"Receipt ▤",GREEN,()->shareRecordedReceipt(m,c)));card.addView(right);panel(content,card);if(++n==16)break;}
   if(n==0)notice("No receipts this month","Choose another month above or collect a contribution.");});});
 }
 private void openPayment(String chosen){background(()->{requireImportedLedger();
    List<Member> all=database.dao().members();
    Map<String,PremiumV26State.Profile> profiles=PremiumV26State.profiles(
     database.dao().meta().inventoryStockJson,knownMembers());
    List<Member> members=new ArrayList<>();
    for(Member item:all)if(!profiles.containsKey(item.id)||profiles.get(item.id).active)members.add(item);
    runOnUiThread(()->{
   if(members.isEmpty()){error(new IllegalStateException("Import your verified ledger first"));return;}
   Dialog dialog=new Dialog(this);LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,21,23);
   LinearLayout cap=row();LinearLayout headline=col();headline.addView(text("New contribution",22,DARK,true));gap(headline,5);headline.addView(text("Securely record and share",12,GRAY,false));cap.addView(headline,new LinearLayout.LayoutParams(0,-2,1));TextView close=inlineButton("✕",GRAY,dialog::dismiss);cap.addView(close);form.addView(cap);gap(form,25);
   form.addView(text("PLAYER",11,GRAY,true));gap(form,9);AutoCompleteTextView who=new AutoCompleteTextView(this);who.setSingleLine(true);who.setTextSize(15);who.setThreshold(1);String[] options=new String[members.size()];for(int i=0;i<members.size();i++)options[i]=effectiveName(members.get(i),profiles);
   who.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,options));who.setHint("Search player name");who.setBackground(border(LIGHT,LINE,12));pad(who,14,12);if(chosen!=null)for(Member m:members)if(m.id.equals(chosen))who.setText(effectiveName(m,profiles),false);form.addView(who);gap(form,16);
   form.addView(text("AMOUNT RECEIVED",11,GRAY,true));gap(form,9);EditText amount=new EditText(this);amount.setSingleLine(true);amount.setHint("Actual payment amount (₹)");amount.setText("");amount.setTextSize(23);amount.setTypeface(Typeface.DEFAULT,Typeface.BOLD);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);amount.setBackground(border(LIGHT,LINE,12));pad(amount,14,11);form.addView(amount);gap(form,9);
   LinearLayout presets=row();for(int a:new int[]{500,1000,2000,4000}){TextView c=pill("₹"+a,GREEN,PALE);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(dp(3),0,dp(3),0);presets.addView(c,cp);pressable(c,()->amount.setText(String.valueOf(a)));}form.addView(presets);gap(form,16);
   form.addView(text("CONTRIBUTION MONTH",11,GRAY,true));gap(form,9);
   EditText month=new EditText(this);month.setSingleLine(true);month.setText(reportMonth);
   month.setFocusable(false);month.setClickable(true);
   month.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.ic_menu_my_calendar,0);
   month.setCompoundDrawablePadding(dp(10));month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);
   pressable(month,()->chooseMonth(month.getText().toString(),month::setText));form.addView(month);gap(form,16);
   form.addView(text("PAYMENT METHOD",11,GRAY,true));gap(form,8);Spinner via=new Spinner(this);via.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Cash","UPI","Bank transfer","Other"}));form.addView(via);gap(form,20);
   Button save=button("Save & share receipt  →",()->{
    String name=who.getText().toString().trim();Member selected=null;for(Member m:members)if(effectiveName(m,profiles).equalsIgnoreCase(name)){selected=m;break;}
    if(selected==null){who.setError("Choose a player from the list");return;}final Member m=selected;
    String fee=month.getText().toString().trim();long value;try{MigrationCodec.ym(fee);value=Long.parseLong(amount.getText().toString());if(value<=0)throw new NumberFormatException();}catch(Exception e){error(new IllegalArgumentException("Enter a valid YYYY-MM month and positive amount"));return;}
    String method=via.getSelectedItem().toString();background(()->{int existing=database.dao().existingCash(m.id,fee);runOnUiThread(()->{
     if(existing>0)new AlertDialog.Builder(this).setTitle("Contribution already recorded").setMessage(m.name+" has "+existing+" cash entry/entries for "+fee+". Record an additional payment?")
       .setNegativeButton("Keep editing",null).setPositiveButton("Add payment",(d,w)->{dialog.dismiss();savePayment(m,fee,value,method);}).show();
     else {dialog.dismiss();savePayment(m,fee,value,method);}
    });});
   });form.addView(save);gap(form,9);TextView disclaimer=text("Enter cash actually received. A monthly fee obligation is not proof of collection. Existing payments are checked before saving.",11,GRAY,false);disclaimer.setGravity(Gravity.CENTER);form.addView(disclaimer);
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
  database.runInTransaction(()->{requireImportedLedger();requireOpenMonth(fee);database.dao().insertPayment(c);AuditEvent a=new AuditEvent();a.id="audit-"+UUID.randomUUID();a.createdAt=new Date().toString();a.action="Contribution added";a.details=c.id+" / "+rupee(amount);database.dao().audit(a);});
  BackupGuardian.save(this,database,"payment",true);
  warnOnBackupFailure();
  runOnUiThread(()->{Toast.makeText(this,"Payment saved",Toast.LENGTH_SHORT).show();showPayments();sharePdf("Receipt_"+c.id+".pdf",()->PdfReports.receipt(this,m,c,database.dao().meta()));});
 });}

 private static boolean isShuttlePurchase(Expense e){
  return e!=null&&e.category!=null&&e.category.toLowerCase(Locale.ROOT).contains("shuttle");
 }
 private static String checkedPurchaseDate(String input,String month){
  String value=input==null?"":input.trim();
  if(value.isEmpty())return "";
  if(value.length()!=10||!value.startsWith(month+"-"))
   throw new IllegalArgumentException("Purchase/donation date must use YYYY-MM-DD within the chosen month.");
  java.text.SimpleDateFormat dateParser=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);
  dateParser.setLenient(false);
  try {
   Date parsed=dateParser.parse(value);
   if(parsed==null||!value.equals(dateParser.format(parsed)))
    throw new IllegalArgumentException("Enter a real calendar date.");
  }catch(java.text.ParseException ex){
   throw new IllegalArgumentException("Enter a real calendar date in YYYY-MM-DD format.");
  }
  return value;
 }
 private void showExpenses(){
  title("Shuttle desk","Purchases, donated packs and acquisition history");monthPicker();
  LinearLayout hero=col();hero.setBackground(gradient(GREEN,DARK,22));pad(hero,20,22);
  hero.addView(text("SHUTTLE ACTIVITY",11,0xFFD6EBDD,true));gap(hero,10);
  hero.addView(text("Purchases, donations & usage",21,Color.WHITE,true));gap(hero,9);
  hero.addView(text("Cash spending and noncash donations are always recorded separately.",12,0xFFD6EBDD,false));gap(hero,15);
  hero.addView(button("＋ Record shuttle purchase",this::openExpense));gap(hero,9);
  hero.addView(button("＋ Record donated shuttle pack",this::openShuttleDonation));gap(hero,9);
  hero.addView(button("− Record shuttle usage",this::openShuttleUsage));gap(hero,9);
  hero.addView(button("◉ Configure confirmed opening stock",this::openInventoryCheckpoint));gap(hero,9);
  hero.addView(button("Configure low-stock warning",this::openStockThreshold));gap(hero,9);
  hero.addView(button("✓ Reconcile completed-month physical stock",this::openPhysicalStockCount));
  content.addView(hero);gap(content,16);
  notice("Stock accounting basis","The first stock checkpoint is a confirmed month-start count. Later independently measured end-of-month counts can reconcile discrepancies without rewriting past purchases or usage. Pack sizes are explicit when known.");
  final String selected=reportMonth;
  background(()->{
   List<Expense> entries=database.dao().expenses();
   List<Contribution> contributions=database.dao().contributions();
   List<LegacyEntry> legacy=database.dao().legacy();
   List<StockUsage> issued=database.dao().stockUsages();
   LedgerMeta stockMeta=database.dao().meta();
   InventoryCheckpoint.Config checkpoint=stockMeta==null?null:InventoryCheckpoint.read(stockMeta.inventoryStockJson);
   StockReconciliationV257.Position stockPosition=checkpoint==null?null:
      StockReconciliationV257.calculate(stockMeta.inventoryStockJson,selected,entries,contributions,issued);
   long reorderAt=stockMeta==null?-1:PremiumV26State.reorderThreshold(stockMeta.inventoryStockJson);
   Map<String,String> names=new HashMap<>();
   for(Member person:database.dao().members())names.put(person.id,person.name);
   long spending=0,purchased=0,donated=0,legacyDonated=0,used=0,lifetimeUsed=0;
   ArrayList<Expense> purchases=new ArrayList<>();
   ArrayList<Contribution> donations=new ArrayList<>();
   ArrayList<StockUsage> usageHistory=new ArrayList<>();
   for(Expense e:entries)if(selected.equals(e.month)){
    spending+=e.amount;
    if(isShuttlePurchase(e)){purchased+=Math.max(0,e.quantity);purchases.add(e);}
   }
   for(Contribution c:contributions)if(selected.equals(c.feeMonth)&&"packs".equals(c.kind)){
    donated+=Math.max(0,c.amount);donations.add(c);
   }
   for(LegacyEntry e:legacy)if("packs".equals(e.kind))legacyDonated+=Math.max(0,e.amount);
   for(StockUsage item:issued){
    lifetimeUsed=Math.addExact(lifetimeUsed,Math.max(0,item.shuttles));
    if(selected.equals(item.month)){
     used=Math.addExact(used,Math.max(0,item.shuttles));
     usageHistory.add(item);
    }
   }
   usageHistory.sort((a,b)->{
    String ad=a.date==null?"":a.date,bd=b.date==null?"":b.date;
    return bd.compareTo(ad);
   });
   purchases.sort((a,b)->{
    String ad=a.date==null?"":a.date,bd=b.date==null?"":b.date;
    return bd.compareTo(ad);
   });
   donations.sort((a,b)->{
    String ad=a.receivedDate==null?"":a.receivedDate,bd=b.receivedDate==null?"":b.receivedDate;
    return bd.compareTo(ad);
   });
   final long fSpent=spending,fBought=purchased,fGiven=donated,fLegacyGiven=legacyDonated,
    fUsed=used,fLifetimeUsed=lifetimeUsed;
   runOnUiThread(()->{
    if(checkpoint==null)notice("Stock balance not yet available",
     "No confirmed start-of-month opening stock and pack size. Purchased packs and consumed shuttles remain visible, but remaining stock is intentionally not calculated.");
    else if(stockPosition==null)notice("Stock checkpoint starts in "+checkpoint.month,
     "Choose a month from "+checkpoint.month+" onward to view the stock balance.");
    else{
     sectionHeading("Stock position from confirmed baseline",stockPosition.basis+" · through "+selected);
     String remaining=String.valueOf(stockPosition.remainingShuttles);
     LinearLayout position=row();
     position.addView(metric("Remaining shuttles",remaining,"◉"),new LinearLayout.LayoutParams(0,-2,1));
     position.addView(metric("Pack size",checkpoint.perPack+" each","▤"),new LinearLayout.LayoutParams(0,-2,1));
     content.addView(position);gap(content,10);
     if(reorderAt>=0&&stockPosition.remainingShuttles<reorderAt)
      notice("LOW STOCK — PURCHASE REVIEW",
       "Recorded balance "+stockPosition.remainingShuttles+" shuttles is below the configured reorder threshold of "+
       reorderAt+". Check the physical stock before ordering; a warning is not an automatic purchase.");
     notice(stockPosition.remainingShuttles<0?"Recorded stock deficit":"Stock calculation",
      "Opening: "+checkpoint.openingShuttles+" individual shuttles"+
      "\nAcquired after count: "+stockPosition.acquiredPacks+" packs / "+stockPosition.acquiredShuttles+" individual shuttles"+
      "\nUsed after count: "+stockPosition.usedShuttles+" individual shuttles"+
      "\nCalculated balance: "+stockPosition.remainingShuttles+" individual shuttles."+
      "\nThis is calculated from recorded events and the latest independently confirmed stock count. Older unmeasured usage is not inferred.");
    }
    gap(content,15);
    sectionHeading("Selected-month summary",dateLabel(selected));
    LinearLayout stats=row();
    stats.addView(metric("Shuttle spending",rupee(fSpent),"◈"),new LinearLayout.LayoutParams(0,-2,1));
    stats.addView(metric("Packs purchased",String.valueOf(fBought),"▥"),new LinearLayout.LayoutParams(0,-2,1));
    content.addView(stats);gap(content,9);
    LinearLayout supply=row();
    supply.addView(metric("Packs donated",String.valueOf(fGiven),"＋"),new LinearLayout.LayoutParams(0,-2,1));
    supply.addView(metric("Total packs acquired",String.valueOf(fBought+fGiven),"▤"),new LinearLayout.LayoutParams(0,-2,1));
    content.addView(supply);gap(content,9);
    LinearLayout useStats=row();
    useStats.addView(metric("Shuttles used this month",String.valueOf(fUsed),"−"),new LinearLayout.LayoutParams(0,-2,1));
    useStats.addView(metric("Shuttles used all-time",String.valueOf(fLifetimeUsed),"▦"),new LinearLayout.LayoutParams(0,-2,1));
    content.addView(useStats);gap(content,9);
    notice("Imported 2025 donations","The historic ledger also contains "+fLegacyGiven+" donated pack(s) across its original 2025 periods. These are not added to the selected month's total.");
    gap(content,14);
    sectionHeading("Shuttle purchases ("+purchases.size()+")","Selected month · recorded prices and dates");
    if(purchases.isEmpty())notice("No shuttle purchases this month","Choose another month or record a purchase.");
    for(Expense e:purchases){
     LinearLayout card=row();card.addView(avatar("Shuttle"));
     LinearLayout labels=col();pad(labels,12,0);
     String recorded=(e.date==null||e.date.isEmpty())?"Exact date not recorded":e.date;
     labels.addView(text(recorded,13,DARK,true));gap(labels,5);
     String unit=e.quantity>0?" · approx. "+rupee(e.amount/e.quantity)+"/pack":"";
     labels.addView(text(e.quantity+" pack(s)"+unit,11,GRAY,false));
     card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
     card.addView(text(rupee(e.amount),14,GREEN,true));panel(content,card);
    }
    gap(content,15);
    sectionHeading("Donated packs ("+donations.size()+")","Noncash · selected month");
    if(donations.isEmpty())notice("No pack donations this month","You can record donated packs without affecting cash balances.");
    for(Contribution c:donations){
     LinearLayout card=row();card.addView(avatar(names.getOrDefault(c.memberId,"?")));
     LinearLayout info=col();pad(info,12,0);
     info.addView(text(names.getOrDefault(c.memberId,"Member"),13,DARK,true));gap(info,5);
     String when=c.approximate||c.receivedDate==null||c.receivedDate.isEmpty()?"Exact date not recorded":c.receivedDate;
     info.addView(text(when+" · noncash",11,GRAY,false));
     card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
     card.addView(text("+"+c.amount+" pk",13,GREEN,true));panel(content,card);
    }
    gap(content,16);
    sectionHeading("Shuttle usage ("+usageHistory.size()+")","Individual shuttles · selected month");
    if(usageHistory.isEmpty())notice("No shuttle usage recorded","No usage records have been inferred. Use Record shuttle usage to add a real count.");
    for(StockUsage item:usageHistory){
     LinearLayout card=row();card.addView(avatar("Shuttle"));
     LinearLayout info=col();pad(info,12,0);
     String day=item.date==null||item.date.isEmpty()?"Exact usage date not recorded":item.date;
     info.addView(text(day,13,DARK,true));gap(info,5);
     String details=item.note==null||item.note.isEmpty()?"No note":item.note;
     info.addView(text(details,11,GRAY,false));
     card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
     card.addView(text("−"+item.shuttles+" shuttles",13,GREEN,true));panel(content,card);
    }
   });
  });
 }


 private void openPhysicalStockCount(){
  background(()->{
   requireImportedLedger();
   LedgerDao dao=database.dao();LedgerMeta meta=dao.meta();
   final InventoryCheckpoint.Config base=InventoryCheckpoint.read(meta.inventoryStockJson);
   if(base==null)throw new IllegalStateException("Confirm starting opening stock before recording a physical reconciliation.");
   final List<StockReconciliationV257.Count> old=StockReconciliationV257.read(meta.inventoryStockJson);
   runOnUiThread(()->{
    Dialog dialog=new Dialog(this);
    LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,20,21);
    form.addView(text("Physical stock reconciliation",21,DARK,true));gap(form,8);
    form.addView(text("Record a genuine count of individual shuttles at the END of a COMPLETED calendar month. No historical stock figure is generated or assumed.",12,GRAY,false));gap(form,15);
    if(!old.isEmpty()){
     StockReconciliationV257.Count last=old.get(old.size()-1);
     form.addView(text("Last physical count: "+last.month+" · "+last.physical+" shuttles. Next reconciliation must be a later month.",12,GREEN,true));gap(form,12);
    }
    form.addView(text("COMPLETED MONTH (YYYY-MM)",11,GRAY,true));gap(form,7);
    EditText month=new EditText(this);month.setSingleLine(true);
    String thisMonth=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());
    month.setText(ReportMath.previousMonth(thisMonth));
    month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);form.addView(month);gap(form,13);
    form.addView(text("PHYSICALLY COUNTED INDIVIDUAL SHUTTLES",11,GRAY,true));gap(form,7);
    EditText physical=new EditText(this);physical.setSingleLine(true);
    physical.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    physical.setHint("Enter actual counted quantity, not packs");
    physical.setBackground(border(LIGHT,LINE,12));pad(physical,14,12);form.addView(physical);gap(form,13);
    form.addView(text("COUNT EVIDENCE / REASON (REQUIRED)",11,GRAY,true));gap(form,7);
    EditText note=new EditText(this);note.setMinLines(2);
    note.setHint("Who counted the physical stock, and why it differs if applicable");
    note.setBackground(border(LIGHT,LINE,12));pad(note,14,12);form.addView(note);gap(form,15);
    form.addView(button("Review reconciliation",()->{
     try{
      final String mm=MigrationCodec.ym(month.getText().toString().trim());
      String now=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());
      if(mm.compareTo(now)>=0)
       throw new IllegalArgumentException("Only COMPLETED months can be reconciled; the current month has not ended.");
      if(mm.compareTo(base.month)<0)throw new IllegalArgumentException("Reconciliation predates the opening-stock checkpoint.");
      if(!old.isEmpty()&&mm.compareTo(old.get(old.size()-1).month)<=0)
       throw new IllegalArgumentException("Earlier physical reconciliations are immutable. Choose a later completed month.");
      final long quantity=Long.parseLong(physical.getText().toString().trim());
      if(quantity<0||quantity>100000000L)throw new IllegalArgumentException("Enter a physical count between zero and 100 million.");
      final String reason=note.getText().toString().trim();
      if(reason.length()<8||reason.length()>200)throw new IllegalArgumentException("Explain the physical count in 8-200 characters.");
      background(()->{
       LedgerDao current=database.dao();
       StockReconciliationV257.Position expected=StockReconciliationV257.calculate(
        current.meta().inventoryStockJson,mm,current.expenses(),current.contributions(),current.stockUsages());
       if(expected==null)throw new IllegalStateException("No opening count available for the selected month");
       final long estimated=expected.remainingShuttles;
       final long difference=Math.subtractExact(quantity,estimated);
       runOnUiThread(()->new AlertDialog.Builder(this)
        .setTitle("Confirm actual month-end stock")
        .setMessage("Month: "+mm+"\nCalculated from recorded history: "+estimated+
         " individual shuttles\nPHYSICALLY COUNTED: "+quantity+
         " individual shuttles\nStock reconciliation difference: "+difference+
         "\nReason: "+reason+
         "\n\nThis becomes the physical stock basis for later months. No past expenses, donations, usage or cash payments are rewritten.")
        .setNegativeButton("Keep editing",null)
        .setPositiveButton("Record audited physical count",(d,w)->{
         dialog.dismiss();
         background(()->{
          database.runInTransaction(()->{
           LedgerDao live=database.dao();LedgerMeta active=live.meta();
           if(active==null)throw new IllegalStateException("No ledger");
           requireOpenMonth(mm);
           String event="physical-count-"+UUID.randomUUID();
           String at=new Date().toString();
           String json=StockReconciliationV257.append(active.inventoryStockJson,mm,
            quantity,reason,estimated,event,at,live.expenses(),live.contributions(),live.stockUsages());
           if(live.updateInventoryJson(json)!=1)throw new IllegalStateException("Stock count was not stored");
           AuditEvent audit=new AuditEvent();
           audit.id=event;audit.createdAt=at;audit.action="Month-end physical stock reconciled";
           audit.details=mm+"; expected "+estimated+"; counted "+quantity+
            "; difference "+difference+"; evidence "+reason;
           live.audit(audit);
          });
          BackupGuardian.save(this,database,"physical-reconciliation",true);
          warnOnBackupFailure();
          runOnUiThread(()->{Toast.makeText(this,"Physical count saved",Toast.LENGTH_LONG).show();showExpenses();});
         });
        }).show());
      });
     }catch(Exception ex){
      error(new IllegalArgumentException(ex.getMessage()==null?"Check physical-stock values.":ex.getMessage()));
     }
    }));
    ScrollView scroll=new ScrollView(this);scroll.addView(form);
    dialog.setContentView(scroll);dialog.show();
    Window window=dialog.getWindow();if(window!=null){
     window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.BOTTOM);
     window.setLayout(-1,Math.min(dp(760),(int)(getResources().getDisplayMetrics().heightPixels*.91f)));
    }
   });
  });
 }

 private void openInventoryCheckpoint(){
  background(()->{
   requireImportedLedger();
   LedgerMeta meta=database.dao().meta();
   final String firstAllowed=meta.openingMonth;
   final InventoryCheckpoint.Config existing=InventoryCheckpoint.read(meta.inventoryStockJson);
   runOnUiThread(()->{
    Dialog dialog=new Dialog(this);LinearLayout form=col();
    form.setBackground(bg(Color.WHITE,24));pad(form,21,22);
    form.addView(text("Confirmed opening shuttle stock",21,DARK,true));gap(form,8);
    form.addView(text("Use a real, verified count at the START of the selected month, before any transactions that month. If only today's count is known, do not backdate it.",12,GRAY,false));gap(form,16);
    if(existing!=null){
     form.addView(text("Currently confirmed: "+existing.openingShuttles+" individual shuttles at start of "+
       existing.month+" · "+existing.perPack+" shuttles/pack. Updating creates an audit record.",12,GREEN,true));
     gap(form,14);
    }
    form.addView(text("OPENING MONTH (YYYY-MM)",11,GRAY,true));gap(form,7);
    EditText month=new EditText(this);month.setSingleLine(true);
    month.setText(existing==null?reportMonth:existing.month);
    month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);form.addView(month);gap(form,14);
    form.addView(text("INDIVIDUAL SHUTTLES AT MONTH START",11,GRAY,true));gap(form,7);
    EditText opening=new EditText(this);opening.setSingleLine(true);
    opening.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    opening.setHint("Count each shuttlecock, not packs");
    if(existing!=null)opening.setText(String.valueOf(existing.openingShuttles));
    opening.setBackground(border(LIGHT,LINE,12));pad(opening,14,12);form.addView(opening);gap(form,14);
    form.addView(text("SHUTTLES PER PACK (CONFIRMED)",11,GRAY,true));gap(form,7);
    EditText perPack=new EditText(this);perPack.setSingleLine(true);
    perPack.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    perPack.setHint("e.g. 12 — enter actual pack size");
    if(existing!=null)perPack.setText(String.valueOf(existing.perPack));
    perPack.setBackground(border(LIGHT,LINE,12));pad(perPack,14,12);form.addView(perPack);gap(form,14);
    form.addView(text("OPTIONAL STOCK-COUNT NOTE",11,GRAY,true));gap(form,7);
    EditText note=new EditText(this);note.setMinLines(2);
    note.setHint("Source of physical count, who checked it");
    if(existing!=null)note.setText(existing.note);
    note.setBackground(border(LIGHT,LINE,12));pad(note,14,12);form.addView(note);gap(form,16);
    form.addView(button("Review confirmed stock checkpoint",()->{
     try{
      final String mm=MigrationCodec.ym(month.getText().toString().trim());
      String today=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());
      if(mm.compareTo(firstAllowed)<0)
       throw new IllegalArgumentException("Checkpoint cannot precede ledger opening "+firstAllowed);
      if(mm.compareTo(today)>0)
       throw new IllegalArgumentException("Cannot confirm a future opening stock count");
      final long count=Long.parseLong(opening.getText().toString().trim());
      final long factor=Long.parseLong(perPack.getText().toString().trim());
      final String details=note.getText().toString().trim();
      final InventoryCheckpoint.Config chosen=InventoryCheckpoint.confirmed(mm,count,factor,details);
      String message="Month-start count: "+count+" individual shuttles"+
       "\nEffective at START of "+mm+
       "\nPack factor: "+factor+" individual shuttles per pack."+
       "\n\nAll recorded purchases, donations and usage in "+mm+" will be counted AFTER this opening amount."+
       "\n\n"+(existing==null?"Create first confirmed checkpoint?":
        "REPLACE existing stock checkpoint? This will recalculate all displayed stock positions from the new basis, without changing transactions.")+
       "\n\nConfirm only if the historical month-start count is known.";
      new AlertDialog.Builder(this).setTitle("Confirm physically checked stock")
       .setMessage(message).setNegativeButton("Keep editing",null)
       .setPositiveButton(existing==null?"Confirm opening stock":"Replace checkpoint",(d,w)->{
        dialog.dismiss();
        background(()->{
         database.runInTransaction(()->{
          LedgerDao dao=database.dao();LedgerMeta live=dao.meta();
          if(live==null)throw new IllegalStateException("Cannot set stock before importing the ledger.");
          for(MonthlyCloseV258.MonthState closed:MonthlyCloseV258.read(live.inventoryStockJson).values()){
           if(closed.closed()&&closed.month.compareTo(mm)>=0)
            throw new IllegalStateException("A closed month depends on this stock baseline. Reopen closed months from "+mm+" onward before changing the checkpoint.");
          }
          PremiumV26StockGuard.requireEditableOpeningCheckpoint(live.inventoryStockJson);
          String updated=InventoryCheckpoint.updatedJson(live.inventoryStockJson,chosen);
          if(dao.updateInventoryJson(updated)!=1)
           throw new IllegalStateException("Inventory metadata update was not applied.");
          AuditEvent audit=new AuditEvent();
          audit.id="audit-stock-"+UUID.randomUUID();
          audit.createdAt=new Date().toString();
          audit.action="Manually confirmed opening stock";
          audit.details="Effective "+mm+"; "+count+" individual shuttles; "+
           factor+" per pack; previous="+(InventoryCheckpoint.read(live.inventoryStockJson)==null?"not set":"replaced")+
           "; note="+details;
          dao.audit(audit);
         });
         BackupGuardian.save(this,database,"confirmed-opening-stock",true);
         warnOnBackupFailure();
         runOnUiThread(()->{Toast.makeText(this,"Stock checkpoint saved",Toast.LENGTH_LONG).show();showExpenses();});
        });
       }).show();
     }catch(Exception ex){
      error(new IllegalArgumentException(ex.getMessage()==null?"Check stock opening values.":ex.getMessage()));
     }
    }));
    ScrollView scroll=new ScrollView(this);scroll.addView(form);
    dialog.setContentView(scroll);dialog.show();Window w=dialog.getWindow();
    if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);
     w.setGravity(Gravity.BOTTOM);w.setLayout(-1,Math.min(dp(740),(int)(getResources().getDisplayMetrics().heightPixels*.91f)));}
   });
  });
 }

 private void openExpense(){
  background(()->{
   requireImportedLedger();
   runOnUiThread(this::openExpenseForm);
  });
 }
 private void openExpenseForm(){
  Dialog dialog=new Dialog(this);LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,22,24);
  form.addView(text("New shuttle purchase",22,DARK,true));gap(form,7);
  form.addView(text("Records a cash expense and the number of packs purchased.",12,GRAY,false));gap(form,18);
  form.addView(text("MONTH (YYYY-MM)",11,GRAY,true));gap(form,7);
  EditText month=new EditText(this);month.setText(reportMonth);month.setSingleLine(true);
  month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);form.addView(month);gap(form,15);
  form.addView(text("DATE (OPTIONAL)",11,GRAY,true));gap(form,7);
  EditText day=new EditText(this);day.setHint("YYYY-MM-DD · leave blank if unknown");
  day.setSingleLine(true);day.setBackground(border(LIGHT,LINE,12));pad(day,14,12);form.addView(day);gap(form,14);
  form.addView(text("NUMBER OF PACKS",11,GRAY,true));gap(form,7);
  EditText count=new EditText(this);count.setInputType(2);count.setText("1");
  count.setBackground(border(LIGHT,LINE,12));pad(count,14,12);form.addView(count);gap(form,14);
  form.addView(text("SHUTTLES PER PACK (OPTIONAL IF UNCHANGED)",11,GRAY,true));gap(form,7);
  EditText packSize=new EditText(this);packSize.setInputType(2);
  packSize.setHint("Actual size 1–60; blank uses confirmed base size");
  packSize.setBackground(border(LIGHT,LINE,12));pad(packSize,14,12);form.addView(packSize);gap(form,15);
  form.addView(text("PRICE PER PACK",11,GRAY,true));gap(form,7);
  EditText price=new EditText(this);price.setInputType(2);price.setHint("Actual price paid per pack");
  price.setBackground(border(LIGHT,LINE,12));pad(price,14,12);form.addView(price);gap(form,8);
  LinearLayout presets=row();
  for(int q:new int[]{3200,3500,4000}){
   TextView chip=pill("₹"+q,GREEN,PALE);presets.addView(chip,new LinearLayout.LayoutParams(0,-2,1));
   pressable(chip,()->price.setText(String.valueOf(q)));
  }
  form.addView(presets);gap(form,17);
  form.addView(button("Review purchase",()->{
   try{
    final String mm=MigrationCodec.ym(month.getText().toString().trim());
    final String dd=checkedPurchaseDate(day.getText().toString(),mm);
    final long qty=Long.parseLong(count.getText().toString().trim());
    final long unit=Long.parseLong(price.getText().toString().trim());
    if(qty<=0||unit<=0)throw new NumberFormatException();
    final long total=Math.multiplyExact(qty,unit);
    final String sizeText=packSize.getText().toString().trim();
    final long explicitSize=sizeText.isEmpty()?0:Long.parseLong(sizeText);
    if(explicitSize!=0&&(explicitSize<1||explicitSize>60))
     throw new IllegalArgumentException("Actual pack size must be 1–60.");
    String summary="Record "+qty+" pack(s) at "+rupee(unit)+" each in "+mm+
     "\nPack factor: "+(explicitSize==0?"confirmed baseline size":"explicit "+explicitSize+" shuttles per pack")+
     "\nTotal cash expense: "+rupee(total)+
     "\nPurchase date: "+(dd.isEmpty()?"not recorded":dd)+
     "\n\nConfirm this new purchase? Existing expenses are not replaced.";
    new AlertDialog.Builder(this).setTitle("Confirm shuttle purchase")
     .setMessage(summary).setNegativeButton("Keep editing",null)
     .setPositiveButton("Record purchase",(d,w)->{
      dialog.dismiss();
      background(()->{
       Expense e=new Expense();e.id="expense-"+UUID.randomUUID();e.month=mm;
       e.category="Shuttle pack";e.date=dd;e.quantity=qty;e.amount=total;
       if(explicitSize>0)e.note=StockReconciliationV257.packNote(explicitSize,"Original purchase pack size");
       database.runInTransaction(()->{
        requireOpenMonth(mm);
        requireEditableStockMonth(mm);
        database.dao().insertExpense(e);
        AuditEvent a=new AuditEvent();a.id="audit-"+UUID.randomUUID();
        a.createdAt=new Date().toString();a.action="Shuttle purchase recorded";
        a.details=e.id+" / "+qty+" pack(s) / "+rupee(total)+" / "+mm;
        database.dao().audit(a);
       });
       BackupGuardian.save(this,database,"shuttle-purchase",true);
       warnOnBackupFailure();
       runOnUiThread(()->{Toast.makeText(this,"Shuttle purchase recorded",Toast.LENGTH_LONG).show();showExpenses();});
      });
     }).show();
   }catch(Exception ex){error(new IllegalArgumentException("Check the month, optional date, pack quantity and positive unit price."));}
  }));
  ScrollView sv=new ScrollView(this);sv.addView(form);dialog.setContentView(sv);dialog.show();
  Window w=dialog.getWindow();
  if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}
 }
 private void openShuttleDonation(){
  background(()->{
   requireImportedLedger();
   List<Member> players=database.dao().members();
   runOnUiThread(()->{
    if(players.isEmpty()){error(new IllegalStateException("Import or add members before recording a donation."));return;}
    Dialog dialog=new Dialog(this);LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,22,24);
    form.addView(text("Record donated shuttle packs",21,DARK,true));gap(form,8);
    form.addView(text("A noncash entry. No collection, expenditure or closing balance is changed.",12,GRAY,false));gap(form,16);
    form.addView(text("MEMBER",11,GRAY,true));gap(form,7);
    AutoCompleteTextView who=new AutoCompleteTextView(this);who.setSingleLine(true);who.setThreshold(1);
    String[] options=new String[players.size()];for(int i=0;i<players.size();i++)options[i]=players.get(i).name;
    who.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,options));
    who.setHint("Select contributing member");who.setBackground(border(LIGHT,LINE,12));pad(who,14,12);form.addView(who);gap(form,15);
    form.addView(text("MONTH (YYYY-MM)",11,GRAY,true));gap(form,7);
    EditText month=new EditText(this);month.setText(reportMonth);month.setSingleLine(true);
    month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);form.addView(month);gap(form,14);
    form.addView(text("DONATED PACKS",11,GRAY,true));gap(form,7);
    EditText quantity=new EditText(this);quantity.setText("1");quantity.setInputType(2);
    quantity.setBackground(border(LIGHT,LINE,12));pad(quantity,14,12);form.addView(quantity);gap(form,14);
    form.addView(text("SHUTTLES PER DONATED PACK (OPTIONAL)",11,GRAY,true));gap(form,7);
    EditText packSize=new EditText(this);packSize.setInputType(2);
    packSize.setHint("Actual size 1–60; blank uses confirmed base size");
    packSize.setBackground(border(LIGHT,LINE,12));pad(packSize,14,12);form.addView(packSize);gap(form,14);
    form.addView(text("DATE (OPTIONAL)",11,GRAY,true));gap(form,7);
    EditText day=new EditText(this);day.setHint("YYYY-MM-DD · leave blank if unknown");day.setSingleLine(true);
    day.setBackground(border(LIGHT,LINE,12));pad(day,14,12);form.addView(day);gap(form,16);
    form.addView(button("Review noncash donation",()->{
     try{
      String entered=who.getText().toString().trim();
      Member chosen=null;for(Member person:players)if(person.name.equalsIgnoreCase(entered)){chosen=person;break;}
      if(chosen==null){who.setError("Choose an existing member");return;}
      final Member donor=chosen;
      final String mm=MigrationCodec.ym(month.getText().toString().trim());
      final String dd=checkedPurchaseDate(day.getText().toString(),mm);
      final long qty=Long.parseLong(quantity.getText().toString().trim());
      if(qty<=0||qty>100)throw new IllegalArgumentException("Enter 1–100 donated packs.");
      final String sizeText=packSize.getText().toString().trim();
      final long explicitSize=sizeText.isEmpty()?0:Long.parseLong(sizeText);
      if(explicitSize!=0&&(explicitSize<1||explicitSize>60))
       throw new IllegalArgumentException("Pack size must be 1–60 shuttles.");
      // Inspect existing donations before adding another: warns, never silently
      // merges or deletes a legitimate historical record.
      background(()->{
       int previousEntries=0;long previousPacks=0;
       for(Contribution existing:database.dao().contributions()){
        if(donor.id.equals(existing.memberId)&&mm.equals(existing.feeMonth)&&"packs".equals(existing.kind)){
         previousPacks+=existing.amount;previousEntries++;
        }
       }
       for(LegacyEntry historical:database.dao().legacy()){
        if(donor.id.equals(historical.memberId)&&mm.equals(historical.period)&&"packs".equals(historical.kind)){
         previousPacks+=historical.amount;previousEntries++;
        }
       }
       boolean historicalControl=false;
       for(MonthlyControl control:database.dao().controls())
        if(mm.equals(control.month)){historicalControl=true;break;}
       final int existingCount=previousEntries;
       final long existingPacks=previousPacks;
       final boolean controlledMonth=historicalControl;
       runOnUiThread(()->{
        String warning=existingCount>0?
         "\n\nPOSSIBLE DUPLICATE: "+existingCount+" existing donated-pack entry/entries total "+existingPacks+
         " pack(s) for this member and month. Add another only if a separate donation occurred.":"";
        if(controlledMonth)
         warning+="\n\nHISTORICAL CONTROL: Earlier monthly reconciliation remains unchanged. The new noncash entry does not erase past differences.";
        new AlertDialog.Builder(this).setTitle(existingCount>0?"Review possible duplicate donation":"Confirm noncash donation")
         .setMessage(donor.name+" · "+qty+" donated pack(s) · "+mm+
        "\nPack factor: "+(explicitSize==0?"confirmed baseline size":explicitSize+" shuttles per pack")+
          "\nDate: "+(dd.isEmpty()?"not recorded":dd)+warning+
          "\n\nCash collection, expenditure and closing balance remain unchanged.")
         .setNegativeButton("Keep editing",null)
         .setPositiveButton(existingCount>0?"Add another donation":"Record donated packs",(d,w)->{
          dialog.dismiss();
          background(()->{
           Contribution c=new Contribution();
           c.id="donated-pack-"+UUID.randomUUID();c.memberId=donor.id;c.feeMonth=mm;
           c.receivedDate=dd.isEmpty()?mm+"-01":dd;c.approximate=dd.isEmpty();
           c.historical=false;c.kind="packs";c.amount=qty;
           c.method="Donated shuttle packs";
           c.note=dd.isEmpty()?"Month-only donated packs; exact day unknown.":"Member donated packs on recorded day.";
         if(explicitSize>0)c.note=StockReconciliationV257.packNote(explicitSize,c.note);
           database.runInTransaction(()->{
            requireOpenMonth(mm);
            requireEditableStockMonth(mm);
            database.dao().insertPayment(c);
            AuditEvent a=new AuditEvent();a.id="audit-"+UUID.randomUUID();
            a.createdAt=new Date().toString();a.action="Noncash shuttle donation recorded";
            a.details=c.id+" / "+donor.name+" / "+qty+" donated pack(s) / "+mm+" / no cash adjustment";
            database.dao().audit(a);
           });
           BackupGuardian.save(this,database,"shuttle-donation",true);warnOnBackupFailure();
           runOnUiThread(()->{Toast.makeText(this,"Noncash shuttle donation recorded",Toast.LENGTH_LONG).show();showExpenses();});
          });
         }).show();
       });
      });
     }catch(Exception ex){error(new IllegalArgumentException("Check member, month, donated-pack quantity and optional date."));}
    }));
    ScrollView scroll=new ScrollView(this);scroll.addView(form);dialog.setContentView(scroll);dialog.show();
    Window w=dialog.getWindow();
    if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}
   });
  });
 }

 private void openShuttleUsage(){
  background(()->{
   requireImportedLedger();
   runOnUiThread(this::openShuttleUsageForm);
  });
 }
 private void openShuttleUsageForm(){
  Dialog dialog=new Dialog(this);
  LinearLayout form=col();form.setBackground(bg(Color.WHITE,24));pad(form,22,24);
  form.addView(text("Record shuttle usage",21,DARK,true));gap(form,8);
  form.addView(text("Count individual shuttles used, not packs. This does not change club cash balances.",12,GRAY,false));gap(form,17);
  form.addView(text("MONTH (YYYY-MM)",11,GRAY,true));gap(form,7);
  EditText month=new EditText(this);month.setText(reportMonth);month.setSingleLine(true);
  month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);form.addView(month);gap(form,14);
  form.addView(text("ACTUAL USAGE DATE (OPTIONAL)",11,GRAY,true));gap(form,7);
  EditText date=new EditText(this);date.setSingleLine(true);
  date.setHint("YYYY-MM-DD · leave blank if unknown");
  date.setBackground(border(LIGHT,LINE,12));pad(date,14,12);form.addView(date);gap(form,14);
  form.addView(text("NUMBER OF INDIVIDUAL SHUTTLES",11,GRAY,true));gap(form,7);
  EditText count=new EditText(this);count.setInputType(2);count.setText("1");
  count.setBackground(border(LIGHT,LINE,12));pad(count,14,12);form.addView(count);gap(form,7);
  LinearLayout shortcuts=row();
  for(int q:new int[]{1,6,12}){
   TextView chip=pill(q+" shuttle(s)",GREEN,PALE);
   shortcuts.addView(chip,new LinearLayout.LayoutParams(0,-2,1));
   pressable(chip,()->count.setText(String.valueOf(q)));
  }
  form.addView(shortcuts);gap(form,14);
  form.addView(text("USAGE CATEGORY",11,GRAY,true));gap(form,7);
  Spinner lossCategory=new Spinner(this);
  lossCategory.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
   new String[]{"Consumed in play","Damaged shuttles","Lost shuttles","Other documented use"}));
  form.addView(lossCategory);gap(form,12);
  form.addView(text("OPTIONAL NOTE",11,GRAY,true));gap(form,7);
  EditText note=new EditText(this);note.setSingleLine(false);note.setMinLines(2);
  note.setHint("e.g. weekend practice");
  note.setBackground(border(LIGHT,LINE,12));pad(note,14,12);form.addView(note);gap(form,17);
  form.addView(button("Review usage entry",()->{
   try{
    final String mm=MigrationCodec.ym(month.getText().toString().trim());
    final String actual=checkedPurchaseDate(date.getText().toString(),mm);
    final long qty=Long.parseLong(count.getText().toString().trim());
    if(qty<1||qty>1000)throw new IllegalArgumentException("Enter 1 to 1,000 shuttles.");
    final String details=lossCategory.getSelectedItem().toString()+" · "+note.getText().toString().trim();
    if(details.length()>200)throw new IllegalArgumentException("Note must be 200 characters or fewer.");
    String summary=qty+" individual shuttles used in "+mm
     +"\nActual date: "+(actual.isEmpty()?"not recorded":actual)
     +"\nNote: "+(details.isEmpty()?"none":details)
     +"\n\nA new usage entry will be added. Cash balance is not affected.";
    new AlertDialog.Builder(this).setTitle("Confirm shuttle usage")
     .setMessage(summary).setNegativeButton("Keep editing",null)
     .setPositiveButton("Record usage",(d,w)->{
      dialog.dismiss();
      background(()->{
       StockUsage entry=new StockUsage();
       entry.id="usage-"+UUID.randomUUID();entry.month=mm;
       entry.date=actual;entry.shuttles=qty;entry.note=details;
       database.runInTransaction(()->{
        requireImportedLedger();requireOpenMonth(mm);
        requireEditableStockMonth(mm);
        database.dao().insertStock(Collections.singletonList(entry));
        AuditEvent audit=new AuditEvent();audit.id="audit-"+UUID.randomUUID();
        audit.createdAt=new Date().toString();audit.action="Shuttle usage recorded";
        audit.details=entry.id+" / "+qty+" individual shuttles / "+mm;
        database.dao().audit(audit);
       });
       BackupGuardian.save(this,database,"shuttle-usage",true);
       warnOnBackupFailure();
       runOnUiThread(()->{
        Toast.makeText(this,"Shuttle usage saved",Toast.LENGTH_LONG).show();
        showExpenses();
       });
      });
     }).show();
   }catch(Exception ex){
    error(new IllegalArgumentException("Check month, optional date, quantity (1-1000) and note (up to 200 characters)."));
   }
  }));
  ScrollView scroll=new ScrollView(this);scroll.addView(form);
  dialog.setContentView(scroll);dialog.show();
  Window w=dialog.getWindow();
  if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);
   w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}
 }

 private static boolean historicalDonationRecorded(LedgerDao dao){
  for(LegacyEntry item:dao.legacy())if("p11".equals(item.memberId)&&"2026-02".equals(item.period)&&"packs".equals(item.kind)&&item.amount>0)return true;
  for(Contribution item:dao.contributions())if("p11".equals(item.memberId)&&"2026-02".equals(item.feeMonth)&&"packs".equals(item.kind)&&item.amount>0)return true;
  return false;
 }
 private static Member confirmedHistoricalMember(LedgerDao dao){
  for(Member member:dao.members())if("p11".equals(member.id)){
   if(!"kanishk".equalsIgnoreCase(member.name.trim()))
    throw new IllegalStateException("The imported member identity differs from the verified February ledger. No correction was made.");
   return member;
  }
  throw new IllegalStateException("The documented February member was not found. No correction was made.");
 }
 private void restoreHistoricalPack(){
  background(()->{
   LedgerDao dao=database.dao();
   if(dao.meta()==null)throw new IllegalStateException("Import your verified ledger first");
   Member person=confirmedHistoricalMember(dao);
   if(historicalDonationRecorded(dao)){
    runOnUiThread(()->Toast.makeText(this,"The February 2026 donated pack is already recorded; nothing was changed.",Toast.LENGTH_LONG).show());
    return;
   }
   runOnUiThread(()->new AlertDialog.Builder(this)
    .setTitle("Confirm documented February shuttle donation")
    .setMessage(person.name+" donated one shuttle pack in February 2026 according to the archived ledger. It is absent from the imported account.\n\nAdd +1 noncash pack only? No cash collection, expenditure or closing balance will be modified. The date is recorded as approximate, and an audit entry will be created.")
    .setNegativeButton("Cancel",null)
    .setPositiveButton("Add one donated pack",(d,w)->applyHistoricalPack())
    .show());
  });
 }
 private void applyHistoricalPack(){
  background(()->{
   final boolean[] restored={false};
   database.runInTransaction(()->{
    LedgerDao dao=database.dao();
    if(dao.meta()==null)throw new IllegalStateException("No imported ledger found");
    requireOpenMonth("2026-02");
    requireEditableStockMonth("2026-02");
    confirmedHistoricalMember(dao);
    if(historicalDonationRecorded(dao))return;
    Contribution donation=new Contribution();
    donation.id="historic-2026-02-p11-donation-1";
    donation.memberId="p11";donation.feeMonth="2026-02";
    donation.receivedDate="2026-02-01";donation.approximate=true;
    donation.kind="packs";donation.amount=1;donation.historical=true;
    donation.method="Historical donated pack";
    donation.note="Exact day unknown; noncash donation verified against archived February 2026 ledger.";
    dao.insertPayment(donation);
    AuditEvent event=new AuditEvent();event.id="audit-"+java.util.UUID.randomUUID();
    event.action="historic_noncash_restoration";
    event.createdAt=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.ROOT).format(new Date());
    event.details="Restored one missing February 2026 noncash donated shuttle pack for member p11. Cash unchanged.";
    dao.audit(event);restored[0]=true;
   });
   if(restored[0]){BackupGuardian.save(this,database,"february-donation",true);warnOnBackupFailure();}
   runOnUiThread(()->{
    Toast.makeText(this,restored[0]?"February donated pack recorded. Cash balances unchanged.":"Donation already exists; no changes made.",Toast.LENGTH_LONG).show();
    if(tab.equals("Reports"))showReports();
   });
  });
 }

 private void openMonthEndReview(String chosenMonth){
  background(()->{
   requireImportedLedger();
   String month=MigrationCodec.ym(chosenMonth);
   LedgerDao dao=database.dao();
   LedgerMeta meta=dao.meta();
   ReportMath.Result row=null;
   for(ReportMath.Result item:ReportMath.monthly(dao,month))if(month.equals(item.month)){row=item;break;}
   if(row==null)throw new IllegalArgumentException("The month is earlier than ledger opening.");
   final ReportMath.Result summary=row;
   final MonthlyCloseV258.MonthState state=MonthlyCloseV258.read(meta.inventoryStockJson).get(month);
   final boolean closed=state!=null&&state.closed();
   List<Contribution> feePayments=dao.contributions();
   List<PremiumV26State.FeePolicy> monthPolicies=PremiumV26State.policies(
    meta.inventoryStockJson,knownMembers());
   int feeConfigured=0,feeUnknown=0,feeUnpaid=0;
   long billedUnpaid=0;
   for(Member member:dao.members()){
    long rate=PremiumV26State.rateFor(monthPolicies,member.id,month);
    if(rate<0){feeUnknown++;continue;}
    feeConfigured++;
    PremiumV26State.FeeSummary dues=PremiumV26State.feeSummary(
     meta.inventoryStockJson,member.id,month,month,feePayments);
    if(dues.arrears>0){feeUnpaid++;billedUnpaid=Math.addExact(billedUnpaid,dues.arrears);}
   }
   final int configuredCount=feeConfigured,unconfiguredCount=feeUnknown,unpaidMembers=feeUnpaid;
   final long unpaidAmount=billedUnpaid;
   runOnUiThread(()->{
    Dialog dialog=new Dialog(this);
    LinearLayout body=col();body.setBackground(bg(Color.WHITE,24));pad(body,21,20);
    body.addView(text("Month-end financial review",21,DARK,true));gap(body,7);
    body.addView(text(month+" · "+(closed?"CLOSED / AUDITED":"OPEN / NOT CLOSED"),13,GREEN,true));gap(body,14);
    body.addView(text("Opening balance: "+rupee(summary.opening),14,DARK,false));gap(body,8);
    body.addView(text("Official collections: "+rupee(summary.collections),14,DARK,false));gap(body,8);
    body.addView(text("Member cash entries: "+rupee(summary.memberEntries),14,DARK,false));gap(body,8);
    body.addView(text("Expenses: "+rupee(summary.expenses),14,DARK,false));gap(body,8);
    body.addView(text("Closing balance: "+rupee(summary.closing),18,GREEN,true));gap(body,10);
    body.addView(text("FEE REGISTER PRE-CLOSE REVIEW",11,GREEN,true));gap(body,6);
    body.addView(text("Fee rules available for "+configuredCount+" member(s); "+
      unconfiguredCount+" member(s) have no configured fee.",12,GRAY,false));gap(body,6);
    body.addView(text("Unpaid or partly paid: "+unpaidMembers+" member(s) · "+
      rupee(unpaidAmount)+" in assessed dues. Unpaid dues are NOT cash received.",
      12,unpaidMembers>0?0xFF9F6B18:GRAY,true));gap(body,11);
    body.addView(text(summary.difference==0?"Official/member sums reconcile.":
     "UNRESOLVED DIFFERENCE: "+rupee(summary.difference)+
      ". Closing records this outstanding difference; it does NOT resolve it.",
     12,summary.difference==0?GRAY:0xFF9F383A,true));gap(body,14);
    body.addView(button("Share A4 landscape review",()->{
     dialog.dismiss();
     sharePdf("Month_End_Review_"+month+".pdf",
      ()->MonthlyReviewPdfV258.make(this,database.dao(),month));
    }));gap(body,8);
    body.addView(button("Save A4 landscape review PDF",()->{
     dialog.dismiss();
     savePdf("Month_End_Review_"+month+".pdf",
      ()->MonthlyReviewPdfV258.make(this,database.dao(),month));
    }));gap(body,8);
    String today=new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(new Date());
    if(closed){
     body.addView(button("Reopen this month with an audit reason",()->{
      dialog.dismiss();
      askMonthCloseReason(month,"REOPEN",summary);
     }));
    }else if(month.compareTo(today)<0){
     body.addView(button("Close completed month with audit",()->{
      dialog.dismiss();
      askMonthCloseReason(month,"CLOSE",summary);
     }));
    }else{
     body.addView(text("Only completed calendar months may be formally closed. The current month remains open.",12,GRAY,false));
    }
    gap(body,10);
    body.addView(text("Closing prevents new payments, refunds, purchase changes and recorded stock entries for this month until it is reopened. Original ledger history remains intact.",11,GRAY,false));gap(body,14);
    body.addView(inlineButton("Back to Reports",GREEN,dialog::dismiss));
    ScrollView scroll=new ScrollView(this);scroll.addView(body);
    dialog.setContentView(scroll);dialog.show();
    Window window=dialog.getWindow();
    if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);
     window.setGravity(Gravity.BOTTOM);
     window.setLayout(-1,Math.min(dp(730),(int)(getResources().getDisplayMetrics().heightPixels*.90f)));}
   });
  });
 }
 private void askMonthCloseReason(String month,String action,ReportMath.Result reviewed){
  EditText note=new EditText(this);note.setMinLines(2);
  note.setHint("Required: reason, reviewer, unresolved difference if any");
  new AlertDialog.Builder(this).setTitle("CLOSE".equals(action)?"Confirm monthly closing":"Audited monthly reopening")
   .setMessage("Month "+month+" · Closing "+rupee(reviewed.closing)+
     "\nUnresolved difference "+rupee(reviewed.difference)+
     "\n\nAn audit event will preserve this action and the reviewed totals. Enter a meaningful reason.")
   .setView(note).setNegativeButton("Cancel",null)
   .setPositiveButton("Confirm "+action,(d,w)->{
    String reason=note.getText().toString().trim();
    if(reason.length()<8||reason.length()>200){
     error(new IllegalArgumentException("Enter an audit reason of 8-200 characters. No month status changed."));return;
    }
    background(()->{
     database.runInTransaction(()->{
      LedgerDao dao=database.dao();LedgerMeta live=dao.meta();
      if(live==null)throw new IllegalStateException("No imported ledger");
      ReportMath.Result now=null;
      for(ReportMath.Result item:ReportMath.monthly(dao,month))if(month.equals(item.month)){now=item;break;}
      if(now==null||now.opening!=reviewed.opening||now.collections!=reviewed.collections||
         now.memberEntries!=reviewed.memberEntries||now.expenses!=reviewed.expenses||
         now.closing!=reviewed.closing||now.difference!=reviewed.difference)
       throw new IllegalStateException("Monthly finances changed during review. Reopen the review to verify fresh totals.");
      String id="month-close-"+UUID.randomUUID(),at=new Date().toString();
      String updated=MonthlyCloseV258.record(live.inventoryStockJson,month,action,reason,id,at,now);
      if(dao.updateInventoryJson(updated)!=1)
       throw new IllegalStateException("Month-close event could not be stored");
      AuditEvent audit=new AuditEvent();audit.id=id;audit.createdAt=at;
      audit.action="Month "+month+" "+action;
      audit.details="Opening "+now.opening+"; receipts "+now.collections+"; expenditure "+
       now.expenses+"; closing "+now.closing+"; difference "+now.difference+"; reason "+reason;
      dao.audit(audit);
     });
     BackupGuardian.save(this,database,"month-"+action.toLowerCase(Locale.ROOT),true);
     warnOnBackupFailure();
     runOnUiThread(()->{
      Toast.makeText(this,"Month "+month+" "+("CLOSE".equals(action)?"closed":"reopened"),Toast.LENGTH_LONG).show();
      openMonthEndReview(month);
     });
    });
   }).show();
 }


 private Set<String> knownMembers(){
  HashSet<String> ids=new HashSet<>();
  for(Member m:database.dao().members())ids.add(m.id);
  return ids;
 }
 private String effectiveName(Member m,Map<String,PremiumV26State.Profile> profiles){
  PremiumV26State.Profile p=profiles.get(m.id);
  return p==null?m.name:p.name;
 }
 private void commitV26Metadata(String action,String affectedMonth,java.util.function.Function<String,String> edit){
  background(()->{
   database.runInTransaction(()->{
    requireImportedLedger();
    LedgerDao dao=database.dao();LedgerMeta meta=dao.meta();
    if(affectedMonth!=null){
     String chosen=MigrationCodec.ym(affectedMonth);
     MonthlyCloseV258.requireOpen(meta.inventoryStockJson,chosen);
     // A backdated policy could revalue later closed periods. Never allow it.
     for(MonthlyCloseV258.MonthState closed:MonthlyCloseV258.read(meta.inventoryStockJson).values())
      if(closed.closed()&&closed.month.compareTo(chosen)>=0)
       throw new IllegalStateException("Audited month "+closed.month+" is closed. Reopen affected periods before backdating a financial rule.");
    }
    String next=edit.apply(meta.inventoryStockJson);
    if(next.equals(meta.inventoryStockJson))throw new IllegalArgumentException("No metadata change");
    if(dao.updateInventoryJson(next)!=1)throw new IllegalStateException("Metadata write failed");
    AuditEvent audit=new AuditEvent();audit.id="audit-v26-"+UUID.randomUUID();
    audit.createdAt=new Date().toString();audit.action=action;
    audit.details="Audited v26 metadata event. Effective month: "+(affectedMonth==null?"not applicable":affectedMonth);
    dao.audit(audit);
   });
   BackupGuardian.save(this,database,"v26-"+action.toLowerCase(Locale.ROOT).replace(' ','-'),true);
   warnOnBackupFailure();
   runOnUiThread(()->{
    Toast.makeText(this,"Saved with audit history",Toast.LENGTH_LONG).show();
    if(tab.equals("Members"))showMembers();else if(tab.equals("Reports"))showReports();
   });
  });
 }
 private void showFeeCentre(){
  background(()->{
   requireImportedLedger();
   LedgerDao dao=database.dao();LedgerMeta meta=dao.meta();
   List<Member> members=dao.members();List<Contribution> payments=dao.contributions();
   Set<String> known=knownMembers();
   Map<String,PremiumV26State.Profile> feeProfiles=PremiumV26State.profiles(meta.inventoryStockJson,known);
   List<PremiumV26State.FeePolicy> rules=PremiumV26State.policies(meta.inventoryStockJson,known);
   List<PremiumV26State.Waiver> waivers=PremiumV26State.waivers(meta.inventoryStockJson,knownMembers());
   String month=reportMonth;
   runOnUiThread(()->{
    LinearLayout view=col();pad(view,20,20);
    view.addView(text("Fees, dues & arrears",22,DARK,true));gap(view,7);
    view.addView(text("Cash received and amounts due are calculated separately. Past receipts are never rewritten.",12,GRAY,false));gap(view,14);
    view.addView(text("Fee rules: "+rules.size()+"  ·  Concessions: "+waivers.size()+"  ·  Review through "+month,12,GREEN,true));gap(view,13);
    view.addView(button("Set monthly fee (effective dated)",()->editFeePolicy(members)));gap(view,9);
    view.addView(button("Record authorized fee concession",()->editFeeWaiver(members)));gap(view,17);
    for(Member member:members){
     PremiumV26State.FeeSummary f=PremiumV26State.feeSummary(meta.inventoryStockJson,
      member.id,"2026-01",month,payments);
     String name=effectiveName(member,feeProfiles);
     long rate=PremiumV26State.rateFor(rules,member.id,month);
     LinearLayout item=col();
     item.addView(text(name,14,DARK,true));gap(item,5);
     item.addView(text("Current fee: "+(rate<0?"not configured":rupee(rate))+
      "  ·  Outstanding across assigned periods: "+rupee(f.arrears),12,GREEN,true));gap(item,4);
     item.addView(text("Cash allocated: "+rupee(f.paid)+"  ·  Concessions: "+rupee(f.waived)+
      "  ·  Excess assigned: "+rupee(f.excess)+
      (f.unconfiguredMonths>0?"  ·  Unconfigured: "+f.unconfiguredMonths+" mo":""),
      11,GRAY,false));
     panel(view,item);
    }
    view.addView(text("Unpaid fees are obligations only, not incoming cash. Excess paid against one month is not silently reassigned to another.",11,GRAY,false));gap(view,10);
    ScrollView sc=new ScrollView(this);sc.addView(view);
    new AlertDialog.Builder(this).setView(sc).setPositiveButton("Close",null).show();
   });
  });
 }
 private void editFeePolicy(List<Member> members){
  LinearLayout f=col();pad(f,16,8);
  Spinner scope=new Spinner(this);ArrayList<String> names=new ArrayList<>();names.add("All members");
  for(Member m:members)names.add(m.name+"  ["+m.id+"]");
  scope.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
  f.addView(text("APPLIES TO",11,GRAY,true));f.addView(scope);
  EditText rate=new EditText(this);rate.setInputType(2);rate.setHint("Monthly amount in INR");f.addView(rate);
  EditText month=new EditText(this);month.setText(reportMonth);month.setHint("Effective YYYY-MM");f.addView(month);
  EditText reason=new EditText(this);reason.setHint("Required reason, 8–200 characters");f.addView(reason);
  new AlertDialog.Builder(this).setTitle("Audited fee policy")
   .setView(f).setNegativeButton("Cancel",null).setPositiveButton("Review", (d,w)->{
    try{
     String scopeId=scope.getSelectedItemPosition()==0?"ALL":members.get(scope.getSelectedItemPosition()-1).id;
     String mm=MigrationCodec.ym(month.getText().toString().trim());
     long amount=Long.parseLong(rate.getText().toString().trim());
     String why=reason.getText().toString().trim();
     if(amount<0||amount>100000000L||why.length()<8||why.length()>200)throw new IllegalArgumentException("Invalid policy amount or audit reason");
     new AlertDialog.Builder(this).setTitle("Confirm effective-dated fee")
      .setMessage("Scope: "+(scopeId.equals("ALL")?"All members":members.get(scope.getSelectedItemPosition()-1).name)+
       "\nEffective from: "+mm+"\nFee per month: "+rupee(amount)+
       "\nReason: "+why+"\n\nOriginal receipts and cash balances are unchanged.")
      .setNegativeButton("Cancel",null).setPositiveButton("Save policy",(dd,ww)->
       commitV26Metadata("Fee policy",mm,raw->PremiumV26State.addPolicy(
        raw,scopeId,mm,amount,why,"fee-"+UUID.randomUUID(),new Date().toString(),knownMembers())))
      .show();
    }catch(Exception ex){error(new IllegalArgumentException("Check fee, effective YYYY-MM and reason."));}
   }).show();
 }
 private void editFeeWaiver(List<Member> members){
  if(members.isEmpty())return;
  LinearLayout f=col();pad(f,16,8);
  Spinner player=new Spinner(this);ArrayList<String> labels=new ArrayList<>();
  for(Member m:members)labels.add(m.name);
  player.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));f.addView(player);
  EditText month=new EditText(this);month.setText(reportMonth);month.setHint("Fee month YYYY-MM");f.addView(month);
  EditText amount=new EditText(this);amount.setInputType(2);amount.setHint("Concession INR");f.addView(amount);
  EditText reason=new EditText(this);reason.setHint("Authorized concession reason 8–200 chars");f.addView(reason);
  new AlertDialog.Builder(this).setTitle("Record authorized noncash concession")
   .setView(f).setNegativeButton("Cancel",null).setPositiveButton("Review",(d,w)->{
    try{
     String id=members.get(player.getSelectedItemPosition()).id;
     String mm=MigrationCodec.ym(month.getText().toString().trim());
     long amt=Long.parseLong(amount.getText().toString().trim());
     String why=reason.getText().toString().trim();
     if(amt<=0||why.length()<8||why.length()>200)throw new IllegalArgumentException("Invalid concession");
     new AlertDialog.Builder(this).setTitle("Confirm concession, not a cash receipt")
      .setMessage(members.get(player.getSelectedItemPosition()).name+
       "\nFee month "+mm+"\nConcession "+rupee(amt)+"\n"+why+
       "\n\nNo cash transaction will be created.")
      .setNegativeButton("Cancel",null).setPositiveButton("Record concession",(dd,ww)->
       commitV26Metadata("Fee concession",mm,raw->{
        List<PremiumV26State.FeePolicy> policies=PremiumV26State.policies(raw,knownMembers());
        long due=PremiumV26State.rateFor(policies,id,mm),already=0;
        for(PremiumV26State.Waiver x:PremiumV26State.waivers(raw,knownMembers()))
         if(x.member.equals(id)&&x.month.equals(mm))already=Math.addExact(already,x.amount);
        if(due<0||Math.addExact(already,amt)>due)
         throw new IllegalArgumentException("Fee must be configured and remaining concession cannot exceed its rate");
        return PremiumV26State.addWaiver(raw,id,mm,amt,why,"waiver-"+UUID.randomUUID(),new Date().toString(),knownMembers());
       }))
      .show();
    }catch(Exception ex){error(new IllegalArgumentException("Check concession, month and reason."));}
   }).show();
 }
 private void openMemberProfile(Member member){
  background(()->{
   requireImportedLedger();
   String raw=database.dao().meta().inventoryStockJson;
   PremiumV26State.Profile p=PremiumV26State.profiles(raw,knownMembers()).get(member.id);
   runOnUiThread(()->{
    LinearLayout v=col();pad(v,16,8);
    EditText name=new EditText(this);name.setText(p==null?member.name:p.name);
    EditText contact=new EditText(this);contact.setHint("Optional contact");contact.setText(p==null?"":p.contact);
    EditText joined=new EditText(this);joined.setHint("Joining date YYYY-MM-DD");joined.setText(p==null?"":p.joined);
    EditText notes=new EditText(this);notes.setHint("Optional notes");notes.setText(p==null?"":p.notes);
    CheckBox active=new CheckBox(this);active.setText("Active club member");active.setChecked(p==null||p.active);
    v.addView(text("DISPLAY NAME",11,GRAY,true));v.addView(name);
    v.addView(text("CONTACT",11,GRAY,true));v.addView(contact);
    v.addView(text("JOINED",11,GRAY,true));v.addView(joined);
    v.addView(text("NOTES",11,GRAY,true));v.addView(notes);v.addView(active);
    v.addView(text("Historical member ID and original receipts remain immutable. Archiving takes effect this calendar month; earlier assessed fees stay recorded. Archived members are excluded from new collection choices.",12,GRAY,false));
    new AlertDialog.Builder(this).setTitle("Member profile · audited")
     .setView(v).setNegativeButton("Cancel",null).setPositiveButton("Review",(d,w)->{
      String display=name.getText().toString().trim(),phone=contact.getText().toString().trim(),
       joinedAt=joined.getText().toString().trim(),note=notes.getText().toString().trim();
      boolean enabled=active.isChecked();
      new AlertDialog.Builder(this).setTitle("Save member profile?")
       .setMessage("Original ID: "+member.id+"\nDisplay name: "+display+
        "\nActive: "+enabled+"\nExisting receipts and transactions remain linked.")
       .setNegativeButton("Cancel",null).setPositiveButton("Save audited profile",(dd,ww)->
        commitV26Metadata("Member profile",null,state->{
         String normalized=display.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
         Map<String,PremiumV26State.Profile> profiles=PremiumV26State.profiles(state,knownMembers());
         for(Member other:database.dao().members()){
          if(member.id.equals(other.id))continue;
          String rival=effectiveName(other,profiles).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
          if(rival.equals(normalized))
           throw new IllegalArgumentException("Possible duplicate member name; profile change canceled");
         }
         return PremiumV26State.addProfile(state,member.id,display,phone,joinedAt,note,enabled,
          "profile-"+UUID.randomUUID(),new Date().toString(),knownMembers());
        }))
       .show();
     }).show();
   });
  });
 }


 private void openStockThreshold(){
  background(()->{
   requireImportedLedger();
   long old=PremiumV26State.reorderThreshold(database.dao().meta().inventoryStockJson);
   runOnUiThread(()->{
    LinearLayout v=col();pad(v,16,8);
    EditText count=new EditText(this);count.setInputType(2);count.setHint("Individual shuttles");count.setText(old<0?"":String.valueOf(old));
    EditText reason=new EditText(this);reason.setHint("Required audit reason (8–200 chars)");
    v.addView(text("REORDER BELOW THIS STOCK COUNT",11,GRAY,true));v.addView(count);gap(v,12);v.addView(reason);
    new AlertDialog.Builder(this).setTitle("Low-stock alert · recorded counts only")
     .setView(v).setNegativeButton("Cancel",null).setPositiveButton("Review",(d,w)->{
      try{
       long value=Long.parseLong(count.getText().toString().trim());
       String why=reason.getText().toString().trim();
       if(value<0||value>1000000L||why.length()<8||why.length()>200)
        throw new IllegalArgumentException("Invalid count or reason");
       new AlertDialog.Builder(this).setTitle("Confirm reorder threshold")
        .setMessage("Previous: "+(old<0?"unconfigured":old+" shuttles")+
         "\nNext: "+value+" shuttles\nReason: "+why+
         "\n\nThis is a warning only, not an inventory adjustment.")
        .setNegativeButton("Cancel",null).setPositiveButton("Save",(dd,ww)->
         commitV26Metadata("Shuttle threshold",null,raw->
          PremiumV26State.setReorder(raw,value,why,"threshold-"+UUID.randomUUID(),new Date().toString())))
        .show();
      }catch(Exception ex){error(new IllegalArgumentException("Set a quantity up to one million and meaningful reason."));}
     }).show();
   });
  });
 }
 private void showExecutiveDashboard(){
  background(()->{
   requireImportedLedger();
   LedgerDao dao=database.dao();String month=reportMonth;
   List<PremiumV26Analytics.Month> data=PremiumV26Analytics.timeline(dao,month);
   String drift=PremiumV26State.closedMonthDrift(dao.meta().inventoryStockJson,
    ReportMath.monthly(dao,ReportMath.lastMonth(dao)));
   long max=0,collections=0,expenses=0,usage=0;
   for(PremiumV26Analytics.Month record:data){
    max=Math.max(max,Math.max(record.collections,record.expenses));
    collections=Math.addExact(collections,record.collections);
    expenses=Math.addExact(expenses,record.expenses);
    usage=Math.addExact(usage,record.usage);
   }
   long scale=Math.max(1,max);
   long recordedIn=collections,recordedOut=expenses,recordedUsed=usage;
   runOnUiThread(()->{
    LinearLayout v=col();pad(v,18,17);
    v.addView(text("Executive Dashboard",23,DARK,true));gap(v,9);
    v.addView(text("Recorded financial and stock movements through "+month,12,GRAY,false));gap(v,11);
    v.addView(text("Collections "+rupee(recordedIn)+"  ·  Expenses "+rupee(recordedOut)+
     "  ·  Usage "+recordedUsed+" shuttles",13,GREEN,true));gap(v,12);
    v.addView(text(drift,12,drift.startsWith("WARNING")?0xFF9F383A:GREEN,true));gap(v,13);
    int start=Math.max(0,data.size()-12);
    for(int i=start;i<data.size();i++){
     PremiumV26Analytics.Month r=data.get(i);
     LinearLayout item=col();item.addView(text(r.month+"    In "+rupee(r.collections)+"  ·  Out "+rupee(r.expenses),12,DARK,true));gap(item,6);
     ProgressBar a=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
     a.setMax(1000);a.setProgress((int)Math.min(1000,r.collections*1000.0/scale));a.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
     item.addView(a,fill());gap(item,3);
     ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
     b.setMax(1000);b.setProgress((int)Math.min(1000,r.expenses*1000.0/scale));b.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFC58E50));
     item.addView(b,fill());gap(item,3);
     item.addView(text("Closing "+rupee(r.closing)+" · Difference "+rupee(r.difference)+
       " · Used "+r.usage+" / acquired "+r.acquired+" shuttles · Contributors "+r.contributingMembers,
       10,GRAY,false));
     panel(v,item);
    }
    v.addView(text("Green bar = recorded collections; amber = recorded expenditure. Contributors count only members with a positive recognized cash contribution, not attendance.",11,GRAY,false));gap(v,13);
    v.addView(text("YEARLY FINANCIAL & STOCK SUMMARY",12,GREEN,true));gap(v,8);
    for(PremiumV26Analytics.Year yr:PremiumV26Analytics.annual(data)){
     LinearLayout group=col();
     group.addView(text(yr.year+" · "+yr.reportedMonths+" recorded month(s)",16,DARK,true));gap(group,7);
     group.addView(text("Cash received "+rupee(yr.received)+
      " · Expenditure "+rupee(yr.spent)+" · Final closing "+rupee(yr.closing),12,GREEN,true));gap(group,6);
     group.addView(text("Unresolved difference "+rupee(yr.unresolved)+
      " · Used "+yr.shuttles+" · Acquired "+yr.acquired+" shuttles",
      11,yr.unresolved!=0?0xFF9F383A:GRAY,false));
     panel(v,group);
    }
    v.addView(button("Share independent A4 dashboard PDF",()->sharePdf("Premium_Dashboard_"+month+".pdf",()->PremiumV26Analytics.pdf(this,data,month))));gap(v,9);
    v.addView(button("Export verified monthly CSV",()->shareAnalyticsCsv(data,month)));gap(v,8);
    v.addView(button("Export yearly reconciliation CSV",()->shareAnnualCsv(data,month)));
    ScrollView scroll=new ScrollView(this);scroll.addView(v);
    new AlertDialog.Builder(this).setView(scroll).setPositiveButton("Close",null).show();
   });
  });
 }
 private File shareCacheDir() throws IOException{
  File dir=new File(getCacheDir(),"pdfs");
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Shared attachment folder unavailable");
  return dir;
 }
 private void shareAnalyticsCsv(List<PremiumV26Analytics.Month> data,String month){
  background(()->{
   File file=new File(shareCacheDir(),"Premium_Dashboard_"+month+".csv");
   try(OutputStream out=new FileOutputStream(file)){
    out.write(PremiumV26Analytics.csv(data).getBytes(StandardCharsets.UTF_8));
   }
   runOnUiThread(()->{
    try{
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
     Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/csv");
     share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(share,"Save or share verified dashboard CSV"));
    }catch(Exception ex){error(new IOException("Could not share monthly CSV: "+ex.getMessage()));}
   });
  });
 }
 private void shareAnnualCsv(List<PremiumV26Analytics.Month> data,String month){
  background(()->{
   File file=new File(shareCacheDir(),"Premium_Yearly_"+month+".csv");
   try(OutputStream out=new FileOutputStream(file)){
    out.write(PremiumV26Analytics.annualCsv(data).getBytes(StandardCharsets.UTF_8));
   }
   runOnUiThread(()->{
    try{
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",file);
     Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/csv");
     send.putExtra(Intent.EXTRA_STREAM,uri);
     send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(send,"Save or share verified yearly CSV"));
    }catch(Exception ex){error(new IOException("Could not share yearly CSV."));}
   });
  });
 }
 private void exportMemberReceiptsZip(Member member){
  background(()->{
   requireImportedLedger();
   List<Contribution> payments=database.dao().contributions();
   List<Contribution> matches=new ArrayList<>();
   for(Contribution p:payments)if(member.id.equals(p.memberId)&&"cash".equals(p.kind)&&p.amount>0)
    matches.add(p);
   if(matches.isEmpty())throw new IllegalArgumentException("No recorded cash receipt PDFs for this member");
   if(matches.size()>400)throw new IllegalArgumentException("Please export a smaller range of receipts");
   File zip=new File(shareCacheDir(),"Member_Receipts_"+member.id.replaceAll("[^A-Za-z0-9_-]","_")+".zip");
   try(java.util.zip.ZipOutputStream output=new java.util.zip.ZipOutputStream(new FileOutputStream(zip))){
    int n=0;byte[] buffer=new byte[16384];
    for(Contribution payment:matches){
     File pdf=renderCashReceipt(member,payment);
     if(pdf==null||pdf.length()<100)throw new IOException("Receipt generation failed for "+payment.id);
     String name=String.format(Locale.ROOT,"%03d_%s_%s.pdf",++n,payment.feeMonth,payment.id.replaceAll("[^A-Za-z0-9_-]","_"));
     output.putNextEntry(new java.util.zip.ZipEntry(name));
     try(InputStream input=new FileInputStream(pdf)){
      int read;while((read=input.read(buffer))!=-1)output.write(buffer,0,read);
     }
     output.closeEntry();
    }
   }
   runOnUiThread(()->{
    try{
     Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",zip);
     Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/zip");
     share.putExtra(Intent.EXTRA_STREAM,uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivity(Intent.createChooser(share,"Save or share original-status receipt bundle"));
    }catch(Exception ex){error(new IOException("Could not share receipt ZIP: "+ex.getMessage()));}
   });
  });
 }


 private void startEncryptedExport(){
  LinearLayout form=col();pad(form,16,9);
  form.addView(text("A password is required on your new phone. If forgotten, this encrypted copy cannot be recovered.",12,GRAY,false));gap(form,12);
  EditText one=new EditText(this),two=new EditText(this);
  one.setHint("Strong password, 12+ characters");
  two.setHint("Repeat password");
  one.setInputType(129);two.setInputType(129);
  form.addView(one);gap(form,9);form.addView(two);
  new AlertDialog.Builder(this).setTitle("Encrypt verified migration backup")
   .setView(form).setNegativeButton("Cancel",null).setPositiveButton("Encrypt & save",(d,w)->{
    char[] first=one.getText().toString().toCharArray(),confirm=two.getText().toString().toCharArray();
    one.setText("");two.setText("");
    if(first.length<12||!Arrays.equals(first,confirm)){
     Arrays.fill(first,'\u0000');Arrays.fill(confirm,'\u0000');
     error(new IllegalArgumentException("Passwords must match and contain at least 12 characters."));return;
    }
    Arrays.fill(confirm,'\u0000');
    background(()->{
     try{
      requireImportedLedger();
      String verified=MigrationCodec.exportVerified(database);
      String encrypted=PremiumBackupCrypto.encrypt(verified,first);
      File file=new File(getCacheDir(),"Badminton_Premium_Encrypted_Migration.bpe");
      try(OutputStream output=new FileOutputStream(file)){
       output.write(encrypted.getBytes(StandardCharsets.UTF_8));
      }
      pendingEncryptedExport=file;
      runOnUiThread(()->{
       Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
       save.addCategory(Intent.CATEGORY_OPENABLE);
       save.setType("application/octet-stream");
       save.putExtra(Intent.EXTRA_TITLE,"Badminton_Premium_"+reportMonth+"_encrypted.bpe");
       try{startActivityForResult(save,SAVE_ENCRYPTED_V26);}
       catch(Exception ex){pendingEncryptedExport=null;error(new IOException("Secure file picker unavailable."));}
      });
     }finally{Arrays.fill(first,'\u0000');}
    });
   }).show();
 }
 private void chooseEncryptedBackup(){
  new AlertDialog.Builder(this).setTitle("Encrypted backup recovery")
   .setMessage("First decrypt and verify the file in temporary memory. Existing live accounts cannot be replaced. If the app is empty, import can be offered after successful verification. No automatic cloud sync or merging.")
   .setNegativeButton("Cancel",null)
   .setNeutralButton("Test only",(d,w)->selectEncryptedDocument(false))
   .setPositiveButton("Test then import if empty",(d,w)->selectEncryptedDocument(true))
   .show();
 }
 private void selectEncryptedDocument(boolean wantsImport){
  encryptedImportAfterCheck=wantsImport;
  Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
  pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
  pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  try{startActivityForResult(pick,READ_ENCRYPTED_V26);}
  catch(Exception ex){error(new IOException("No encrypted file picker available"));}
 }
 private byte[] readDocumentLimited(Uri uri,int limit)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(InputStream in=getContentResolver().openInputStream(uri)){
   if(in==null)throw new IOException("Could not reopen document");
   byte[] buffer=new byte[16384];int count;
   while((count=in.read(buffer))!=-1){
    if(count<=0)continue;
    if(bytes.size()+count>limit)throw new IOException("Document exceeds safe size limit");
    bytes.write(buffer,0,count);
   }
  }
  return bytes.toByteArray();
 }
 private void readEncryptedBackup(Uri uri,boolean allowImport){
  EditText pass=new EditText(this);pass.setInputType(129);
  pass.setHint("Password used for this encrypted backup");
  new AlertDialog.Builder(this).setTitle("Unlock encrypted backup")
   .setView(pass).setNegativeButton("Cancel",null).setPositiveButton("Verify",(d,w)->{
    char[] password=pass.getText().toString().toCharArray();pass.setText("");
    background(()->{
     try{
      byte[] payload=readDocumentLimited(uri,PremiumBackupCrypto.MAX_ENVELOPE);
      String decrypted=PremiumBackupCrypto.decrypt(new String(payload,StandardCharsets.UTF_8),password);
      String summary=BackupRecoveryV259.dryRun(this,decrypted);
      MigrationCodec.Preview preview=MigrationCodec.inspect(decrypted);
      if(allowImport&&database.dao().metaCount()==0){
       pendingImport=preview;
       runOnUiThread(()->new AlertDialog.Builder(this)
        .setTitle("Encrypted migration verified · empty app")
        .setMessage(summary+"\n\nImport this verified ledger into the currently EMPTY app? No merge is performed.")
        .setNegativeButton("Keep empty",(dd,ww)->pendingImport=null)
        .setPositiveButton("Import verified history",(dd,ww)->importPending()).show());
      }else{
       runOnUiThread(()->new AlertDialog.Builder(this)
        .setTitle("Encrypted backup verified · read only")
        .setMessage(summary+
         (allowImport?"\n\nImport blocked because the installed app already contains a ledger. No replacement was performed.":""))
        .setPositiveButton("OK",null).show());
      }
     }finally{Arrays.fill(password,'\u0000');}
    });
   }).show();
 }

 private void showReports(){title("Reports","Your club finances, beautifully presented");
    reportTile("▣","v26 EXECUTIVE DASHBOARD","View verified monthly cash, expenses, shuttle usage and unresolved differences",this::showExecutiveDashboard);
    reportTile("◎","MONTHLY FEES & DUES","Configure effective fees, inspect arrears and record audited concessions",this::showFeeCentre);
    reportTile("🔒","ENCRYPTED BACKUP · OPTIONAL","Password-protected export for offline transfer to a new phone",this::startEncryptedExport);
    reportTile("✓","TEST / IMPORT ENCRYPTED BACKUP","Decrypt, verify and dry-run first; import only into an empty app",this::chooseEncryptedBackup);
   LinearLayout selector=col();selector.addView(text("REPORTING MONTH",11,GRAY,true));gap(selector,9);
   EditText month=new EditText(this);month.setSingleLine(true);month.setText(reportMonth);
   month.setFocusable(false);month.setClickable(true);
   month.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.ic_menu_my_calendar,0);
   month.setCompoundDrawablePadding(dp(10));month.setBackground(border(LIGHT,LINE,12));pad(month,14,12);
   pressable(month,()->chooseMonth(month.getText().toString(),v->{month.setText(v);reportMonth=v;}));
   selector.addView(month);panel(content,selector);gap(content,13);
   notice("Prevent portrait printouts", "Use Save A4 Landscape PDF or Print A4 Landscape below. Avoid printing a shared PDF through another viewer, which can scale it into portrait.");
   gap(content,8);
   sectionHeading("Month-end review","Cashbook, reconciliation, closing and controlled reopening");
   reportTile("▤","MONTH-END FINANCIAL REVIEW","Review cash balances and generate a one-page A4 landscape PDF; close or reopen with an audit reason",
    ()->{try{openMonthEndReview(MigrationCodec.ym(month.getText().toString().trim()));}catch(Exception ex){error(ex);}});

   notice("Financial correction audit","Previous cash payments can be corrected, canceled or marked refunded only through the Receipt Library. Corrections preserve the original entry, require a reason, and adjust reported balances. Imported 2025 period totals remain protected.");

   sectionHeading("Backup & recovery","Protect your ledger before changing phones or reinstalling");
   notice("Verified backup status",BackupGuardian.status(this));
   notice("Recovery readiness",BackupGuardian.recoveryReadiness(this));
   reportTile("▣","BACK UP NOW","Creates and checks a verified JSON copy; review any folder warnings",this::backupNow);
   reportTile("✓","VERIFY LAST PRIVATE BACKUP","Checks the newest saved copy, SHA-256, and verified ledger format without changing accounts",this::verifyPrivateBackup);
   reportTile("✓","VERIFY LATEST EXTERNAL BACKUP","Reopens the latest selected-folder JSON; validates checksum, structure, and cash totals without importing",this::verifyExternalBackup);
   reportTile("▣","TEST RESTORE WITHOUT CHANGING DATA","Import a chosen verified JSON into temporary memory, re-export, compare balances and discard test database",this::testRestoreWithoutChanges);
   reportTile("⌕","COMPARE SAVED JSON WITH CURRENT LEDGER","Read-only counts, dates, and closing-balance comparison; no replacement or import",this::chooseBackupToVerify);
   reportTile("▱","CHOOSE EXTERNAL BACKUP FOLDER","One-time folder permission; automatic copies after every change",this::pickBackupFolder);
   if(BackupGuardian.folderSelected(this))
    reportTile("×","Disconnect external folder","Stops external copies without deleting any old backup files",this::disableBackupFolder);
   reportTile("⇩","Restore / import verified JSON","Safe import only into an empty database; existing accounts cannot be overwritten",this::pickMigration);
   notice("Important", "App-private copies disappear on uninstall. Choose a private Documents folder, verify its latest external backup, then test a restore in temporary memory. JSON contains unencrypted member financial information; keep exported files private.");
   gap(content,14);
   sectionHeading("Previous paid receipts","Read-only copies of existing cash entries");
   reportTile("▤","RECEIPT LIBRARY · ALL MONTHS","Search by member or month, then share or save any existing receipt or historical statement",this::showReceiptLibrary);
   gap(content,13);
   sectionHeading("Choose a report","PDF export and native landscape printing");
   reportTile("!","Kanishk February: +1 shuttle pack","Check and apply the documented noncash correction once; does not change cash balances",this::restoreHistoricalPack);
   reportTile("↓","SAVE CLASSIC PDF · A4 LANDSCAPE","Recommended: direct file export, original landscape page, larger readable text",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;savePdf("Badminton_Classic_A4_Landscape_"+m+".pdf",()->ClassicLedgerV231.master(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("▤","PRINT CLASSIC · A4 LANDSCAPE","Opens Android printing preset to landscape; preserves original landscape PDF pages",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;printClassicLandscape(m);}catch(Exception e){error(e);}});

   reportTile("⇧","Share Classic Master Ledger","Sends the original PDF file; some receiving apps may reprint as portrait",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;sharePdf("Badminton_Classic_"+m+".pdf",()->ClassicLedgerV231.master(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("≡","Monthly Cashbook","Collection, expenses and running balance for the selected month",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;sharePdf("Badminton_Monthly_"+m+".pdf",()->PdfReports.monthly(this,database.dao(),m));}catch(Exception e){error(e);}});
   
   reportTile("↓","Save Monthly PDF to phone","Choose where to store the monthly cashbook PDF",()->{try{String m=MigrationCodec.ym(month.getText().toString().trim());reportMonth=m;savePdf("Badminton_Monthly_"+m+".pdf",()->PdfReports.monthly(this,database.dao(),m));}catch(Exception e){error(e);}});
   reportTile("◫","Complete history","Classic master ledger through the latest recorded month",()->background(()->{String last=ReportMath.lastMonth(database.dao());runOnUiThread(()->sharePdf("Badminton_Complete_"+last+".pdf",()->ClassicLedgerV231.master(this,database.dao(),last)));}));
   
   reportTile("⇩","Verified account export","Save your complete ledger to a JSON file for migration or safekeeping",this::startVerifiedExport);
   gap(content,9);notice("Offline by default","PDFs and backup files are generated on your phone. External backups are written only to a folder you select. Nothing is uploaded by this app automatically.");
 }
 private void reportTile(String symbol,String heading,String detail,Runnable action){LinearLayout card=row();TextView mark=text(symbol,23,GREEN,true);mark.setGravity(Gravity.CENTER);mark.setBackground(bg(PALE,15));card.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(52)));LinearLayout body=col();pad(body,13,0);body.addView(text(heading,15,DARK,true));gap(body,6);body.addView(text(detail,11,GRAY,false));card.addView(body,new LinearLayout.LayoutParams(0,-2,1));card.addView(text("›",26,GREEN,true));pressable(card,action);panel(content,card);}
 
 

 /**
  * Hand the already-generated one-page landscape PDF directly to Android's print spooler.
  * The PDF bytes are copied intact (not rasterized onto a portrait canvas).
  * The print framework is asked to use A4 landscape and rejects portrait settings.
  */
 private void printClassicLandscape(String selectedMonth){
  background(()->{
   if(database.dao().meta()==null)throw new IllegalStateException("Import your verified ledger first");
   final File source=ClassicLedgerV231.master(this,database.dao(),selectedMonth);
   verifyClassicPdf(source);
   runOnUiThread(()->{
    try{
     PrintManager manager=(PrintManager)getSystemService(Context.PRINT_SERVICE);
     if(manager==null)throw new IOException("Android printing service is unavailable");
     PrintAttributes attributes=new PrintAttributes.Builder()
      .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
      .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
      .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
      .build();
     String filename="Badminton_Classic_A4_Landscape_"+selectedMonth+".pdf";
     manager.print(filename,new PrintDocumentAdapter(){
      @Override public void onLayout(PrintAttributes oldAttributes,PrintAttributes current,
       CancellationSignal signal,LayoutResultCallback callback,Bundle extras){
       if(signal.isCanceled()){callback.onLayoutCancelled();return;}
       if(current.getMediaSize()!=null&&current.getMediaSize().isPortrait()){
        callback.onLayoutFailed("Please select A4 landscape instead of portrait");
        return;
       }
       PrintDocumentInfo info=new PrintDocumentInfo.Builder(filename)
         .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build();
       callback.onLayoutFinished(info,true);
      }
      @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,
       CancellationSignal signal,WriteResultCallback callback){
       io.execute(()->{
        if(signal.isCanceled()){callback.onWriteCancelled();return;}
        try(InputStream input=new FileInputStream(source);
            OutputStream output=new FileOutputStream(destination.getFileDescriptor())){
         byte[] buffer=new byte[16384];int count;
         while((count=input.read(buffer))!=-1){
          if(signal.isCanceled()){callback.onWriteCancelled();return;}
          output.write(buffer,0,count);
         }
         output.flush();
         callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        }catch(Exception ex){callback.onWriteFailed("Unable to write A4 landscape PDF: "+ex.getMessage());}
       });
      }
      @Override public void onFinish(){source.delete();super.onFinish();}
     },attributes);
    }catch(Exception exception){
     source.delete();
     error(new IOException("Unable to start A4 landscape printing: "+exception.getMessage()));
    }
   });
  });
 }

 private void verifyClassicPdf(File file) throws IOException{
  if(!file.getName().startsWith("Classic_v23_1_"))return;
  try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
      android.graphics.pdf.PdfRenderer doc=new android.graphics.pdf.PdfRenderer(descriptor)){
   if(doc.getPageCount()!=1)throw new IOException("The Classic PDF must contain exactly one page");
   try(android.graphics.pdf.PdfRenderer.Page pg=doc.openPage(0)){
    if(pg.getWidth()!=842||pg.getHeight()!=595)throw new IOException("Classic PDF is not A4 landscape. Generation cancelled to prevent a tiny portrait ledger.");
   }
  }
 }

 interface RunnablePdf{File make() throws Exception;}
 private void savePdf(String name,RunnablePdf renderer){background(()->{
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
 });}
 private void previewPdf(RunnablePdf renderer){
  background(()->{
   requireImportedLedger();
   File file=renderer.make();
   if(file==null||!file.isFile()||file.length()<100)
    throw new IOException("The selected receipt could not be generated.");
   final Bitmap bitmap;
   final int pages;
   try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
       android.graphics.pdf.PdfRenderer pdf=new android.graphics.pdf.PdfRenderer(descriptor)){
    pages=pdf.getPageCount();
    if(pages<1)throw new IOException("Receipt PDF has no pages");
    try(android.graphics.pdf.PdfRenderer.Page firstPage=pdf.openPage(0)){
     float multiplier=Math.min(2.5f,2000f/Math.max(firstPage.getWidth(),firstPage.getHeight()));
     int width=Math.max(1,Math.round(firstPage.getWidth()*multiplier));
     int height=Math.max(1,Math.round(firstPage.getHeight()*multiplier));
     bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
     bitmap.eraseColor(Color.WHITE);
     firstPage.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
    }
   }
   runOnUiThread(()->{
    final Dialog viewer=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    LinearLayout frame=col();frame.setBackgroundColor(Color.WHITE);pad(frame,10,9);
    LinearLayout toolbar=row();
    LinearLayout captions=col();
    captions.addView(text("Receipt preview",20,DARK,true));gap(captions,3);
    captions.addView(text(pages==1?"Full-screen · one verified PDF page":
       "Displaying first of "+pages+" PDF pages",11,GRAY,false));
    toolbar.addView(captions,new LinearLayout.LayoutParams(0,-2,1));
    toolbar.addView(inlineButton("Close",GREEN,viewer::dismiss));
    frame.addView(toolbar);gap(frame,10);
    ImageView image=new ImageView(this);
    image.setImageBitmap(bitmap);image.setAdjustViewBounds(true);
    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int available=Math.max(dp(260),getResources().getDisplayMetrics().widthPixels-dp(36));
    image.setLayoutParams(new LinearLayout.LayoutParams(available,-2));
    android.widget.HorizontalScrollView horizontal=new android.widget.HorizontalScrollView(this);
    horizontal.setFillViewport(true);horizontal.addView(image);
    ScrollView vertical=new ScrollView(this);vertical.setFillViewport(true);
    vertical.addView(horizontal);
    frame.addView(vertical,new LinearLayout.LayoutParams(-1,0,1));gap(frame,7);
    LinearLayout controls=row();
    controls.addView(inlineButton("Fit width",GREEN,()->{
     image.getLayoutParams().width=available;image.requestLayout();
     horizontal.smoothScrollTo(0,0);
    }),new LinearLayout.LayoutParams(0,-2,1));
    controls.addView(inlineButton("Zoom 2×",GREEN,()->{
     image.getLayoutParams().width=Math.min(available*2,bitmap.getWidth()*2);
     image.requestLayout();
    }),new LinearLayout.LayoutParams(0,-2,1));
    frame.addView(controls);
    viewer.setContentView(frame);
    viewer.setOnDismissListener(d->{
     image.setImageDrawable(null);
     if(!bitmap.isRecycled())bitmap.recycle();
     file.delete();
    });
    viewer.show();
    Window window=viewer.getWindow();
    if(window!=null)window.setLayout(-1,-1);
   });
  });
 }
 private void sharePdf(String filename,RunnablePdf renderer){background(()->{
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
 });}
}

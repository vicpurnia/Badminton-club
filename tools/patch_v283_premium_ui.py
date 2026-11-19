#!/usr/bin/env python3
"""v28.3 native presentation overlay; preserve all financial handlers and formats."""
from pathlib import Path
import re
import shutil

root = Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity = root / "MainActivity.java"
s = activity.read_text(encoding="utf-8")


def matching(source, start, opening="{", closing="}"):
    """Match Java delimiters while ignoring comments and quoted text."""
    assert source[start] == opening
    depth, state, i = 0, None, start
    while i < len(source):
        ch = source[i]
        following = source[i:i + 2]
        if state in ('"', "'"):
            if ch == "\\":
                i += 2
                continue
            if ch == state:
                state = None
        elif state == "//":
            if ch == "\n":
                state = None
        elif state == "/*":
            if following == "*/":
                state = None
                i += 1
        elif following in ("//", "/*"):
            state = following
            i += 1
        elif ch in ('"', "'"):
            state = ch
        elif ch == opening:
            depth += 1
        elif ch == closing:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise AssertionError("Unbalanced Java source")


def method(signature):
    start = s.index(signature)
    brace = s.index("{", start)
    return s[start:matching(s, brace) + 1]


def once(old, new):
    global s
    if s.count(old) != 1:
        raise AssertionError(f"Expected one v28.3 source anchor ({s.count(old)}): {old[:100]}")
    s = s.replace(old, new, 1)


def replace_method(signature, new):
    once(method(signature), new.strip())


once(
    "private static final int GREEN=0xFF12644E,DARK=0xFF102E29,LIGHT=0xFFF6F8F4,GRAY=0xFF72847C,GOLD=0xFFE6C88A,LINE=0xFFE6EBE5,PALE=0xFFEAF3ED;",
    "private static final int GREEN=PremiumV283Ui.ACCENT,DARK=PremiumV283Ui.INK,"
    "LIGHT=PremiumV283Ui.IVORY,GRAY=PremiumV283Ui.MUTED,GOLD=PremiumV283Ui.GOLD,"
    "LINE=PremiumV283Ui.LINE,PALE=PremiumV283Ui.SOFT;",
)

# Recreating the Activity keeps presentation state, while financial forms still
# use their existing explicit review/commit flow and immutable submission IDs.
once('restoreMemberPhotoSelection(b);layout();selectTab(pendingPhotoMemberId==null?"Home":"Members");',
     'restorePremiumUiState(b);restoreMemberPhotoSelection(b);\n'
     '  memberPhotoActivity=new java.lang.ref.WeakReference<>(this);\n'
     '  layout();selectTab(pendingPhotoMemberId==null?tab:"Members");')
once('  super.onSaveInstanceState(state);', '''  super.onSaveInstanceState(state);
  state.putString("v283.ui.tab",tab);
  state.putString("v283.ui.month",reportMonth);
  state.putString("v283.ui.memberSearch",searchText);
  state.putString("v283.ui.memberFilter",memberFilter);
  state.putString("v283.ui.reportSearch",premiumReportQuery);
  state.putString("v283.ui.reportGroup",premiumReportGroup);''')
once(' private void restoreMemberPhotoSelection(Bundle state){', ''' private void restorePremiumUiState(Bundle state){
  if(state==null)return;
  String restoredTab=state.getString("v283.ui.tab","Home");
  if(Arrays.asList("Home","Collect","Shuttles","Members","Reports").contains(restoredTab))tab=restoredTab;
  String month=state.getString("v283.ui.month",reportMonth);
  if(month!=null&&month.matches("[0-9]{4}-(0[1-9]|1[0-2])"))reportMonth=month;
  searchText=state.getString("v283.ui.memberSearch","");
  String filter=state.getString("v283.ui.memberFilter","All");
  if(Arrays.asList("All","Paid","Partial","Unpaid","Settled","Not due","Unconfigured").contains(filter))memberFilter=filter;
  premiumReportQuery=state.getString("v283.ui.reportSearch","");
  String group=state.getString("v283.ui.reportGroup","All");
  if(Arrays.asList("All","Reports","Club","Backups").contains(group))premiumReportGroup=group;
 }
 private void restoreMemberPhotoSelection(Bundle state){''')
once('return screenEpoch==epoch&&expectedTab.equals(tab)&&month.equals(reportMonth);',
     'return !isFinishing()&&!isDestroyed()&&screenEpoch==epoch&&expectedTab.equals(tab)&&month.equals(reportMonth);')
once('@Override protected void onDestroy(){io.shutdown();super.onDestroy();}',
     '@Override protected void onDestroy(){screenEpoch++;memberRenderGeneration++;\n'
     '  if(memberPhotoActivity.get()==this)memberPhotoActivity.clear();\n'
     '  io.shutdown();super.onDestroy();}')
once('private void error(Exception e){', 'private void error(Exception e){if(isFinishing()||isDestroyed())return;')
replace_method('private void background(', '''
private void background(CheckedJob job){
 if(isFinishing()||isDestroyed())return;
 try{io.execute(()->{try{job.run();}catch(Exception issue){runOnUiThread(()->error(issue));}});}
 catch(java.util.concurrent.RejectedExecutionException issue){error(issue);}
}''')

# A photo save is process-scoped, so queued work on a previous Activity cannot
# overwrite a newer selection made after rotation. Completion follows the live
# Activity and does not cancel already queued accounting operations.
once(' private boolean memberPhotoInProgress;', ''' private boolean memberPhotoInProgress;
 private static final java.util.concurrent.atomic.AtomicBoolean memberPhotoWork=
  new java.util.concurrent.atomic.AtomicBoolean(false);
 private static java.lang.ref.WeakReference<MainActivity> memberPhotoActivity=
  new java.lang.ref.WeakReference<>(null);
 private void finishMemberPhotoWork(Exception issue,String success){
  new Handler(Looper.getMainLooper()).post(()->{
   memberPhotoWork.set(false);
   memberPhotoInProgress=false;
   MainActivity current=memberPhotoActivity.get();
   if(current==null||current.isFinishing()||current.isDestroyed())return;
   current.memberPhotoInProgress=false;
   if("Members".equals(current.tab))current.selectTab("Members");
   if(issue!=null)current.error(issue);
   else Toast.makeText(current,success,Toast.LENGTH_SHORT).show();
  });
 }''')
once('if(pendingPhotoMemberId==null&&!memberPhotoInProgress)return false;',
     'if(pendingPhotoMemberId==null&&!memberPhotoInProgress&&!memberPhotoWork.get())return false;')
once('''    if(memberPhotoBusy())return;
    memberPhotoInProgress=true;
    final int screen=screenEpoch;
    background(()->{
     try{
      PremiumV28MemberPhotos.remove(this,member.id);
      runOnUiThread(()->{
       if(!isFinishing()&&!isDestroyed()&&screen==screenEpoch)selectTab("Members");
      });
     }finally{runOnUiThread(()->memberPhotoInProgress=false);}
    });''', '''    if(memberPhotoBusy()||!memberPhotoWork.compareAndSet(false,true))return;
    memberPhotoInProgress=true;
    try{
     io.execute(()->{
      Exception failure=null;
      try{PremiumV28MemberPhotos.remove(getApplicationContext(),member.id);}
      catch(Exception issue){failure=issue;}
      finally{finishMemberPhotoWork(failure,"Member photo removed");}
     });
    }catch(java.util.concurrent.RejectedExecutionException issue){finishMemberPhotoWork(issue,"Member photo removed");}''')
once('''   final Uri chosen=data.getData();
   final int screen=screenEpoch;
   memberPhotoInProgress=true;
   background(()->{
    try{
     requireImportedLedger();
     boolean exists=false;
     for(Member m:database.dao().members())if(memberId.equals(m.id)){exists=true;break;}
     if(!exists)throw new IOException("Original member ID no longer exists. Photograph was not saved.");
     PremiumV28MemberPhotos.addOrReplace(this,chosen,memberId);
     runOnUiThread(()->{
      if(isFinishing()||isDestroyed())return;
      Toast.makeText(this,"Member photo saved",Toast.LENGTH_SHORT).show();
      if(screen==screenEpoch)selectTab("Members");
     });
    }finally{runOnUiThread(()->memberPhotoInProgress=false);}
   });''', '''   final Uri chosen=data.getData();
   if(!memberPhotoWork.compareAndSet(false,true)){
    Toast.makeText(this,"Finish the current photo save first",Toast.LENGTH_SHORT).show();return;
   }
   memberPhotoInProgress=true;
   try{
    io.execute(()->{
     Exception failure=null;
     try{
      requireImportedLedger();
      boolean exists=false;
      for(Member m:database.dao().members())if(memberId.equals(m.id)){exists=true;break;}
      if(!exists)throw new IOException("Original member ID no longer exists. Photograph was not saved.");
      PremiumV28MemberPhotos.addOrReplace(getApplicationContext(),chosen,memberId);
     }catch(Exception issue){failure=issue;}
     finally{finishMemberPhotoWork(failure,"Member photo saved");}
    });
   }catch(java.util.concurrent.RejectedExecutionException issue){finishMemberPhotoWork(issue,"Member photo saved");}''')

# Read-only form preparation may finish after rotation. Never attach a dialog
# to the obsolete Activity, and leave submitted worker operations untouched.
for signature in ("private void openMemberProfile(", "private void memberWallet(",
                  "private void openPayment(", "private void showFeeCentre(",
                  "private void openStockThreshold("):
    original = method(signature)
    guarded = original.replace("runOnUiThread(()->{", "runOnUiThread(()->{\n    if(isFinishing()||isDestroyed())return;")
    assert guarded != original, "Expected asynchronous form preparation: " + signature
    once(original, guarded)


replace_method("private Button button(", """
private Button button(String label,Runnable task){
 Button view=new Button(this);view.setAllCaps(false);view.setText(label);
 view.setTextColor(Color.WHITE);view.setTextSize(15);
 view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
 view.setMinHeight(dp(50));pad(view,16,12);
 view.setBackground(bg(GREEN,12));bindGuardedClick(view,task);return view;
}
private Button secondaryButton(String label,Runnable task){
 Button view=button(label,task);view.setTextColor(GREEN);
 view.setBackground(border(Color.WHITE,LINE,12));return view;
}
private Button dangerButton(String label,Runnable task){
 Button view=button(label,task);view.setTextColor(PremiumV283Ui.DANGER);
 view.setBackground(bg(PremiumV283Ui.DANGER_SOFT,12));return view;
}""")
once("private void bindGuardedClick(View view,Runnable task){",
     "private void bindGuardedClick(View view,Runnable task){\n  PremiumV283Ui.touch(view);")
once("t.setTextSize(size);", "t.setTextSize(Math.max(12,size));")
replace_method("private TextView inlineButton(", """
private TextView inlineButton(String label,int color,Runnable run){
 TextView view=text(label,13,color,true);pad(view,12,12);
 view.setGravity(Gravity.CENTER);view.setBackground(border(PALE,LINE,12));
 bindGuardedClick(view,run);return view;
}""")
replace_method("private LinearLayout metric(", """
private LinearLayout metric(String label,String value,String symbol){
 LinearLayout card=col();card.setBackground(border(Color.WHITE,LINE,16));pad(card,14,16);
 card.addView(PremiumV283Ui.icon(this,PremiumV283Ui.iconKey(label),GREEN,22),
  new LinearLayout.LayoutParams(dp(22),dp(22)));
 gap(card,12);card.addView(text(label,12,GRAY,false));gap(card,6);
 card.addView(text(value,22,DARK,true));return card;
}""")
replace_method("private LinearLayout action(", """
private LinearLayout action(String symbol,String caption,Runnable runnable){
 LinearLayout box=col();box.setGravity(Gravity.CENTER);
 box.setBackground(border(Color.WHITE,LINE,16));pad(box,8,16);
 box.addView(PremiumV283Ui.icon(this,PremiumV283Ui.iconKey(caption),GREEN,24),
  new LinearLayout.LayoutParams(dp(24),dp(24)));
 gap(box,10);TextView label=text(caption,12,DARK,true);
 label.setGravity(Gravity.CENTER);box.addView(label);
 pressable(box,runnable);return box;
}""")
replace_method("private TextView avatar(", """
private TextView avatar(String name){
 TextView view=text(PremiumV283Ui.initials(name),14,GREEN,true);
 view.setGravity(Gravity.CENTER);view.setBackground(bg(PALE,15));
 view.setWidth(dp(44));view.setHeight(dp(44));return view;
}""")
replace_method("private void title(", """
private void title(String heading,String sub){
 content.removeAllViews();if(pageScroll!=null)pageScroll.scrollTo(0,0);
 content.addView(text(heading,23,DARK,true));gap(content,6);
 content.addView(text(sub,13,GRAY,false));gap(content,18);
}""")

version = re.search(r'"(v\d+\.\d+ DEVELOPMENT CANDIDATE)"', method("private void layout()")).group(1)
replace_method("private void layout()", """
private void layout(){
 root=col();root.setBackgroundColor(LIGHT);setContentView(root);
 if(Build.VERSION.SDK_INT>=35){
  root.setOnApplyWindowInsetsListener((view,insets)->{
   android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
   view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
  });root.requestApplyInsets();
 }
 getWindow().setStatusBarColor(LIGHT);getWindow().setNavigationBarColor(LIGHT);
 androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView())
  .setAppearanceLightStatusBars(true);
 androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView())
  .setAppearanceLightNavigationBars(true);
 header=col();header.setBackgroundColor(LIGHT);pad(header,18,10);
 boolean compactHeader=PremiumV283Ui.compact(this);
 LinearLayout top=row();
 ImageView brandIcon=PremiumV283Ui.icon(this,"court",DARK,32);
 brandIcon.setBackground(bg(GOLD,11));pad(brandIcon,6,6);
 int brandSize=compactHeader?32:40;
 top.addView(brandIcon,new LinearLayout.LayoutParams(dp(brandSize),dp(brandSize)));
 LinearLayout brand=col();pad(brand,11,0);
 TextView brandTitle=text("Badminton Club",compactHeader?16:17,DARK,true);
 brandTitle.setTag("brand:title");brand.addView(brandTitle);
 if(!compactHeader){gap(brand,4);
  brand.addView(text("__VERSION__".replace(" DEVELOPMENT CANDIDATE"," · Preview"),11,GRAY,false));
 }
 top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
 if(!compactHeader)top.addView(pill("Offline",PremiumV283Ui.SUCCESS,PremiumV283Ui.SUCCESS_SOFT));
 header.addView(top);
 if(compactHeader){
  gap(header,6);LinearLayout meta=row();
  meta.addView(text("__VERSION__".replace(" DEVELOPMENT CANDIDATE"," Preview"),11,GRAY,false),
   new LinearLayout.LayoutParams(0,-2,1));
  meta.addView(pill("Offline",PremiumV283Ui.SUCCESS,PremiumV283Ui.SUCCESS_SOFT));header.addView(meta);
 }
 root.addView(header,fill());
 ScrollView scroll=new ScrollView(this);pageScroll=scroll;
 scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
 content=col();pad(content,16,16);scroll.addView(content);
 root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
 nav=row();nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(6));pad(nav,5,6);
 root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
}""".replace("__VERSION__", version))
replace_method("private void drawNav()", """
private void drawNav(){
 nav.removeAllViews();
 boolean compactNav=PremiumV283Ui.compact(this);
 nav.setOrientation(compactNav?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
 LinearLayout line=nav;
 if(compactNav){line=row();nav.addView(line,fill());}
 int position=0;
 for(String item:new String[]{"Home","Collect","Shuttles","Members","Reports"}){
  if(compactNav&&position==3){gap(nav,4);line=row();nav.addView(line,fill());}
  boolean active=tab.equals(item);LinearLayout tile=col();tile.setGravity(Gravity.CENTER);
  tile.setBackground(bg(active?PALE:Color.WHITE,14));pad(tile,2,8);
  tile.setTag("nav:"+item);tile.setSelected(active);tile.setContentDescription(item);
  if(!compactNav){
   tile.addView(PremiumV283Ui.icon(this,PremiumV283Ui.iconKey(item),active?GREEN:GRAY,24),
    new LinearLayout.LayoutParams(dp(24),dp(24)));gap(tile,5);
  }
  TextView caption=text(item,11,active?GREEN:GRAY,active);
  caption.setTextSize(11);caption.setGravity(Gravity.CENTER);tile.addView(caption);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
  params.setMargins(dp(2),0,dp(2),0);line.addView(tile,params);
  pressable(tile,()->selectTab(item));tile.setMinimumHeight(dp(compactNav?52:68));position++;
 }
}""")
replace_method("private void monthPicker()", """
private void monthPicker(){
 LinearLayout holder=row();holder.setBackground(border(Color.WHITE,LINE,14));pad(holder,6,4);
 ImageView previous=PremiumV283Ui.icon(this,"chevron-left",GREEN,24);pad(previous,12,12);
 previous.setBackground(bg(PALE,12));pressable(previous,()->shiftMonth(-1));
 previous.setContentDescription("Previous month");
 holder.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));
 LinearLayout middle=col();middle.setGravity(Gravity.CENTER);
 TextView month=text(dateLabel(reportMonth)+"  ▾",16,DARK,true);
 month.setGravity(Gravity.CENTER);middle.addView(month);
 middle.setContentDescription("Choose month, "+dateLabel(reportMonth));
 pressable(middle,()->chooseMonth(reportMonth,value->{reportMonth=value;selectTab(tab);}));
 holder.addView(middle,new LinearLayout.LayoutParams(0,-2,1));
 ImageView next=PremiumV283Ui.icon(this,"chevron-right",GREEN,24);pad(next,12,12);
 next.setBackground(bg(PALE,12));pressable(next,()->shiftMonth(1));
 next.setContentDescription("Next month");
 holder.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));
 content.addView(holder,fill());gap(content,16);
}""")

# Fields use consistent native, keyboard-accessible controls. Existing handlers,
# input types, hints and values remain intact.
s = s.replace("new EditText(this)", "PremiumV283Ui.field(this)")
s = s.replace("AutoCompleteTextView who=new AutoCompleteTextView(this);",
              "AutoCompleteTextView who=new AutoCompleteTextView(this);PremiumV283Ui.styleField(who);")
once('.setNegativeButton("Keep editing",(d,w)->reviewing.set(false))',
     '.setNegativeButton("Keep editing",(d,w)->reviewing.set(false))\n'
     '        .setOnCancelListener(cancelled->reviewing.set(false))')

# Everyday actions appear before longer status information.
home = method("private void showHome()")
quick_start = home.index('    sectionHeading("Quick actions"')
quick_end = home.index('    sectionHeading("Recent activity"', quick_start)
quick = home[quick_start:quick_end]
new_home = home[:quick_start] + home[quick_end:]
new_home = new_home.replace('    LinearLayout stats=row();', quick + '    LinearLayout stats=row();', 1)
new_home = new_home.replace('text(rupee(latest.closing),39,', 'text(rupee(latest.closing),33,')
new_home = new_home.replace('pad(card,22,24)', 'pad(card,20,22)')
new_home = new_home.replace(
    "VINAYAK Badminton Premium v26.9 — development TEST candidate only. Keep your signed v25.9 ledger and its independently verified external backup. A signed test build is not an approved production release. Verified history can be imported only into an EMPTY database.",
    "Import your verified ledger to bring your members, receipts and stock records onto this phone. Start from a verified backup. This is a preview build; imports are available only when the ledger is empty.")
new_home = new_home.replace(
    '   final int paymentCount=count;',
    '   final Map<String,PremiumV26State.Profile> displayProfiles=PremiumV26State.profiles(meta.inventoryStockJson,knownMembers());\n'
    '   final int paymentCount=count;')
new_home = new_home.replace(
    'for(Member m:members)names.put(m.id,m.name);',
    'for(Member m:members)names.put(m.id,effectiveName(m,displayProfiles));')
once(home, new_home)

# Member cards keep photo action IDs and financial click behavior, with room for
# names and amounts. The add-player action no longer requires scrolling the roster.
once(
    'content.addView(button("Monthly fee rules, dues & concessions",this::showFeeCentre));\n'
    '  gap(content,13);\n'
    '  LinearLayout results=col();content.addView(results);gap(content,10);\n'
    '  content.addView(button("＋ Add new player",this::addMember),fill());',
    'content.addView(button("＋ Add new player",this::addMember),fill());gap(content,9);\n'
    '  content.addView(secondaryButton("Monthly fee rules, dues & concessions",this::showFeeCentre));gap(content,14);\n'
    '  LinearLayout results=col();results.addView(text("Loading members…",13,GRAY,false));content.addView(results);')
once(
    'if(generation==memberRenderGeneration&&currentScreen(screen,"Members",viewedMonth))notice("No verified ledger","Import your saved club ledger first.");',
    'if(generation==memberRenderGeneration&&currentScreen(screen,"Members",viewedMonth)){\n'
    '       results.removeAllViews();panel(results,blockHeader("No verified ledger","Import your saved club ledger first."));\n'
    '      }')
once(
    '''LinearLayout card=row();
      card.addView(memberAvatar(item.member,item.name));
      LinearLayout labels=col();pad(labels,11,0);
      labels.addView(text(item.name+(item.active?"":" · Archived"),14,DARK,true));gap(labels,5);
      labels.addView(text(viewedMonth+" · "+item.detail,11,GRAY,false));
      card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
      boolean settled="Paid".equals(item.status)||"Settled".equals(item.status);
      boolean partial="Partial".equals(item.status);
      card.addView(pill(item.status,settled?GREEN:partial?0xFF9B6B18:GRAY,
        settled?PALE:partial?0xFFFFF2D7:0xFFF1F3F1));''',
    '''LinearLayout card=col();LinearLayout identity=row();
      identity.addView(memberAvatar(item.member,item.name));
      TextView memberName=text(item.name+(item.active?"":" · Archived"),15,DARK,true);
      memberName.setTag("member-name:"+item.member.id);pad(memberName,11,0);
      if(!PremiumV283Ui.compact(this)){
       memberName.setMaxLines(2);memberName.setEllipsize(android.text.TextUtils.TruncateAt.END);
      }
      identity.addView(memberName,new LinearLayout.LayoutParams(0,-2,1));
      boolean settled="Paid".equals(item.status)||"Settled".equals(item.status);
      boolean partial="Partial".equals(item.status);
      card.addView(identity);gap(card,10);
      TextView status=pill(item.status,settled?PremiumV283Ui.SUCCESS:partial?PremiumV283Ui.WARNING:GRAY,
        settled?PremiumV283Ui.SUCCESS_SOFT:partial?PremiumV283Ui.WARNING_SOFT:LIGHT);
      card.addView(status,new LinearLayout.LayoutParams(-2,-2));gap(card,10);
      card.addView(text(item.detail,12,GRAY,false));
      card.setContentDescription(item.name+". "+item.status+". "+item.detail+". Open member history");''')
once('body.addView(button("Remove photo",', 'body.addView(dangerButton("Remove photo",')
once('body.addView(button("Done",options::dismiss)', 'body.addView(secondaryButton("Done",options::dismiss)')
once('LinearLayout body=col();pad(body,20,17);',
     'LinearLayout body=col();pad(body,20,17);body.setBackground(bg(Color.WHITE,24));')
s = s.replace('body.addView(button("Remove local member photo",',
              'body.addView(dangerButton("Remove local member photo",')
once('search.setSingleLine(true);search.setTextSize(14);search.setHint("Search members by name");',
     'search.setSingleLine(true);search.setTextSize(14);search.setHint("Search members by name");\n'
     '  search.setTag("members:search");')
once('''   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
   lp.setMargins(dp(3),0,dp(5),0);''',
     '''   chip.setSelected(option.equals(memberFilter));chip.setTag("member-filter:"+option);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
   lp.setMargins(dp(3),0,dp(5),0);''')

payments = method("private void showPayments()")
hero_start = payments.index("  LinearLayout headline=")
hero_end = payments.index('  sectionHeading("Monthly receipts"', hero_start)
new_payments = payments[:hero_start] + '''
  LinearLayout headline=col();headline.setBackground(border(Color.WHITE,LINE,18));pad(headline,18,18);
  headline.addView(text("Record a contribution",20,DARK,true));gap(headline,8);
  headline.addView(text("Choose a player, confirm the cash received and share a receipt.",13,GRAY,false));gap(headline,16);
  headline.addView(button("＋  Record cash actually received",()->openPayment(null)));gap(headline,9);
  headline.addView(secondaryButton("▤  Find or share any past receipt",this::showReceiptLibrary));
  content.addView(headline);gap(content,20);
''' + payments[hero_end:]
new_payments = new_payments.replace(
    'Map<String,Member> names=new HashMap<>();',
    'Map<String,PremiumV26State.Profile> displayProfiles=PremiumV26State.profiles(meta.inventoryStockJson,knownMembers());'
    'Map<String,Member> names=new HashMap<>();')
new_payments = new_payments.replace('avatar(m.name)', 'avatar(effectiveName(m,displayProfiles))')
new_payments = new_payments.replace('text(m.name,14,', 'text(effectiveName(m,displayProfiles),14,')
receipt_start = new_payments.index('   LinearLayout card=row();card.addView(avatar(')
receipt_end = new_payments.index('if(++n==16)break;}', receipt_start)
new_payments = new_payments[:receipt_start] + '''
   LinearLayout card=col();LinearLayout identity=row();
   identity.addView(avatar(effectiveName(m,displayProfiles)),new LinearLayout.LayoutParams(dp(44),dp(44)));
   LinearLayout info=col();pad(info,11,0);
   TextView memberLabel=text(effectiveName(m,displayProfiles),15,DARK,true);
   memberLabel.setMaxLines(2);memberLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
   info.addView(memberLabel);gap(info,5);
   info.addView(text(dateLabel(c.feeMonth)+(c.approximate?" · Imported":" · "+c.method),12,GRAY,false));
   identity.addView(info,new LinearLayout.LayoutParams(0,-2,1));card.addView(identity,fill());gap(card,12);
   long net=corrections.containsKey(c.id)?corrections.get(c.id).effective:c.amount;
   boolean compactReceipt=PremiumV283Ui.compact(this);
   LinearLayout receiptFooter=compactReceipt?col():row();
   TextView receiptAmount=text(net==0?"VOID":rupee(net),19,net==0?GRAY:DARK,true);
   receiptAmount.setTag("receipt-amount:"+c.id);
   receiptFooter.addView(receiptAmount,compactReceipt?fill():new LinearLayout.LayoutParams(0,-2,1));
   if(compactReceipt)gap(receiptFooter,8);
   TextView receiptAction=inlineButton(net==0?"Void record":"Receipt ▤",GREEN,()->shareRecordedReceipt(m,c));
   receiptAction.setTag("receipt-action:"+c.id);
   receiptAction.setContentDescription((net==0?"Share void record for ":"Share receipt for ")+
    effectiveName(m,displayProfiles)+" · "+dateLabel(c.feeMonth));
   receiptFooter.addView(receiptAction,compactReceipt?fill():new LinearLayout.LayoutParams(-2,-2));
   card.addView(receiptFooter,fill());panel(content,card);
   ''' + new_payments[receipt_end:]
once(payments, new_payments)

expenses = method("private void showExpenses()")
hero_start = expenses.index("  LinearLayout hero=")
hero_end = expenses.index("  final String selected=", hero_start)
new_expenses = expenses[:hero_start] + '''
  LinearLayout hero=col();hero.setBackground(border(Color.WHITE,LINE,18));pad(hero,18,18);
  hero.addView(text("Keep your court stocked",20,DARK,true));gap(hero,8);
  hero.addView(text("Record purchases, donations and the shuttles you use.",13,GRAY,false));gap(hero,16);
  hero.addView(button("＋ Record shuttle purchase",this::openExpense));gap(hero,9);
  hero.addView(secondaryButton("＋ Record donated shuttle pack",this::openShuttleDonation));gap(hero,9);
  hero.addView(secondaryButton("− Record shuttle usage",this::openShuttleUsage));
  content.addView(hero);gap(content,12);
  LinearLayout settings=col();settings.setVisibility(View.GONE);
  settings.addView(secondaryButton("◉ Configure confirmed opening stock",this::openInventoryCheckpoint));gap(settings,8);
  settings.addView(secondaryButton("Configure low-stock warning",this::openStockThreshold));gap(settings,8);
  settings.addView(secondaryButton("✓ Reconcile completed-month physical stock",this::openPhysicalStockCount));gap(settings,10);
  settings.addView(text("Start with a confirmed month-start count. Later physical counts reconcile differences without changing earlier purchases or usage.",12,GRAY,false));
  TextView setup=inlineButton("Stock settings & physical count  ▾",GREEN,()->{
   boolean expanded=settings.getVisibility()!=View.VISIBLE;
   settings.setVisibility(expanded?View.VISIBLE:View.GONE);
  });
  setup.setTag("stock:setup");content.addView(setup,fill());gap(content,10);
  content.addView(settings,fill());gap(content,12);
''' + expenses[hero_end:]
once(expenses, new_expenses)

# Receipt Library owns its screen epoch; returning always goes through navigation.
# Searches accept both the immutable recorded name and current profile label.
library = method("private void showReceiptLibrary()")
new_library = library.replace(
    "private void showReceiptLibrary(){",
    'private void showReceiptLibrary(){\n  screenEpoch++;tab="Collect";drawNav();\n'
    '  final int libraryScreen=screenEpoch;final String libraryMonth=reportMonth;')
new_library = new_library.replace('button("‹ Back to Collect",this::showPayments)',
                                  'secondaryButton("‹ Back to Collect",()->selectTab("Collect"))')
new_library = new_library.replace(
    'Map<String,Member> members=new HashMap<>();',
    'Map<String,PremiumV26State.Profile> displayProfiles=PremiumV26State.profiles(dao.meta().inventoryStockJson,knownMembers());\n'
    '   Map<String,Member> members=new HashMap<>();')
new_library = new_library.replace(
    '   runOnUiThread(()->{\n    final String[] mode=',
    '   runOnUiThread(()->{\n    if(!currentScreen(libraryScreen,"Collect",libraryMonth))return;\n'
    '    final java.util.Map<String,TextView> receiptChips=new java.util.LinkedHashMap<>();\n'
    '    final String[] mode=')
new_library = new_library.replace(
    '    repaint[0]=()->{\n     results.removeAllViews();',
    '''    repaint[0]=()->{
     if(!currentScreen(libraryScreen,"Collect",libraryMonth))return;
     for(java.util.Map.Entry<String,TextView> entry:receiptChips.entrySet()){
      boolean active=entry.getKey().equals(mode[0]);TextView chip=entry.getValue();
      chip.setSelected(active);chip.setTextColor(active?Color.WHITE:GREEN);
      chip.setBackground(bg(active?GREEN:PALE,100));
     }
     results.removeAllViews();''')
new_library = new_library.replace(
    'String haystack=(item.member.name+" "+item.period()',
    'String displayName=effectiveName(item.member,displayProfiles);\n'
    '      String haystack=(displayName+" "+item.member.name+" "+item.period()')
new_library = new_library.replace('avatar(item.member.name)', 'avatar(displayName)')
new_library = new_library.replace('text(item.member.name,15,', 'text(displayName,15,')
new_library = new_library.replace('TextView chip=pill(option,GREEN,PALE);',
                                   'TextView chip=pill(option,GREEN,PALE);receiptChips.put(option,chip);')
once(library, new_library)

# Retain each Report callback verbatim, including PDF filenames and import guards.
reports = method("private void showReports()")
tiles = []
cursor = 0
while True:
    start = reports.find("reportTile(", cursor)
    if start < 0:
        break
    end = matching(reports, start + len("reportTile"), "(", ")") + 1
    call = reports[start:end] + ";"
    heading = re.match(r'reportTile\("(?:[^"\\]|\\.)*","((?:[^"\\]|\\.)*)"', call).group(1)
    tiles.append((heading, call))
    cursor = end
assert len(tiles) == 24, f"Expected all 24 report actions, got {len(tiles)}"
priority = [
    "MONTH-END FINANCIAL REVIEW", "MONTHLY FEES & DUES", "RECEIPT LIBRARY · ALL MONTHS",
    "v26 EXECUTIVE DASHBOARD", "SAVE CLASSIC PDF · A4 LANDSCAPE",
    "PRINT CLASSIC · A4 LANDSCAPE", "Monthly Cashbook", "Save Monthly PDF to phone",
]
tiles.sort(key=lambda row: priority.index(row[0]) if row[0] in priority else len(priority))
selector_start = reports.index("   LinearLayout selector=")
selector_end = reports.index("   notice(", selector_start)
selector = reports[selector_start:selector_end].replace("panel(content,selector);gap(content,13);",
                                                      "panel(content,selector);")
calls = "\n".join(
    ('  if(BackupGuardian.folderSelected(this))\n   ' if heading == "Disconnect external folder" else "  ") + call
    for heading, call in tiles
)
replace_method("private void showReports()", '''
private void showReports(){
 title("Reports","Your club finances, beautifully presented");
''' + selector + '''
 setupReportCatalogue();
''' + calls + '''
 filterReportCatalogue();
 gap(content,12);
 content.addView(text("PDFs and backups are created on your phone. For printing, choose the A4 landscape actions. Keep a verified external backup before changing phones.",12,GRAY,false));
}''')
replace_method("private void reportTile(", '''
private final ArrayList<PremiumV283Ui.ToolRow> premiumReportRows=new ArrayList<>();
private final java.util.Map<String,TextView> premiumReportChips=new java.util.LinkedHashMap<>();
private LinearLayout premiumReportsList;
private TextView premiumReportCount;
private EditText premiumReportSearch;
private String premiumReportGroup="All",premiumReportQuery="";
private void setupReportCatalogue(){
 premiumReportRows.clear();premiumReportChips.clear();
 premiumReportSearch=PremiumV283Ui.field(this);premiumReportSearch.setSingleLine(true);
 premiumReportSearch.setHint("Search reports, receipts or backups");
 premiumReportSearch.setText(premiumReportQuery);
 premiumReportSearch.setTag("reports:search");content.addView(premiumReportSearch,fill());gap(content,12);
 LinearLayout chips=row();
 for(String group:new String[]{"All","Reports","Club","Backups"}){
  TextView chip=pill(group,GREEN,PALE);chip.setTag("reports:category:"+group);
  premiumReportChips.put(group,chip);
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMargins(0,0,dp(8),0);
  chips.addView(chip,params);
  pressable(chip,()->{premiumReportGroup=group;filterReportCatalogue();});
 }
 HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);
 strip.addView(chips);content.addView(strip,fill());gap(content,12);
 premiumReportCount=text("",12,GRAY,false);content.addView(premiumReportCount);gap(content,10);
 premiumReportsList=col();content.addView(premiumReportsList,fill());
 premiumReportSearch.addTextChangedListener(new android.text.TextWatcher(){
  public void beforeTextChanged(CharSequence value,int start,int count,int after){}
  public void onTextChanged(CharSequence value,int start,int before,int count){filterReportCatalogue();}
  public void afterTextChanged(android.text.Editable value){}
 });
}
private void filterReportCatalogue(){
 premiumReportQuery=premiumReportSearch.getText().toString();
 String query=premiumReportSearch.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0;
 for(PremiumV283Ui.ToolRow row:premiumReportRows){
  boolean matches=("All".equals(premiumReportGroup)||premiumReportGroup.equals(row.category))&&
   (query.isEmpty()||(row.title+" "+PremiumV283Ui.label(row.title)+" "+row.detail).toLowerCase(Locale.ROOT).contains(query));
  row.view.setVisibility(matches?View.VISIBLE:View.GONE);if(matches)shown++;
 }
 premiumReportCount.setText(shown==0?"No matching tools. Try another search or category.":shown+" tools · "+premiumReportGroup);
 for(java.util.Map.Entry<String,TextView> entry:premiumReportChips.entrySet()){
  boolean active=entry.getKey().equals(premiumReportGroup);TextView chip=entry.getValue();
  chip.setSelected(active);chip.setTextColor(active?Color.WHITE:GREEN);
  chip.setBackground(bg(active?GREEN:PALE,100));
 }
}
private void reportTile(String symbol,String heading,String detail,Runnable action){
 LinearLayout card=row();pad(card,14,16);card.setBackground(border(Color.WHITE,LINE,16));
 card.setTag("report:"+heading);
 ImageView icon=PremiumV283Ui.icon(this,PremiumV283Ui.iconKey(heading),GREEN,24);
 icon.setBackground(bg(PALE,12));pad(icon,10,10);
 card.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));
 LinearLayout body=col();pad(body,12,0);
 body.addView(text(PremiumV283Ui.label(heading),15,DARK,true));gap(body,6);
 body.addView(text(detail,12,GRAY,false));
 card.addView(body,new LinearLayout.LayoutParams(0,-2,1));
 card.addView(text("›",24,GRAY,false));pressable(card,action);
 LinearLayout wrapper=col();wrapper.addView(card,fill());gap(wrapper,10);
 premiumReportsList.addView(wrapper,fill());
 premiumReportRows.add(new PremiumV283Ui.ToolRow(wrapper,heading,detail));
}''')

# Replace legacy green surface tokens only in the UI, never in ledger/PDF code.
colors = {
    "0xFF165E4A": "0xFF233C62", "0xFF07352D": "0xFF14243B", "0xFF154E3E": "0xFF233C62",
    "0xFF0A513F": "0xFF264BA6", "0xFFD7F3E0": "0xFFE2E9F5", "0xFF2C735B": "0xFF38547B",
    "0xFFD2E7DA": "0xFFD5DFEE", "0xFF376C5B": "0xFF425773", "0xFFB6D4C3": "0xFFCAD5E6",
    "0xFFD6F0E1": "0xFFE0E7F4", "0xFFD4E9DF": "0xFFDCE5F3", "0xFFD6EBDD": "0xFFDCE5F3",
    "0xFFE1F1E9": "0xFFE2E9F5", "0x2412644E": "0x24315DCC",
}
for old, new in colors.items():
    s = s.replace(old, new)

# Keep editors alive through validation and review. A committed transaction is
# distinct from its subsequent backup: backup trouble must never offer the
# already accepted mutation as an unsaved retry.
original = method("private void commitV26Metadata(")
transaction_start = original.index("   database.runInTransaction(()->{")
transaction_end = original.index("   BackupGuardian.save(")
transaction = original[transaction_start:transaction_end]
replace_method("private void commitV26Metadata(", '''
private void commitV26Metadata(String action,String affectedMonth,java.util.function.Function<String,String> edit){
 commitV26Metadata(null,action,affectedMonth,edit);
}
private void commitV26Metadata(android.content.DialogInterface editor,String action,String affectedMonth,
 java.util.function.Function<String,String> edit){
 savePremiumForm(editor,()->{
''' + transaction + '''
 },"v26-"+action.toLowerCase(Locale.ROOT).replace(' ','-'),()->{
  Toast.makeText(this,"Saved with audit history",Toast.LENGTH_LONG).show();
  if(tab.equals("Members"))showMembers();else if(tab.equals("Reports"))showReports();
 });
}
private void savePremiumForm(android.content.DialogInterface editor,CheckedJob transaction,
 String backupReason,Runnable completed){
 if(isFinishing()||isDestroyed())return;
 if(editor!=null&&!PremiumV283FormBuilder.beginSave(editor))return;
 try{
  io.execute(()->{
   try{transaction.run();}
   catch(Exception issue){
    runOnUiThread(()->{PremiumV283FormBuilder.failed(editor);error(issue);});
    return;
   }
   // This completion belongs to the transaction, even if the backup later fails.
   runOnUiThread(()->{
    PremiumV283FormBuilder.saved(editor);
    if(isFinishing()||isDestroyed())return;
    completed.run();
   });
   try{
    BackupGuardian.save(getApplicationContext(),database,backupReason,true);
    warnOnBackupFailure();
   }catch(Exception backupIssue){
    runOnUiThread(()->{
     if(isFinishing()||isDestroyed())return;
     new AlertDialog.Builder(this).setTitle("Saved · backup needs attention")
      .setMessage("Your change was saved. Do not submit it again. Check backups in Reports.\\n\\n"+backupIssue.getMessage())
      .setPositiveButton("OK",null).show();
    });
   }
  });
 }catch(java.util.concurrent.RejectedExecutionException issue){
  PremiumV283FormBuilder.failed(editor);error(issue);
 }
}''')

original = method("private void addMember(")
revised = original.replace("new AlertDialog.Builder(this)", "new PremiumV283FormBuilder(this)", 1)
revised = revised.replace('.setPositiveButton("Add",(d,w)->background(()->{',
                          '.setPositiveButton("Add",(d,w)->{')
revised = revised.replace('    if(n.isEmpty()||n.length()>100)throw new IllegalArgumentException("Name must be 1–100 characters");',
                         '    if(n.isEmpty()||n.length()>100){error(new IllegalArgumentException("Name must be 1–100 characters"));return;}\n'
                         '    savePremiumForm(d,()->{')
revised = revised.replace('    BackupGuardian.save(this,database,"member-added",true);warnOnBackupFailure();\n'
                          '    runOnUiThread(this::showMembers);\n'
                          '   })).show();',
                         '    },"member-added",()->{if("Members".equals(tab))showMembers();});\n'
                         '   }).show();')
assert revised != original and 'savePremiumForm(d,' in revised
once(original, revised)

for signature, title, action in (
    ("private void editFeePolicy(", "Audited fee policy", "Fee policy"),
    ("private void editFeeWaiver(", "Record authorized noncash concession", "Fee concession"),
    ("private void openMemberProfile(", "Member profile · audited", "Member profile"),
    ("private void openStockThreshold(", "Low-stock alert · recorded counts only", "Shuttle threshold"),
):
    original = method(signature)
    builder = 'new AlertDialog.Builder(this).setTitle("' + title + '")'
    assert original.count(builder) == 1
    revised = original.replace(builder, builder.replace("AlertDialog.Builder", "PremiumV283FormBuilder"))
    revised = revised.replace('commitV26Metadata("' + action + '",', 'commitV26Metadata(d,"' + action + '",')
    if action == "Member profile":
        revised = revised.replace('      boolean enabled=active.isChecked();',
                                  '      boolean enabled=active.isChecked();\n'
                                  '      final String profileEventId="profile-"+UUID.randomUUID();')
        revised = revised.replace('          "profile-"+UUID.randomUUID(),new Date().toString(),knownMembers());',
                                  '          profileEventId,new Date().toString(),knownMembers());')
    if action == "Shuttle threshold":
        revised = revised.replace('       new AlertDialog.Builder(this).setTitle("Confirm reorder threshold")',
                                  '       final String thresholdEventId="threshold-"+UUID.randomUUID();\n'
                                  '       new AlertDialog.Builder(this).setTitle("Confirm reorder threshold")')
        revised = revised.replace('PremiumV26State.setReorder(raw,value,why,"threshold-"+UUID.randomUUID(),new Date().toString())',
                                  'PremiumV26State.setReorder(raw,value,why,thresholdEventId,new Date().toString())')
    once(original, revised)

original = method("private void startEncryptedExport(")
revised = original.replace('new AlertDialog.Builder(this).setTitle("Encrypt verified migration backup")',
                          'new PremiumV283FormBuilder(this).setTitle("Encrypt verified migration backup")')
revised = revised.replace('.setNegativeButton("Cancel",(d,w)->encryptedExportGate.finish())',
                          '.setNegativeButton("Cancel",(d,w)->{one.setText("");two.setText("");encryptedExportGate.finish();})')
revised = revised.replace('.setOnCancelListener(d->encryptedExportGate.finish())',
                          '.setOnCancelListener(d->{one.setText("");two.setText("");encryptedExportGate.finish();})')
revised = revised.replace('    one.setText("");two.setText("");\n', '', 1)
revised = revised.replace('     encryptedExportGate.finish();\n     error(new IllegalArgumentException("Passwords must match',
                          '     error(new IllegalArgumentException("Passwords must match', 1)
revised = revised.replace("    Arrays.fill(confirm,'\\u0000');\n    background(()->{", '''    Arrays.fill(confirm,'\\u0000');
    if(!PremiumV283FormBuilder.beginSave(d)){Arrays.fill(first,'\\u0000');return;}
    one.setText("");two.setText("");
    PremiumV283FormBuilder.saved(d);
    try{io.execute(()->{''', 1)
revised = revised.replace('      runOnUiThread(()->{\n       Intent save=', '''      runOnUiThread(()->{
       if(isFinishing()||isDestroyed()){
        pendingEncryptedExport=null;file.delete();encryptedExportGate.finish();return;
       }
       Intent save=''', 1)
revised = revised.replace('      throw ex;\n     }finally{Arrays.fill(first,',
                          '      runOnUiThread(()->error(ex));\n     }finally{Arrays.fill(first,', 1)
revised = revised.replace("    });\n   }).show();", '''    });}catch(java.util.concurrent.RejectedExecutionException issue){
     Arrays.fill(first,'\\u0000');encryptedExportGate.finish();error(issue);
    }
   }).show();''', 1)
assert revised != original and 'if(!PremiumV283FormBuilder.beginSave(d))' in revised
once(original, revised)

# Only the form body scrolls. The retained builder constrains its viewport to
# the usable dialog window, keeping native Review/Cancel actions above the IME.
for signature, body in (("private void editFeePolicy(", "f"),
                        ("private void editFeeWaiver(", "f"),
                        ("private void openMemberProfile(", "v"),
                        ("private void openStockThreshold(", "v"),
                        ("private void addMember(", "e"),
                        ("private void startEncryptedExport(", "form")):
    original = method(signature)
    old_view = ".setView(" + body + ")"
    assert original.count(old_view) == 1, "Unexpected form container: " + signature
    once(original, original.replace(old_view, ".setView(premiumForm(" + body + "))"))
once("private Button button(", '''private ScrollView premiumForm(View body){
 return PremiumV283FormBuilder.body(this,body);
}
private Button button(''')

activity.write_text(s, encoding="utf-8")
shutil.copyfile("src/premium/v28_3/PremiumV283Ui.java", root / "PremiumV283Ui.java")
shutil.copyfile("src/premium/v28_3/PremiumV283FormBuilder.java", root / "PremiumV283FormBuilder.java")
theme = Path("app/src/main/res/values/premium_v283_styles.xml")
theme.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile("src/premium/v28_3/styles.xml", theme)
manifest = Path("app/src/main/AndroidManifest.xml")
manifest_text = manifest.read_text(encoding="utf-8")
old_theme = '@android:style/Theme.Material.Light.NoActionBar'
assert manifest_text.count(old_theme) == 1, "Unexpected manifest theme"
manifest.write_text(manifest_text.replace(old_theme, "@style/PremiumV283Theme"), encoding="utf-8")
print("PASS v28.3 premium native UI, 24 unchanged report handlers, receipt routing and duplicate-cancel repair")

package com.vinayak.badminton.nativepreview;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.EditText;
import android.widget.TextView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Synthetic-only UI acceptance: real controls, read-only dialogs, no cash writes. */
@RunWith(AndroidJUnit4.class)
public final class V283WorkflowUiTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String MONTH="2026-08";
 private static final String[] TABS={"Home","Members","Collect","Shuttles","Reports"};
 private static final String[] REPORTS={
  "v28 BACKUP & RELEASE READINESS","v26 EXECUTIVE DASHBOARD","MONTHLY FEES & DUES",
  "CASH RECEIPT-DATE EVIDENCE","ENCRYPTED BACKUP · OPTIONAL","TEST / IMPORT ENCRYPTED BACKUP",
  "MONTH-END FINANCIAL REVIEW","BACK UP NOW","VERIFY LAST PRIVATE BACKUP",
  "VERIFY LATEST EXTERNAL BACKUP","TEST RESTORE WITHOUT CHANGING DATA",
  "COMPARE SAVED JSON WITH CURRENT LEDGER","CHOOSE EXTERNAL BACKUP FOLDER",
  "Restore / import verified JSON","RECEIPT LIBRARY · ALL MONTHS",
  "Kanishk February: +1 shuttle pack","SAVE CLASSIC PDF · A4 LANDSCAPE",
  "PRINT CLASSIC · A4 LANDSCAPE","Share Classic Master Ledger","Monthly Cashbook",
  "Save Monthly PDF to phone","Complete history","Verified account export"
 };
 private Instrumentation instrumentation;
 private Context context;
 private MainActivity activity;

 @Test public void premiumScreensAndActionsRemainUsable()throws Exception {
  instrumentation=InstrumentationRegistry.getInstrumentation();
  context=instrumentation.getTargetContext();
  LedgerDatabase db=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db")
   .allowMainThreadQueries().build();
  try {
   assertNotNull("Synthetic upgrade fixture is required",db.dao().meta());
   assertTrue("Never test writes or real club data",db.dao().meta().club.startsWith("CI Synthetic Badminton Club"));
   assertEquals(15,db.dao().members().size());
   List<Member> members=db.dao().members();
   Member selected=null;
   for(Member member:members)if(MEMBER.equals(member.id))selected=member;
   assertNotNull("Known positive-payment fixture member must exist",selected);
   final Member member=selected;
   String before=canonical(db);
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
    scenario.onActivity(value->activity=value);
    onMain(()->{
     setField("reportMonth",MONTH);
     invoke("selectTab",new Class<?>[]{String.class},"Home");
     // Accepted names must never make avatar rendering crash, including an
     // honorific-only label and a supplementary Unicode character.
     for(String name:new String[]{"Dr.","🏸 Player","","   ","डॉ. प्रकाश"}){
      Object avatar=invoke("avatar",new Class<?>[]{String.class},name);
      assertTrue("Avatar remains a renderable view for "+name,avatar instanceof View);
      if(avatar instanceof TextView)assertTrue("Initials fallback cannot be empty",
       ((TextView)avatar).getText().length()>0);
     }
    });

    for(String tab:TABS){
     selectTab(tab);
     await("Loaded content on "+tab,()->screenReady(tab));
     twoFrames();
     onMain(()->assertNavigation(tab));
     screenshot("premium-"+tab.toLowerCase(java.util.Locale.ROOT)+".png");
    }
    onMain(()->{
     for(String heading:REPORTS){
      View tile=activity.getWindow().getDecorView().findViewWithTag("report:"+heading);
      assertNotNull("Report action remains reachable in view hierarchy: "+heading,tile);
      assertTrue("Report action is enabled: "+heading,tile.isEnabled());
      assertTrue("Report action is wired: "+heading,tile.isClickable());
      assertTouchTarget(tile,"Report "+heading);
     }
    });
    System.out.println("V283_REPORT_ACTION_INVENTORY_PASS: "+REPORTS.length+" report entry points");
    onMain(()->{
     View search=activity.getWindow().getDecorView().findViewWithTag("reports:search");
     assertTrue("Report search field is available",search instanceof EditText);
     ((EditText)search).setText("cashbook");
    });
    await("Report search filters actions",()->reportShown("Monthly Cashbook")&&!reportShown("BACK UP NOW"));
    onMain(()->((EditText)activity.getWindow().getDecorView().findViewWithTag("reports:search")).setText(""));
    await("Clearing search restores report actions",()->reportShown("Monthly Cashbook")&&reportShown("BACK UP NOW"));
    clickView(()->activity.getWindow().getDecorView().findViewWithTag("reports:category:Backups"),"Backups category");
    await("Backup category filters actions",()->reportShown("BACK UP NOW")&&!reportShown("Monthly Cashbook"));
    onMain(()->assertTrue("Chosen report category is marked selected",
     activity.getWindow().getDecorView().findViewWithTag("reports:category:Backups").isSelected()));
    clickView(()->activity.getWindow().getDecorView().findViewWithTag("reports:category:All"),"All report categories");
    await("All category restores report actions",()->reportShown("Monthly Cashbook")&&reportShown("BACK UP NOW"));
    System.out.println("V283_REPORT_SEARCH_AND_CATEGORIES_PASS");

    // Regresses Reports -> Library -> Back leaving tab=Reports and silently
    // rejecting actual Collect receipt rows through the stale-screen guard.
    clickReport("RECEIPT LIBRARY · ALL MONTHS");
    await("Receipt Library contains actual fixture receipt",()->
     activityText("Receipt Library")&&activityText(NAME));
    onMain(()->{
     EditText search=(EditText)findHint(activity.getWindow().getDecorView(),
      "Player, month, receipt ID, amount, payment method");
     assertNotNull("Receipt Library search remains available",search);
     search.setText("zz-no-such-synthetic-player");
    });
    await("Receipt Library shows no-match state",()->activityText("No matching cash records"));
    onMain(()->((EditText)findHint(activity.getWindow().getDecorView(),
     "Player, month, receipt ID, amount, payment method")).setText(NAME));
    await("Original member name remains searchable",()->activityText(NAME)&&!activityText("No matching cash records"));
    clickActivityText("‹ Back to Collect");
    await("Back to Collect renders actual August receipt",()->
     "Collect".equals(field("tab"))&&activityText("Collect fees")&&activityText(NAME));
    onMain(()->assertNavigation("Collect"));
    System.out.println("V283_REPORTS_RECEIPT_LIBRARY_BACK_PASS");

    // A canceled duplicate confirmation must release the form's reviewing
    // flag. Both attempts stop at confirmation; no new payment is committed.
    onMain(()->invoke("openPayment",new Class<?>[]{String.class},MEMBER));
    awaitDialog("New contribution");
    onMain(()->{
     View form=requireDialog("New contribution");
     EditText amount=(EditText)findHint(form,"Actual payment amount (₹)");
     assertNotNull("Actual amount field is present",amount);
     amount.setText("1379");
    });
    clickDialogText("New contribution","Save & share receipt  →");
    awaitDialog("Contribution already recorded");
    backUntilClosed("Contribution already recorded");
    awaitDialog("New contribution");
    SystemClock.sleep(750); // Honor the existing guarded-click debounce.
    clickDialogText("New contribution","Save & share receipt  →");
    awaitDialog("Contribution already recorded");
    clickDialogText("Contribution already recorded","Keep editing");
    await("Confirmation closed",()->dialog("Contribution already recorded")==null);
    backUntilClosed("New contribution");
    assertEquals("Duplicate-review cancellation cannot alter ledger",before,canonical(db));
    System.out.println("V283_DUPLICATE_PAYMENT_BACK_RETRY_PASS");

    // Exercise each stock entry form through its existing real screen button.
    selectTab("Shuttles");
    await("Shuttle ledger loaded",()->screenReady("Shuttles"));
    openActivityForm("＋ Record shuttle purchase","New shuttle purchase",5,"Review purchase");
    openActivityForm("＋ Record donated shuttle pack","Record donated shuttle packs",5,"Review noncash donation");
    openActivityForm("− Record shuttle usage","Record shuttle usage",4,"Review usage entry");
    onMain(()->{
     View setup=findText(activity.getWindow().getDecorView(),"◉ Configure confirmed opening stock");
     assertTrue("Advanced stock controls start folded",setup==null||!setup.isShown());
    });
    clickView(()->activity.getWindow().getDecorView().findViewWithTag("stock:setup"),"Stock settings & physical count");
    await("Stock settings can be expanded",()->{
     View setup=findText(activity.getWindow().getDecorView(),"◉ Configure confirmed opening stock");
     return setup!=null&&setup.isShown();
    });
    openActivityForm("◉ Configure confirmed opening stock","Confirmed opening shuttle stock",4,
     "Review confirmed stock checkpoint");
    openActivityForm("Configure low-stock warning","Low-stock alert · recorded counts only",2,"Review");
    clickActivityText("✓ Reconcile completed-month physical stock");
    if(InventoryCheckpoint.read(db.dao().meta().inventoryStockJson)==null){
     awaitDialog("Action not completed");
     onMain(()->assertNotNull("Missing opening stock has a specific prerequisite message",
      findText(requireDialog("Action not completed"),
       "Confirm starting opening stock before recording a physical reconciliation.")));
     backUntilClosed("Action not completed");
     System.out.println("V283_PHYSICAL_COUNT_PREREQUISITE_GUARD_PASS");
    }else{
     assertForm("Physical stock reconciliation",3,"Review reconciliation");
     backUntilClosed("Physical stock reconciliation");
    }

    selectTab("Members");
    await("Members roster loaded",()->screenReady("Members"));
    openActivityForm("＋ Add new player","New player",1,"Add");
    onMain(()->invoke("openMemberProfile",new Class<?>[]{Member.class},member));
    assertForm("Member profile · audited",4,"Review");
    backUntilClosed("Member profile · audited");
    System.out.println("V283_MEMBER_PROFILE_ENTRY_PASS");

    selectTab("Reports");
    clickReport("MONTHLY FEES & DUES");
    awaitDialog("Fees, dues & arrears");
    clickDialogText("Fees, dues & arrears","Set monthly fee (effective dated)");
    assertForm("Audited fee policy",3,"Review");
    backUntilClosed("Audited fee policy");
    clickDialogText("Fees, dues & arrears","Record authorized fee concession");
    assertForm("Record authorized noncash concession",3,"Review");
    backUntilClosed("Record authorized noncash concession");
    backUntilClosed("Fees, dues & arrears");
    System.out.println("V283_FEE_AND_CONCESSION_ENTRY_PASS");

    clickReport("MONTH-END FINANCIAL REVIEW");
    awaitDialog("Month-end financial review");
    clickDialogText("Month-end financial review","Close completed month with audit");
    assertForm("Confirm monthly closing",1,"Confirm CLOSE");
    backUntilClosed("Confirm monthly closing");
    System.out.println("V283_MONTH_END_REVIEW_AND_CANCEL_PASS");

    reportDialog("v26 EXECUTIVE DASHBOARD","Executive Dashboard",
     new String[]{"Share independent A4 dashboard PDF","Export verified monthly CSV","Export yearly reconciliation CSV"});
    reportDialog("CASH RECEIPT-DATE EVIDENCE","Recorded receipt-date evidence",
     new String[]{"Share dated-cash evidence CSV"});
    reportDialog("BACK UP NOW","Verified backup status",new String[]{"OK"});
    reportDialog("VERIFY LAST PRIVATE BACKUP","Backup integrity check",new String[]{"OK"});
    reportDialog("v28 BACKUP & RELEASE READINESS","Release readiness · read-only",new String[]{"Close"});
    clickReport("ENCRYPTED BACKUP · OPTIONAL");
    assertForm("Encrypt verified migration backup",2,"Encrypt & save");
    backUntilClosed("Encrypt verified migration backup");
    reportDialog("TEST / IMPORT ENCRYPTED BACKUP","Encrypted backup recovery",
     new String[]{"Test only","Test then import if empty","Cancel"});
    // External folders are deliberately not granted by this test.
    clickReport("VERIFY LATEST EXTERNAL BACKUP");
    awaitDialog("Action not completed");
    onMain(()->assertNotNull("Missing external permission has a specific recovery instruction",
     findText(requireDialog("Action not completed"),"Choose an external backup folder before verification.")));
    backUntilClosed("Action not completed");
    System.out.println("V283_BACKUP_AND_ENCRYPTED_ENTRY_PASS");

    assertEquals("Opening and canceling every checked workflow preserves all ledger fields",before,canonical(db));
    assertEquals("No member was added or archived",15,db.dao().members().size());
    assertEquals("No contribution was duplicated",2,db.dao().contributions().size());
    assertEquals("No purchase was recorded",1,db.dao().expenses().size());
    assertEquals("No usage entry was recorded",1,db.dao().stockUsages().size());
    System.out.println("V283_PREMIUM_WORKFLOW_UI_PASS: navigation, form access, cancellation, original ledger unchanged");
   }
  }finally{db.close();}
 }

 private void openActivityForm(String action,String title,int fields,String commit)throws Exception {
  clickActivityText(action);assertForm(title,fields,commit);backUntilClosed(title);
  System.out.println("V283_FORM_ENTRY_PASS: "+title);
 }
 private void reportDialog(String action,String title,String[] controls)throws Exception {
  clickReport(action);awaitDialog(title);
  onMain(()->{
   View root=requireDialog(title);
   for(String label:controls)assertNotNull("Dialog action is present: "+label,clickable(findText(root,label)));
  });
  backUntilClosed(title);System.out.println("V283_REPORT_DIALOG_PASS: "+title);
 }
 private void assertForm(String title,int fields,String commit)throws Exception {
  awaitDialog(title);twoFrames();
  onMain(()->{
   View root=requireDialog(title);
   assertTrue("Required input fields remain in "+title,countInputs(root)>=fields);
   View action=clickable(findText(root,commit));
   assertNotNull("Review action exists in "+title,action);
   assertTrue("Review action is enabled in "+title,action.isEnabled());
   assertTouchTarget(action,title+" review");
  });
 }
 private void selectTab(String tab)throws Exception {
  onMain(()->{
   View navigation=activity.getWindow().getDecorView().findViewWithTag("nav:"+tab);
   assertNotNull("Navigation item exists: "+tab,navigation);
   assertTrue(navigation.performClick());
  });
  await("Selected navigation "+tab,()->tab.equals(field("tab")));
 }
 private void assertNavigation(String selected){
  int selectedCount=0;
  for(String name:TABS){
   View view=activity.getWindow().getDecorView().findViewWithTag("nav:"+name);
   assertNotNull(name+" navigation",view);assertTrue(view.isClickable());
   assertTrue(name+" navigation must receive focus",view.isFocusable());
   assertEquals(name+" selected state",name.equals(selected),view.isSelected());
   if(view.isSelected())selectedCount++;
   assertTouchTarget(view,"Navigation "+name);
  }
  assertEquals("Exactly one navigation destination is selected",1,selectedCount);
 }
 private boolean screenReady(String tab){
  if("Home".equals(tab))return activityText("Overview")&&activityText(NAME);
  if("Members".equals(tab))return activityText(NAME)&&activityText("CI Synthetic Player 15");
  if("Collect".equals(tab))return activityText("Collect fees")&&activityText(NAME);
  if("Shuttles".equals(tab))return activityText("Shuttle purchases (1)");
  return activity.getWindow().getDecorView().findViewWithTag("report:Verified account export")!=null;
 }
 private void clickReport(String title)throws Exception {
  clickView(()->activity.getWindow().getDecorView().findViewWithTag("report:"+title),"Report "+title);
 }
 private boolean reportShown(String title){
  View tile=activity.getWindow().getDecorView().findViewWithTag("report:"+title);
  return tile!=null&&tile.isShown();
 }
 private void clickActivityText(String label)throws Exception {
  clickView(()->clickable(findText(activity.getWindow().getDecorView(),label)),label);
 }
 private void clickDialogText(String title,String label)throws Exception {
  clickView(()->clickable(findText(requireDialog(title),label)),title+" / "+label);
 }
 private interface ViewFinder{View find();}
 private void clickView(ViewFinder finder,String label)throws Exception {
  onMain(()->{
   View target=finder.find();assertNotNull("Cannot find action: "+label,target);
   target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);
  });
  twoFrames();
  onMain(()->{
   View target=finder.find();assertNotNull(label,target);
   assertTrue("Action enabled: "+label,target.isEnabled());
   assertTrue("Action clickable: "+label,target.isClickable());
   assertTrue("Action dispatches: "+label,target.performClick());
  });
 }
 private void backUntilClosed(String title)throws Exception {
  for(int attempt=0;attempt<3;attempt++){
   instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
   SystemClock.sleep(180);
   AtomicBoolean closed=new AtomicBoolean();onMain(()->closed.set(dialog(title)==null));
   if(closed.get())return;
  }
  fail("Back did not dismiss dialog: "+title+"\n"+windowDiagnostic());
 }
 private void awaitDialog(String title)throws Exception {await("Dialog "+title,()->dialog(title)!=null);}
 private View requireDialog(String title){View root=dialog(title);assertNotNull("Dialog "+title,root);return root;}
 private View dialog(String title){
  List<View> windows=WindowInspector.getGlobalWindowViews();
  for(int i=windows.size()-1;i>=0;i--){
   View root=windows.get(i);
   if(root!=activity.getWindow().getDecorView()&&root.isAttachedToWindow()&&root.isShown()&&findText(root,title)!=null)return root;
  }
  return null;
 }
 private boolean activityText(String text){return findText(activity.getWindow().getDecorView(),text)!=null;}
 private static View findText(View root,String label){
  if(root instanceof TextView&&label.equalsIgnoreCase(((TextView)root).getText().toString()))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;
   for(int i=0;i<group.getChildCount();i++){View found=findText(group.getChildAt(i),label);if(found!=null)return found;}}
  return null;
 }
 private static View findHint(View root,String hint){
  if(root instanceof EditText&&hint.contentEquals(((EditText)root).getHint()==null?"":((EditText)root).getHint()))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;
   for(int i=0;i<group.getChildCount();i++){View found=findHint(group.getChildAt(i),hint);if(found!=null)return found;}}
  return null;
 }
 private static View clickable(View node){
  for(View view=node;view!=null;view=view.getParent() instanceof View?(View)view.getParent():null)
   if(view.isClickable())return view;
  return null;
 }
 private static int countInputs(View root){
  int count=root instanceof EditText?1:0;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;
   for(int i=0;i<group.getChildCount();i++)count+=countInputs(group.getChildAt(i));}
  return count;
 }
 private void assertTouchTarget(View view,String label){
  int minimum=(int)(48*activity.getResources().getDisplayMetrics().density+.5f);
  assertTrue(label+" needs at least 48 dp height; actual="+view.getHeight(),view.getHeight()>=minimum);
 }
 private void await(String label,BooleanSupplier ready)throws Exception {
  long end=SystemClock.elapsedRealtime()+15000;
  AtomicBoolean done=new AtomicBoolean();
  do{onMain(()->done.set(ready.getAsBoolean()));if(done.get())return;Thread.sleep(100);}
  while(SystemClock.elapsedRealtime()<end);
  fail("Timed out: "+label+"\n"+windowDiagnostic());
 }
 private void onMain(Runnable work){
  AtomicReference<Throwable> failure=new AtomicReference<>();
  instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable issue){failure.set(issue);}});
  if(failure.get()!=null)throw new AssertionError("UI assertion failed",failure.get());
 }
 private Object invoke(String name,Class<?>[] types,Object...args){
  try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,args);}
  catch(Exception issue){throw new AssertionError("Cannot invoke "+name,issue);}
 }
 private Object field(String name){
  try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
  catch(Exception issue){throw new AssertionError(name,issue);}
 }
 private void setField(String name,Object value){
  try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(activity,value);}
  catch(Exception issue){throw new AssertionError(name,issue);}
 }
 private static String canonical(LedgerDatabase db)throws Exception {
  return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
 }
 private void twoFrames()throws Exception {
  CountDownLatch frames=new CountDownLatch(1);
  onMain(()->Choreographer.getInstance().postFrameCallback(first->
   Choreographer.getInstance().postFrameCallback(second->frames.countDown())));
  assertTrue("UI must produce two draw frames",frames.await(5,TimeUnit.SECONDS));
  instrumentation.waitForIdleSync();
 }
 private void screenshot(String name){
  Bitmap image=null;
  try{
   image=instrumentation.getUiAutomation().takeScreenshot();if(image==null)return;
   File directory=context.getExternalFilesDir("premium-ui-evidence");
   if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))return;
   try(FileOutputStream output=new FileOutputStream(new File(directory,name))){image.compress(Bitmap.CompressFormat.PNG,100,output);}
  }catch(Exception issue){System.out.println("Optional premium UI screenshot unavailable: "+issue.getClass().getSimpleName());}
  finally{if(image!=null)image.recycle();}
 }
 private String windowDiagnostic(){
  StringBuilder result=new StringBuilder();
  onMain(()->{
   for(View window:WindowInspector.getGlobalWindowViews()){
    if(window.isShown()){result.append("WINDOW ").append(window.getClass().getSimpleName()).append('\n');
     describe(window,result,new int[]{35});}
   }
  });
  return result.toString();
 }
 private static void describe(View view,StringBuilder out,int[] remaining){
  if(remaining[0]--<=0)return;
  if(view instanceof TextView){String text=((TextView)view).getText().toString().replace('\n',' ');
   if(!text.isEmpty())out.append(text.length()>100?text.substring(0,100):text).append('\n');}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount()&&remaining[0]>0;i++)describe(group.getChildAt(i),out,remaining);}
 }
}

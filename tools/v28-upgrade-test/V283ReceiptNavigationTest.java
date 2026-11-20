package com.vinayak.badminton.nativepreview;

import android.accessibilityservice.AccessibilityService;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real system Back, delayed index loading and recreation; synthetic ledger only. */
@RunWith(AndroidJUnit4.class)
public final class V283ReceiptNavigationTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String SEARCH="CI Synthetic Member";
 private static final String MONTH="2026-08";
 private Instrumentation instrumentation;
 private Context context;
 private MainActivity activity;

 @Test public void receiptLibraryRestoresFiltersAndReturnsThroughSystemBack()throws Exception {
  instrumentation=InstrumentationRegistry.getInstrumentation();
  context=instrumentation.getTargetContext();
  LedgerDatabase db=syntheticLedger();
  List<QueueHold> holds=new ArrayList<>();
  try {
   String before=canonical(db);
   String status=PremiumV26State.monthStatus(PremiumV26State.feeSummary(
    db.dao().meta().inventoryStockJson,MEMBER,MONTH,MONTH,db.dao().contributions()),
    PremiumV26State.billableMonth(db.dao().meta().inventoryStockJson,MEMBER,MONTH));
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
    scenario.onActivity(value->activity=value);
    try {
     await("Initial screen owns focus",()->activity.hasWindowFocus());
     onMain(()->{
      setField("reportMonth",MONTH);
      invoke("selectTab",new Class<?>[]{String.class},"Members");
     });
     await("Member filter is available",()->tag("member-filter:"+status)!=null);
     onMain(()->((EditText)tag("members:search")).setText(SEARCH));
     click(()->tag("member-filter:"+status),"Synthetic member status");
     await("Filtered member loaded",()->tag("member-photo-action:"+MEMBER)!=null);
     selectTab("Reports");
     onMain(()->((EditText)tag("reports:search")).setText("receipt"));
     click(()->tag("reports:category:Reports"),"Reports category");
     await("Receipt Library report is available",()->reportVisible());

     // The search must survive even when the old index worker has not run yet.
     // A latch makes this deterministic instead of relying on device speed.
     MainActivity original=activity;
     QueueHold delayed=holdQueue();holds.add(delayed);
     click(()->tag("report:RECEIPT LIBRARY · ALL MONTHS"),"Open Receipt Library from Reports");
     await("Receipt search exists before index finishes",()->tag("receipts:search") instanceof EditText);
     onMain(()->{
      assertNotNull("Index is deliberately still loading",findText(decor(),"Reading recorded payments…"));
      ((EditText)tag("receipts:search")).setText(SEARCH);
     });
     scenario.recreate();scenario.onActivity(value->activity=value);
     await("Restored library loads matching payment",()->receiptReady("All"));
     assertNotSame("Recreation creates a new Activity owner",original,activity);
     onMain(()->assertTrue("Previous Activity is destroyed",original.isDestroyed()));
     delayed.releaseAndDrain(true);instrumentation.waitForIdleSync();
     assertReceiptState("All");
     System.out.println("V283_RECEIPT_QUERY_DURING_LOADING_RECREATION_PASS");

     click(()->tag("receipts:filter:Recorded"),"Recorded receipts filter");
     await("Recorded filter selects the fixture receipt",()->receiptReady("Recorded"));
     scenario.recreate();scenario.onActivity(value->activity=value);
     await("Receipt subpage and selected filter survive recreation",()->receiptReady("Recorded"));
     assertReceiptState("Recorded");
     hideKeyboard();capture("premium-receipt-library-restored.png");
     System.out.println("V283_RECEIPT_SUBPAGE_QUERY_AND_FILTER_RECREATION_PASS");

     systemBack();
     await("System Back opens Collect and its actual historical receipt",()->collectReady());
     assertNoReceiptBackHandler();
     scenario.recreate();scenario.onActivity(value->activity=value);
     await("Leaving the library also persists across recreation",()->collectReady());
     assertNoReceiptBackHandler();
     System.out.println("V283_RECEIPT_SYSTEM_BACK_TO_COLLECT_PASS");

     click(()->findText(decor(),"▤  Find or share any past receipt"),"Open Receipt Library from Collect");
     await("Reopened library retains its receipt filters",()->receiptReady("Recorded"));
     hideKeyboard();
     click(()->findText(decor(),"‹ Back to Collect"),"Explicit Back to Collect");
     await("Explicit Back opens Collect",()->collectReady());
     assertNoReceiptBackHandler();
     System.out.println("V283_RECEIPT_EXPLICIT_BACK_TO_COLLECT_PASS");

     // A queued index completion from a library already left must not repaint
     // the active Members screen or revive that library's system Back handler.
     QueueHold departed=holdQueue();holds.add(departed);
     click(()->findText(decor(),"▤  Find or share any past receipt"),"Open delayed Receipt Library");
     selectTab("Members");
     departed.releaseAndDrain(false);instrumentation.waitForIdleSync();
     await("Active Members results load after stale library work",()->tag("member-photo-action:"+MEMBER)!=null);
     onMain(()->{
      assertEquals("Original member search remains",SEARCH,((EditText)tag("members:search")).getText().toString());
      assertTrue("Original member status remains selected",tag("member-filter:"+status).isSelected());
      assertNull("Old library cannot repaint the Members screen",findText(decor(),"Receipt Library"));
     });
     assertNoReceiptBackHandler();
     selectTab("Reports");
     onMain(()->{
      assertEquals("Original report search remains","receipt",((EditText)tag("reports:search")).getText().toString());
      assertTrue("Original report category remains selected",tag("reports:category:Reports").isSelected());
      assertEquals("Original historical month remains",MONTH,field("reportMonth"));
     });
     assertNoReceiptBackHandler();
     System.out.println("V283_RECEIPT_STALE_INDEX_AND_OTHER_SCREEN_STATE_PASS");

     // At a top-level destination the custom callback must be absent: the
     // actual system Back should now leave the app normally. This is last so
     // no relaunch workaround can mask an accidentally retained callback.
     hideKeyboard();systemBack();
     long deadline=SystemClock.elapsedRealtime()+15000;
     while(scenario.getState()==Lifecycle.State.RESUMED&&SystemClock.elapsedRealtime()<deadline)
      Thread.sleep(100);
     assertNotEquals("Top-level system Back retains Android's normal task behavior",
      Lifecycle.State.RESUMED,scenario.getState());
     System.out.println("V283_TOP_LEVEL_SYSTEM_BACK_DEFAULT_BEHAVIOR_PASS");
    }catch(Exception|AssertionError issue){optionalCapture("premium-receipt-navigation-failure.png");throw issue;}
   }
   assertEquals("Receipt search, navigation and recreation preserve every ledger field",before,canonical(db));
   System.out.println("V283_RECEIPT_NAVIGATION_LIFECYCLE_PASS: real platform Back, state restoration, stale index rejection, ledger unchanged");
  }finally {
   for(QueueHold hold:holds)hold.release.countDown();
   db.close();
  }
 }

 private LedgerDatabase syntheticLedger(){
  LedgerDatabase db=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db")
   .allowMainThreadQueries().build();
  try {
   assertNotNull("Synthetic upgrade ledger is required",db.dao().meta());
   assertEquals("Never run navigation tests against a real club ledger",
    "CI Synthetic Badminton Club — no real data",db.dao().meta().club);
   assertEquals("Expected synthetic roster",15,db.dao().members().size());
   boolean found=false;
   for(Member member:db.dao().members())if(MEMBER.equals(member.id)){
    found=true;assertEquals("Immutable fixture identity",NAME,member.name);
   }
   assertTrue("Known synthetic member exists",found);return db;
  }catch(RuntimeException|AssertionError issue){db.close();throw issue;}
 }
 private void assertReceiptState(String mode){
  onMain(()->{
   assertEquals("Receipt Library remains under Collect","Collect",field("tab"));
   assertEquals("Historical month survives receipt recreation",MONTH,field("reportMonth"));
   assertEquals("Receipt query survives recreation",SEARCH,((EditText)tag("receipts:search")).getText().toString());
   assertTrue("Receipt filter is visibly selected",tag("receipts:filter:"+mode).isSelected());
   assertTrue("Collect is visibly selected in navigation",tag("nav:Collect").isSelected());
   assertEquals("Receipt subpage identity is retained",Boolean.TRUE,field("receiptLibraryOpen"));
   if(Build.VERSION.SDK_INT>=33)assertNotNull("Modern system Back callback owns this open library",field("receiptBackCallback"));
  });
 }
 private void assertNoReceiptBackHandler(){
  onMain(()->{
   assertEquals("Library subpage is closed",Boolean.FALSE,field("receiptLibraryOpen"));
   assertNull("Leaving releases only the library Back callback",field("receiptBackCallback"));
  });
 }
 private boolean receiptReady(String mode){
  View search=tag("receipts:search"),filter=tag("receipts:filter:"+mode);
  return findText(decor(),"Receipt Library")!=null&&search instanceof EditText&&
   SEARCH.equals(((EditText)search).getText().toString())&&filter!=null&&filter.isSelected()&&
   findText(decor(),NAME)!=null&&findText(decor(),"No matching cash records")==null;
 }
 private boolean collectReady(){
  return "Collect".equals(field("tab"))&&findText(decor(),"Collect fees")!=null&&
   findText(decor(),NAME)!=null&&activity.hasWindowFocus();
 }
 private boolean reportVisible(){View report=tag("report:RECEIPT LIBRARY · ALL MONTHS");return report!=null&&report.isShown();}
 private void selectTab(String name)throws Exception {
  click(()->tag("nav:"+name),"Navigate to "+name);
  await("Selected destination "+name,()->name.equals(field("tab")));
 }
 private void systemBack(){
  assertTrue("Android accepts the actual system Back action",instrumentation.getUiAutomation()
   .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK));
 }
 private void hideKeyboard()throws Exception {
  onMain(()->{
   View focused=activity.getCurrentFocus();if(focused!=null)focused.clearFocus();
   ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE))
    .hideSoftInputFromWindow(decor().getWindowToken(),0);
  });
  if(Build.VERSION.SDK_INT>=30)await("Keyboard is hidden before testing screen Back",()->{
   WindowInsets insets=decor().getRootWindowInsets();return insets!=null&&!insets.isVisible(WindowInsets.Type.ime());
  });
  twoFrames();
 }
 private QueueHold holdQueue()throws Exception {
  AtomicReference<ExecutorService> executor=new AtomicReference<>();
  onMain(()->executor.set((ExecutorService)field("io")));
  QueueHold hold=new QueueHold(executor.get());
  try {assertTrue("Receipt index worker reached deterministic hold",hold.entered.await(15,TimeUnit.SECONDS));return hold;}
  catch(Throwable failure){hold.release.countDown();throw failure;}
 }
 private static final class QueueHold {
  final ExecutorService executor;
  final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  final AtomicBoolean timedOut=new AtomicBoolean();
  QueueHold(ExecutorService executor){
   this.executor=executor;
   executor.execute(()->{
    entered.countDown();
    try {if(!release.await(45,TimeUnit.SECONDS))timedOut.set(true);}
    catch(InterruptedException issue){Thread.currentThread().interrupt();timedOut.set(true);}
   });
  }
  void releaseAndDrain(boolean destroyed)throws Exception {
   if(destroyed)assertTrue("Recreation closes the old worker to new submissions",executor.isShutdown());
   CountDownLatch drained=new CountDownLatch(1);
   if(!destroyed)executor.execute(drained::countDown);
   release.countDown();
   assertTrue("Previously queued index work completes",destroyed?
    executor.awaitTermination(15,TimeUnit.SECONDS):drained.await(15,TimeUnit.SECONDS));
   assertFalse("Queue was released explicitly, never by timing out",timedOut.get());
  }
 }
 private interface Finder{View find();}
 private void click(Finder finder,String label)throws Exception {
  onMain(()->{
   View target=finder.find();assertNotNull(label,target);
   target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);
  });twoFrames();
  onMain(()->{
   View target=finder.find();assertNotNull(label,target);
   assertTrue(label+" is enabled and visible",target.isEnabled()&&target.isShown());
   assertTrue(label+" dispatches its actual screen callback",target.performClick());
  });
 }
 private View decor(){return activity.getWindow().getDecorView();}
 private View tag(String value){return decor().findViewWithTag(value);}
 private static View findText(View view,String value){
  if(view instanceof TextView&&value.contentEquals(((TextView)view).getText()))return view;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++){View found=findText(group.getChildAt(i),value);if(found!=null)return found;}
  }return null;
 }
 private Object field(String name){
  try {Field value=MainActivity.class.getDeclaredField(name);value.setAccessible(true);return value.get(activity);}
  catch(Exception issue){throw new AssertionError("Cannot inspect "+name,issue);}
 }
 private void setField(String name,Object value){
  try {Field target=MainActivity.class.getDeclaredField(name);target.setAccessible(true);target.set(activity,value);}
  catch(Exception issue){throw new AssertionError("Cannot set synthetic screen state "+name,issue);}
 }
 private void invoke(String name,Class<?>[] types,Object... args){
  try {Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);}
  catch(Exception issue){throw new AssertionError("Cannot invoke "+name,issue);}
 }
 private void await(String label,BooleanSupplier condition)throws Exception {
  long deadline=SystemClock.elapsedRealtime()+15000;AtomicBoolean done=new AtomicBoolean();
  do {onMain(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(100);}
  while(SystemClock.elapsedRealtime()<deadline);fail("Timed out: "+label);
 }
 private void onMain(Runnable work){
  AtomicReference<Throwable> failure=new AtomicReference<>();
  instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable issue){failure.set(issue);}});
  if(failure.get()!=null)throw new AssertionError("UI assertion failed",failure.get());
 }
 private void twoFrames()throws Exception {
  CountDownLatch drawn=new CountDownLatch(1);
  onMain(()->Choreographer.getInstance().postFrameCallback(first->
   Choreographer.getInstance().postFrameCallback(second->drawn.countDown())));
  assertTrue("Two UI frames draw",drawn.await(5,TimeUnit.SECONDS));instrumentation.waitForIdleSync();
 }
 private void capture(String name)throws Exception {
  CountDownLatch committed=new CountDownLatch(1);
  onMain(()->{
   ScrollView scroll=(ScrollView)field("pageScroll");scroll.scrollTo(0,0);
   if(Build.VERSION.SDK_INT>=29)decor().getViewTreeObserver().registerFrameCommitCallback(committed::countDown);
   else committed.countDown();
   decor().invalidate();
  });twoFrames();assertTrue("Receipt screenshot frame is committed",committed.await(5,TimeUnit.SECONDS));
  Bitmap image=instrumentation.getUiAutomation().takeScreenshot();assertNotNull("Actual Receipt Library screenshot",image);
  try {
   File folder=context.getExternalFilesDir("premium-ui-evidence");assertNotNull("Evidence location",folder);
   assertTrue("Evidence directory",folder.isDirectory()||folder.mkdirs());
   try(FileOutputStream output=new FileOutputStream(new File(folder,name))){assertTrue("Screenshot encodes",image.compress(Bitmap.CompressFormat.PNG,100,output));}
  }finally{image.recycle();}
 }
 private void optionalCapture(String name){try{capture(name);}catch(Exception|AssertionError issue){System.out.println("Receipt failure screenshot unavailable: "+issue.getClass().getSimpleName());}}
 private static String canonical(LedgerDatabase db)throws Exception {
  return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
 }
}

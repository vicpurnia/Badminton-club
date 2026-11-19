package com.vinayak.badminton.nativepreview;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.Layout;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inspector.WindowInspector;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Synthetic-only checks for accessibility, recreation and enlarged native text. */
@RunWith(AndroidJUnit4.class)
public final class V283AccessibilityStateTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String MONTH="2026-08";
 private static final String SEARCH="CI Synthetic Member";
 private static final String[] TABS={"Home","Members","Collect","Shuttles","Reports"};
 private Instrumentation instrumentation;
 private Context context;
 private MainActivity activity;

 @Test public void accessibilityAndScreenStateSurviveRecreation()throws Exception {
  init();LedgerDatabase db=syntheticLedger();
  try {
   String before=canonical(db);
   String status=PremiumV26State.monthStatus(PremiumV26State.feeSummary(
    db.dao().meta().inventoryStockJson,MEMBER,MONTH,MONTH,db.dao().contributions()),
    PremiumV26State.billableMonth(db.dao().meta().inventoryStockJson,MEMBER,MONTH));
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
    scenario.onActivity(value->activity=value);
    await("Initial screen owns focus",()->activity.hasWindowFocus());
    twoFrames();
    assertMonthAccessibility();
    chooseHistoricalMonth();
    selectTab("Members");
    await("Member search is available",()->tag("members:search") instanceof EditText);
    onMain(()->((EditText)tag("members:search")).setText(SEARCH));
    click(()->tag("member-filter:"+status),"Member status "+status);
    await("Filtered member has loaded",()->tag("member-photo-action:"+MEMBER)!=null);
    scenario.recreate();scenario.onActivity(value->activity=value);
    await("Restored member result is available",()->tag("member-photo-action:"+MEMBER)!=null);
    twoFrames();
    onMain(()->{
     assertEquals("Members tab survives recreation","Members",field("tab"));
     assertEquals("Historical month survives recreation",MONTH,field("reportMonth"));
     assertEquals("Search survives recreation",SEARCH,((EditText)tag("members:search")).getText().toString());
     assertEquals("Status survives recreation",status,field("memberFilter"));
     assertTrue("Restored status is visually selected",tag("member-filter:"+status).isSelected());
     assertNavigation("Members");
    });
    assertMonthAccessibility();
    System.out.println("V283_MEMBER_SCREEN_RECREATION_PASS");

    selectTab("Reports");
    await("Report catalogue exists",()->tag("reports:search") instanceof EditText);
    onMain(()->((EditText)tag("reports:search")).setText("encrypted"));
    click(()->tag("reports:category:Backups"),"Backups category");
    await("Report filter has applied",()->reportShown("ENCRYPTED BACKUP · OPTIONAL")&&!reportShown("Monthly Cashbook"));
    scenario.recreate();scenario.onActivity(value->activity=value);
    await("Restored report catalogue exists",()->tag("reports:search") instanceof EditText);
    twoFrames();
    onMain(()->{
     assertEquals("Reports tab survives recreation","Reports",field("tab"));
     assertEquals("Reports retain historical month",MONTH,field("reportMonth"));
     assertEquals("Report query survives recreation","encrypted",((EditText)tag("reports:search")).getText().toString());
     assertTrue("Backups category survives recreation",tag("reports:category:Backups").isSelected());
     assertTrue("Matching encrypted action remains",reportShown("ENCRYPTED BACKUP · OPTIONAL"));
     assertFalse("Nonmatching cashbook stays filtered",reportShown("Monthly Cashbook"));
     assertNavigation("Reports");
    });
    screenshot("premium-restored-reports.png");
    assertEquals("Navigation and recreation cannot alter ledger",before,canonical(db));
    System.out.println("V283_ACCESSIBILITY_AND_SCREEN_STATE_PASS");
   }catch(Exception|AssertionError issue){screenshot("premium-state-failure.png");throw issue;}
  }finally{db.close();}
 }

 @Test public void largeTextOnNarrowScreenKeepsActionsReachable()throws Exception {
  init();LedgerDatabase db=syntheticLedger();
  try {
   String before=canonical(db),receiptId=null;
   for(Contribution receipt:db.dao().contributions())
    if(MEMBER.equals(receipt.memberId)&&MONTH.equals(receipt.feeMonth)&&"cash".equals(receipt.kind)){
     assertEquals(1379L,receipt.amount);receiptId=receipt.id;break;
    }
   assertNotNull("Known positive receipt is required",receiptId);
   final String cashId=receiptId;
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
    scenario.onActivity(value->activity=value);
    onMain(()->{
     assertTrue("Runner must enable 200% fonts",activity.getResources().getConfiguration().fontScale>=1.95f);
     int width=activity.getResources().getConfiguration().screenWidthDp;
     assertTrue("Runner must use a narrow 320dp screen, actual="+width,width>=310&&width<=330);
    });
    await("Narrow screen owns focus",()->activity.hasWindowFocus());
    chooseHistoricalMonth();
    for(String destination:TABS){
     selectTab(destination);
     await("Loaded narrow "+destination,()->screenReady(destination));
     twoFrames();
     onMain(()->assertNavigation(destination));
     screenshot("premium-large-text-"+destination.toLowerCase(java.util.Locale.ROOT)+".png");
    }
    selectTab("Members");
    await("Member name is ready",()->tag("member-name:"+MEMBER)!=null);
    scroll(()->tag("member-name:"+MEMBER));
    onMain(()->{
     TextView name=(TextView)tag("member-name:"+MEMBER);
     assertEquals("Member identity is readable",NAME,name.getText().toString());
     assertVisible(name,"Enlarged member name");assertTextFits(name,"Enlarged member name");
    });
    scroll(()->tag("member-photo-action:"+MEMBER));
    onMain(()->{
     View photo=tag("member-photo-action:"+MEMBER);
     assertVisible(photo,"Member photo action");assertTouch(photo,"Member photo action");
     assertTextFits((TextView)photo,"Member photo action");
     assertTrue("Photo action retains complete member name",photo.getContentDescription().toString().contains(NAME));
    });
    screenshot("premium-large-text-member-card.png");
    checkProfileWithKeyboard();

    selectTab("Collect");
    await("Cash receipt footer is available",()->tag("receipt-action:"+cashId)!=null);
    scroll(()->tag("receipt-amount:"+cashId));
    onMain(()->{
     TextView amount=(TextView)tag("receipt-amount:"+cashId);
     assertEquals("Actual receipt money remains readable","₹1,379",amount.getText().toString());
     assertVisible(amount,"Receipt amount");assertTextFits(amount,"Receipt amount");
    });
    scroll(()->tag("receipt-action:"+cashId));
    onMain(()->{
     TextView action=(TextView)tag("receipt-action:"+cashId);
     assertVisible(action,"Receipt action");assertTextFits(action,"Receipt action");assertTouch(action,"Receipt action");
    });
    screenshot("premium-large-text-receipt.png");
    assertEquals("Large-text navigation and canceled editing preserve every ledger field",before,canonical(db));
    System.out.println("V283_NARROW_320DP_LARGE_TEXT_200_PERCENT_PASS");
   }catch(Exception|AssertionError issue){screenshot("premium-large-text-failure.png");throw issue;}
  }finally{db.close();}
 }

 private void init(){instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();}
 private LedgerDatabase syntheticLedger(){
  LedgerDatabase db=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db").allowMainThreadQueries().build();
  try {
   assertNotNull("Synthetic ledger is required",db.dao().meta());
   assertTrue("Never run this test on a real club ledger",db.dao().meta().club.startsWith("CI Synthetic Badminton Club"));
   assertEquals("Expected synthetic roster",15,db.dao().members().size());
   boolean found=false;for(Member member:db.dao().members())if(MEMBER.equals(member.id)){found=true;assertEquals(NAME,member.name);}
   assertTrue("Known synthetic member must exist",found);return db;
  }catch(RuntimeException|AssertionError issue){db.close();throw issue;}
 }
 private void chooseHistoricalMonth()throws Exception {
  click(()->findDescriptionPrefix(decor(),"Choose month, "),"Choose month");
  await("Native month picker",()->datePickerWindow()!=null);
  onMain(()->{
   DatePicker picker=findType(datePickerWindow(),DatePicker.class);
   assertNotNull("Native date picker is present",picker);picker.updateDate(2026,7,1);
  });
  click(()->datePickerWindow().findViewById(android.R.id.button1),"Confirm historical month");
  await("Historical month selected",()->MONTH.equals(field("reportMonth"))&&datePickerWindow()==null);
  twoFrames();
 }
 private void checkProfileWithKeyboard()throws Exception {
  click(()->clickable(tag("member-name:"+MEMBER)),"Open member history");
  await("Member history",()->dialog("Complete recorded member history")!=null);
  click(()->findText(requireDialog("Complete recorded member history"),"Edit member profile / archive safely"),"Edit member profile");
  await("Profile form",()->dialog("Member profile · audited")!=null);
  await("Profile owns keyboard focus",()->requireDialog("Member profile · audited").hasWindowFocus());
  scroll(()->findHint(requireDialog("Member profile · audited"),"Optional notes"));
  onMain(()->{
   EditText notes=(EditText)findHint(requireDialog("Member profile · audited"),"Optional notes");
   assertNotNull("Notes field exists",notes);notes.requestFocus();
   notes.setText("Canceled UI check");
   InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
   assertNotNull("System keyboard service",keyboard);keyboard.showSoftInput(notes,InputMethodManager.SHOW_IMPLICIT);
  });
  await("Actual software keyboard is visible",()->{
   WindowInsets insets=requireDialog("Member profile · audited").getRootWindowInsets();
   return insets!=null&&insets.isVisible(WindowInsets.Type.ime());
  });
  scroll(()->findHint(requireDialog("Member profile · audited"),"Optional notes"));
  onMain(()->assertVisible(findHint(requireDialog("Member profile · audited"),"Optional notes"),"Notes above keyboard"));
  scroll(()->findText(requireDialog("Member profile · audited"),"Active club member"));
  onMain(()->{
   TextView active=(TextView)findText(requireDialog("Member profile · audited"),"Active club member");
   assertVisible(active,"Active member control above keyboard");assertTextFits(active,"Active member control");
  });
  screenshot("premium-large-text-profile-keyboard.png");
  click(()->requireDialog("Member profile · audited").findViewById(android.R.id.button2),"Cancel profile edit");
  await("Profile cancellation closes form",()->dialog("Member profile · audited")==null);
  twoFrames();
  System.out.println("V283_NARROW_PROFILE_KEYBOARD_REACHABILITY_PASS");
 }
 private void assertMonthAccessibility()throws Exception {
  AtomicReference<String> initialMonth=new AtomicReference<>();
  onMain(()->initialMonth.set((String)field("reportMonth")));
  for(String label:new String[]{"Previous month","Next month"}){
   scroll(()->findDescriptionPrefix(decor(),label));
   onMain(()->{
    View arrow=findDescriptionPrefix(decor(),label);
    assertNotNull(label+" exists",arrow);assertTrue(label+" participates in accessibility",arrow.isImportantForAccessibility());
    assertVisible(arrow,label);assertTouch(arrow,label);
   });
   AtomicReference<String> beforeMonth=new AtomicReference<>();
   onMain(()->beforeMonth.set((String)field("reportMonth")));
   String expected=YearMonth.parse(beforeMonth.get()).plusMonths("Previous month".equals(label)?-1:1).toString();
   long deadline=SystemClock.elapsedRealtime()+12000;boolean found=false;
   do{
    AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
    try{found=root!=null&&performAccessibleAction(root,label);}finally{if(root!=null)root.recycle();}
    if(found)break;Thread.sleep(100);
   }while(SystemClock.elapsedRealtime()<deadline);
   assertTrue(label+" is exposed as an actionable Android accessibility node",found);
   await(label+" changes the displayed month",()->expected.equals(field("reportMonth")));
   twoFrames();
  }
  onMain(()->assertEquals("Previous/next accessible actions return to starting month",initialMonth.get(),field("reportMonth")));
  System.out.println("V283_MONTH_ARROWS_ACCESSIBLE_PASS");
 }
 private static boolean performAccessibleAction(AccessibilityNodeInfo node,String label){
  if(label.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))
   return node.isVisibleToUser()&&node.isEnabled()&&node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
  for(int i=0;i<node.getChildCount();i++){
   AccessibilityNodeInfo child=node.getChild(i);if(child==null)continue;
   try{if(performAccessibleAction(child,label))return true;}finally{child.recycle();}
  }return false;
 }
 private void selectTab(String name)throws Exception {
  click(()->tag("nav:"+name),"Navigate to "+name);await("Selected tab "+name,()->name.equals(field("tab")));
 }
 private void assertNavigation(String selected){
  int count=0;for(String name:TABS){
   View navigation=tag("nav:"+name);assertNotNull(name+" navigation exists",navigation);
   assertVisible(navigation,name+" navigation");assertTouch(navigation,name+" navigation");
   assertEquals(name+" selected state",name.equals(selected),navigation.isSelected());if(navigation.isSelected())count++;
   TextView label=(TextView)findText(navigation,name);assertNotNull(name+" label",label);
   assertVisible(label,name+" label");assertTextFits(label,name+" label");
  }assertEquals("Exactly one selected navigation destination",1,count);
 }
 private void assertTouch(View view,String label){
  assertTrue(label+" is enabled and clickable",view.isEnabled()&&view.isClickable());
  int minimum=(int)(48*activity.getResources().getDisplayMetrics().density+.5f);
  assertTrue(label+" is at least48dp tall",view.getHeight()>=minimum);
  assertTrue(label+" is at least48dp wide",view.getWidth()>=minimum);
 }
 private void assertVisible(View view,String label){
  assertNotNull(label,view);assertTrue(label+" has measured bounds",view.getWidth()>0&&view.getHeight()>0);
  Rect visible=new Rect();assertTrue(label+" is on-screen",view.isShown()&&view.getGlobalVisibleRect(visible));
  assertEquals(label+" width is not clipped",view.getWidth(),visible.width());
  assertEquals(label+" height is not clipped",view.getHeight(),visible.height());
  Rect usable=new Rect();view.getWindowVisibleDisplayFrame(usable);
  int[] screenPosition=new int[2];view.getLocationOnScreen(screenPosition);
  Rect screenBounds=new Rect(screenPosition[0],screenPosition[1],screenPosition[0]+view.getWidth(),screenPosition[1]+view.getHeight());
  assertTrue(label+" is above system bars/keyboard: "+screenBounds+" / "+usable,usable.contains(screenBounds));
 }
 private static void assertTextFits(TextView view,String label){
  Layout layout=view.getLayout();assertNotNull(label+" text has a layout",layout);
  assertTrue(label+" has readable content",view.getText().length()>0&&layout.getLineCount()>0);
  int height=view.getHeight()-view.getCompoundPaddingTop()-view.getCompoundPaddingBottom();
  assertTrue(label+" text height fits: "+layout.getHeight()+" / "+height,layout.getHeight()<=height+1);
  for(int line=0;line<layout.getLineCount();line++){
   assertEquals(label+" must not hide text with ellipsis",0,layout.getEllipsisCount(line));
   assertTrue(label+" line is not horizontally clipped",layout.getLineLeft(line)>=-1&&layout.getLineRight(line)<=layout.getWidth()+1);
  }
  assertEquals(label+" lays out its full text",view.getText().length(),layout.getLineEnd(layout.getLineCount()-1));
 }
 private boolean screenReady(String selected){
  if("Home".equals(selected))return findText(decor(),NAME)!=null;
  if("Members".equals(selected))return tag("member-photo-action:"+MEMBER)!=null;
  if("Collect".equals(selected))return findText(decor(),NAME)!=null;
  if("Shuttles".equals(selected))return findText(decor(),"Shuttle purchases (1)")!=null;
  return tag("report:Verified account export")!=null;
 }
 private boolean reportShown(String title){View view=tag("report:"+title);return view!=null&&view.isShown();}
 private interface Finder{View find();}
 private void scroll(Finder finder)throws Exception {
  onMain(()->{
   View target=finder.find();assertNotNull("Scrollable target exists",target);
   target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);
  });twoFrames();
 }
 private void click(Finder finder,String label)throws Exception {
  scroll(finder);
  onMain(()->{View target=finder.find();assertNotNull(label,target);assertTrue(label+" enabled",target.isEnabled());
   assertTrue(label+" dispatches through real control",target.performClick());});
 }
 private View decor(){return activity.getWindow().getDecorView();}
 private View tag(String tag){return decor().findViewWithTag(tag);}
 private Object field(String name){
  try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
  catch(Exception issue){throw new AssertionError("Cannot inspect "+name,issue);}
 }
 private View requireDialog(String title){View view=dialog(title);assertNotNull("Dialog "+title,view);return view;}
 private View datePickerWindow(){
  List<View> windows=WindowInspector.getGlobalWindowViews();
  for(int i=windows.size()-1;i>=0;i--){View view=windows.get(i);
   if(view!=decor()&&view.isAttachedToWindow()&&view.isShown()&&findType(view,DatePicker.class)!=null)return view;}
  return null;
 }
 private View dialog(String title){
  List<View> windows=WindowInspector.getGlobalWindowViews();
  for(int i=windows.size()-1;i>=0;i--){View view=windows.get(i);
   if(view!=decor()&&view.isAttachedToWindow()&&view.isShown()&&findText(view,title)!=null)return view;}
  return null;
 }
 private static View findText(View root,String value){
  if(root instanceof TextView&&value.equalsIgnoreCase(((TextView)root).getText().toString()))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){
   View found=findText(group.getChildAt(i),value);if(found!=null)return found;}}return null;
 }
 private static View findDescriptionPrefix(View root,String prefix){
  if(root.getContentDescription()!=null&&root.getContentDescription().toString().startsWith(prefix))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){
   View found=findDescriptionPrefix(group.getChildAt(i),prefix);if(found!=null)return found;}}return null;
 }
 private static View findHint(View root,String hint){
  if(root instanceof EditText&&hint.contentEquals(((EditText)root).getHint()==null?"":((EditText)root).getHint()))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){
   View found=findHint(group.getChildAt(i),hint);if(found!=null)return found;}}return null;
 }
 private static <T extends View>T findType(View root,Class<T> type){
  if(type.isInstance(root))return type.cast(root);
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){
   T found=findType(group.getChildAt(i),type);if(found!=null)return found;}}return null;
 }
 private static View clickable(View view){for(View node=view;node!=null;node=node.getParent() instanceof View?(View)node.getParent():null)if(node.isClickable())return node;return null;}
 private void await(String label,BooleanSupplier condition)throws Exception {
  long deadline=SystemClock.elapsedRealtime()+15000;AtomicBoolean done=new AtomicBoolean();
  do{onMain(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(100);}while(SystemClock.elapsedRealtime()<deadline);
  fail("Timed out: "+label);
 }
 private void onMain(Runnable work){
  AtomicReference<Throwable> issue=new AtomicReference<>();instrumentation.runOnMainSync(()->{
   try{work.run();}catch(Throwable failure){issue.set(failure);}});
  if(issue.get()!=null)throw new AssertionError("UI assertion failed",issue.get());
 }
 private void twoFrames()throws Exception {
  CountDownLatch frames=new CountDownLatch(1);onMain(()->Choreographer.getInstance().postFrameCallback(first->
   Choreographer.getInstance().postFrameCallback(second->frames.countDown())));
  assertTrue("UI draws two frames",frames.await(5,TimeUnit.SECONDS));instrumentation.waitForIdleSync();
 }
 private static String canonical(LedgerDatabase db)throws Exception{return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);}
 private void screenshot(String filename){
  Bitmap image=null;
  try{
   image=instrumentation.getUiAutomation().takeScreenshot();if(image==null)return;
   File directory=context.getExternalFilesDir("premium-ui-evidence");
   if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))return;
   try(FileOutputStream output=new FileOutputStream(new File(directory,filename))){image.compress(Bitmap.CompressFormat.PNG,100,output);}
  }catch(Exception issue){System.out.println("Optional accessibility screenshot unavailable: "+issue.getClass().getSimpleName());}
  finally{if(image!=null)image.recycle();}
 }
}

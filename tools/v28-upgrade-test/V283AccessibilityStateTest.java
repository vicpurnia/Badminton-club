package com.vinayak.badminton.nativepreview;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.Layout;
import android.view.Choreographer;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inspector.WindowInspector;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
 private static final String PROFILE="Member profile · audited";
 private static final String PROFILE_REVIEW="Save member profile?";
 private static final String[] TABS={"Home","Members","Collect","Shuttles","Reports"};
 private Instrumentation instrumentation;
 private Context context;
 private MainActivity activity;
 private int profileKeyboardAttempts,tapAttempts;

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
    captureEvidence("premium-restored-reports.png",()->tag("reports:search"));
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
    drainPriorToasts();
    chooseHistoricalMonth();
    for(String destination:TABS){
     selectTab(destination);
     await("Loaded narrow "+destination,()->screenReady(destination));
     twoFrames();
     onMain(()->{assertNavigation(destination);assertCompactBrand();});
     captureEvidence("premium-large-text-"+destination.toLowerCase(java.util.Locale.ROOT)+".png",()->tag("brand:title"));
    }
    selectTab("Members");
    await("Member name is ready",()->tag("member-name:"+MEMBER)!=null);
    scroll(()->tag("member-name:"+MEMBER));
    onMain(()->{
     TextView name=(TextView)tag("member-name:"+MEMBER);
     assertEquals("Member identity is readable",NAME,name.getText().toString());
     assertVisible(name,"Enlarged member name");assertTextFits(name,"Enlarged member name");
    });
    captureEvidence("premium-large-text-member-name.png",()->tag("member-name:"+MEMBER));
    scroll(()->tag("member-photo-action:"+MEMBER));
    onMain(()->{
     View photo=tag("member-photo-action:"+MEMBER);
     assertVisible(photo,"Member photo action");assertTouch(photo,"Member photo action");
     assertTextFits((TextView)photo,"Member photo action");
     assertTrue("Photo action retains complete member name",photo.getContentDescription().toString().contains(NAME));
    });
    captureEvidence("premium-large-text-member-card.png",()->tag("member-photo-action:"+MEMBER));
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
    captureEvidence("premium-large-text-receipt.png",()->tag("receipt-action:"+cashId));
    assertEquals("Large-text navigation and canceled editing preserve every ledger field",before,canonical(db));
    System.out.println("V283_NARROW_320DP_LARGE_TEXT_200_PERCENT_PASS");
   }catch(Exception|AssertionError issue){screenshot("premium-large-text-failure.png");throw issue;}
  }finally{db.close();}
 }

 private void init(){
  instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();
  AccessibilityServiceInfo service=instrumentation.getUiAutomation().getServiceInfo();
  assertNotNull("UI automation accessibility service",service);
  service.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
  instrumentation.getUiAutomation().setServiceInfo(service);
 }
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
  await("Profile form",()->dialog(PROFILE)!=null);
  AtomicReference<View> originalForm=new AtomicReference<>();
  AtomicReference<EditText> originalNotes=new AtomicReference<>();
  final String draft="Canceled UI check";
  onMain(()->{
   View profile=requireDialog(PROFILE);originalForm.set(profile);
   EditText notes=(EditText)findHint(profile,"Optional notes");
   assertNotNull("Notes field exists",notes);originalNotes.set(notes);notes.setText(draft);
  });
  showProfileKeyboard();
  scroll(()->findHint(requireDialog(PROFILE),"Optional notes"));
  onMain(()->{
   TextView notes=(TextView)findHint(requireDialog(PROFILE),"Optional notes");
   assertVisible(notes,"Notes above keyboard");assertTextFits(notes,"Notes above keyboard");
  });
  captureProfileKeyboardEvidence("premium-large-text-profile-keyboard-notes.png",()->originalNotes.get());
  scroll(()->findText(requireDialog(PROFILE),"Active club member"));
  onMain(()->{
   TextView active=(TextView)findText(requireDialog(PROFILE),"Active club member");
   assertVisible(active,"Active member control above keyboard");assertTextFits(active,"Active member control");
  });
  captureProfileKeyboardEvidence("premium-large-text-profile-keyboard.png",()->findText(requireDialog(PROFILE),"Active club member"));
  tap(()->requireDialog(PROFILE).findViewById(android.R.id.button1),"Review profile above keyboard",true);
  await("Coordinate tap opens profile review",()->dialog(PROFILE_REVIEW)!=null&&requireDialog(PROFILE_REVIEW).hasWindowFocus());
  tap(()->requireDialog(PROFILE_REVIEW).findViewById(android.R.id.button2),"Cancel profile review",false,
   "premium-large-text-profile-review-ready.png");
  await("Cancel review returns to profile form",()->dialog(PROFILE_REVIEW)==null&&dialog(PROFILE)!=null&&requireDialog(PROFILE).hasWindowFocus());
  onMain(()->{
   assertSame("Cancel review retains the original form",originalForm.get(),requireDialog(PROFILE));
   assertSame("Cancel review retains the original notes field",originalNotes.get(),findHint(requireDialog(PROFILE),"Optional notes"));
   assertEquals("Cancel review retains the unsaved draft",draft,originalNotes.get().getText().toString());
  });
  showProfileKeyboard();
  captureProfileKeyboardEvidence("premium-large-text-profile-keyboard-retained.png",()->requireDialog(PROFILE).findViewById(android.R.id.button2));
  tap(()->requireDialog(PROFILE).findViewById(android.R.id.button2),"Cancel original profile above keyboard",true);
  await("Coordinate tap cancels original profile form",()->dialog(PROFILE)==null&&dialog(PROFILE_REVIEW)==null);
  twoFrames();
  System.out.println("V283_NARROW_PROFILE_KEYBOARD_REACHABILITY_PASS");
 }
 private void showProfileKeyboard()throws Exception {
  final int attempt=++profileKeyboardAttempts;
  AtomicReference<View> observedProfile=new AtomicReference<>();
  AtomicReference<ViewTreeObserver> observer=new AtomicReference<>();
  AtomicLong layoutCount=new AtomicLong(),lastLayoutAt=new AtomicLong();
  ViewTreeObserver.OnGlobalLayoutListener layouts=()->{
   layoutCount.incrementAndGet();lastLayoutAt.set(SystemClock.elapsedRealtime());
  };
  AtomicReference<String> lastObservation=new AtomicReference<>("Keyboard wait not started");
  AtomicReference<String> previous=new AtomicReference<>();AtomicLong stableSince=new AtomicLong();
  try{
   await("Profile owns keyboard focus",()->requireDialog(PROFILE).hasWindowFocus());
   onMain(()->{
    View profile=requireDialog(PROFILE);observedProfile.set(profile);
    ViewTreeObserver tree=profile.getViewTreeObserver();observer.set(tree);tree.addOnGlobalLayoutListener(layouts);
   });
   scroll(()->findHint(requireDialog(PROFILE),"Optional notes"));
   onMain(()->assertTrue("Notes accepts keyboard focus",findHint(requireDialog(PROFILE),"Optional notes").requestFocus()));
   twoFrames();
   onMain(()->{
    InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    assertNotNull("System keyboard service",keyboard);
    keyboard.showSoftInput(findHint(requireDialog(PROFILE),"Optional notes"),InputMethodManager.SHOW_IMPLICIT);
   });
   await("Visible keyboard and both complete profile actions have stable bounds",()->{
    View profile=requireDialog(PROFILE);WindowInsets insets=profile.getRootWindowInsets();
    View review=profile.findViewById(android.R.id.button1),cancel=profile.findViewById(android.R.id.button2);
    Rect imeBounds=inputMethodWindowBounds();
    boolean focused=profile.hasWindowFocus(),ime=imeVisible(insets)&&validInputMethodBounds(imeBounds);
    boolean reviewVisible=fullyVisible(review),cancelVisible=fullyVisible(cancel);
    boolean actionsAboveIme=ime&&review!=null&&cancel!=null&&
     screenBounds(review).bottom<=imeBounds.top&&screenBounds(cancel).bottom<=imeBounds.top;
    lastObservation.set("profileWindowFocus="+focused+"; imePredicate="+ime+
     "; dialogImeVisible="+imeVisible(insets)+"; inputMethodScreenBounds="+imeBounds+
     "; reviewFullyVisible="+reviewVisible+"; cancelFullyVisible="+cancelVisible+"; actionsAboveIme="+actionsAboveIme);
    if(!focused||!ime||!reviewVisible||!cancelVisible||!actionsAboveIme){previous.set(null);return false;}
    Rect usable=new Rect();profile.getWindowVisibleDisplayFrame(usable);
    String geometry=usable+" / "+screenBounds(review)+" / "+screenBounds(cancel)+" / imeScreen="+imeBounds;
    if(!geometry.equals(previous.get())){previous.set(geometry);stableSince.set(SystemClock.elapsedRealtime());return false;}
    return SystemClock.elapsedRealtime()-stableSince.get()>=250;
   });
   twoFrames();onMain(()->assertProfileActionsAboveKeyboard(requireDialog(PROFILE)));
  }catch(Exception|AssertionError issue){
   // Capture here: ActivityScenario.close() removes the dialog before the
   // outer test catch can take its otherwise blank failure screenshot.
   captureProfileKeyboardFailure(attempt,issue,observedProfile.get(),lastObservation.get(),
    previous.get(),stableSince.get(),layoutCount.get(),lastLayoutAt.get());
   throw issue;
  }finally{
   try{onMain(()->{
    ViewTreeObserver watched=observer.get();
    if(watched!=null&&watched.isAlive())watched.removeOnGlobalLayoutListener(layouts);
    View profile=observedProfile.get();
    if(profile!=null){ViewTreeObserver current=profile.getViewTreeObserver();
     if(current!=watched&&current.isAlive())current.removeOnGlobalLayoutListener(layouts);}
   });}catch(Exception|AssertionError cleanupIssue){System.out.println("Optional keyboard observer cleanup: "+cleanupIssue);}
  }
 }
 private void captureProfileKeyboardFailure(int attempt,Throwable failure,View observedProfile,
   String predicates,String stableGeometry,long stableSince,long layoutCount,long lastLayoutAt){
  String filename="premium-large-text-profile-keyboard-failure-"+attempt+".png";
  StringBuilder record=new StringBuilder("file=").append(filename).append("\nfailure=").append(failure)
   .append("\nelapsedRealtime=").append(SystemClock.elapsedRealtime()).append("\nlastWaitPredicates=").append(predicates)
   .append("\nlastStableGeometry=").append(stableGeometry).append("\nstableSince=").append(stableSince)
   .append("\nobservedGlobalLayouts=").append(layoutCount).append("\nlastGlobalLayoutAt=").append(lastLayoutAt);
  try{onMain(()->{
   record.append("\nfontScale=").append(activity.getResources().getConfiguration().fontScale)
    .append("\nwidthDp=").append(activity.getResources().getConfiguration().screenWidthDp)
    .append("\ndensity=").append(activity.getResources().getDisplayMetrics().density)
    .append("\nactivityFinishing=").append(activity.isFinishing()).append("; activityDestroyed=").append(activity.isDestroyed())
    .append("\nactivityCurrentWindowBounds=").append(activity.getWindowManager().getCurrentWindowMetrics().getBounds())
    .append("\nactivityMaximumWindowBounds=").append(activity.getWindowManager().getMaximumWindowMetrics().getBounds());
   appendKeyboardWindow(record,"activity",decor());
   View current=dialog(PROFILE),profile=current==null?observedProfile:current;
   record.append("\nprofileFoundByTitle=").append(current!=null).append("; sameObservedProfile=").append(current==observedProfile);
   appendKeyboardWindow(record,"profile",profile);
   if(profile!=null){
    appendKeyboardAncestors(record,"review",profile.findViewById(android.R.id.button1));
    appendKeyboardAncestors(record,"cancel",profile.findViewById(android.R.id.button2));
    appendKeyboardAncestors(record,"notes",findHint(profile,"Optional notes"));
    appendKeyboardView(record,"active",findText(profile,"Active club member"));
    appendKeyboardScrollViews(record,profile);
   }
   List<View> windows=WindowInspector.getGlobalWindowViews();record.append("\nglobalWindowCount=").append(windows.size());
   for(int index=0;index<windows.size();index++)appendKeyboardWindow(record,"window["+index+"]",windows.get(index));
   appendAccessibilityWindows(record);
  });}catch(Exception|AssertionError diagnosticIssue){record.append("\ngeometryCaptureIssue=").append(diagnosticIssue);}
  writeFailureEvidence(filename,record,"V283_KEYBOARD_FAILURE");
 }
 private void writeFailureEvidence(String filename,StringBuilder record,String prefix){
  screenshot(filename);
  try{
   File directory=context.getExternalFilesDir("premium-ui-evidence");
   if(directory!=null&&(directory.isDirectory()||directory.mkdirs()))
    try(FileOutputStream output=new FileOutputStream(new File(directory,filename+".txt"))){
     output.write(record.append('\n').toString().getBytes(StandardCharsets.UTF_8));
    }
  }catch(Exception diagnosticIssue){System.out.println("Optional failure geometry write: "+diagnosticIssue);}
  for(String line:record.toString().split("\n"))System.out.println(prefix+" "+line);
 }
 private void appendKeyboardWindow(StringBuilder record,String label,View window){
  appendKeyboardView(record,label,window);if(window==null)return;
  WindowInsets insets=window.getRootWindowInsets();
  record.append('\n').append(label).append(".rootInsets=").append(insets);
  if(insets!=null){
   int bottom=insets.getInsets(WindowInsets.Type.ime()).bottom;
   record.append('\n').append(label).append(".imeVisible=").append(insets.isVisible(WindowInsets.Type.ime()))
    .append("; imeBottom=").append(bottom).append("; imePredicate=").append(imeVisible(insets))
    .append("; activityBottomMinusIme=").append(activity.getWindowManager().getCurrentWindowMetrics().getBounds().bottom-bottom)
    .append("; windowScreenBottomMinusIme=").append(screenBounds(window).bottom-bottom)
    .append("; systemBarInsets=").append(insets.getInsets(WindowInsets.Type.systemBars()));
  }
  if(window.getLayoutParams() instanceof WindowManager.LayoutParams){
   WindowManager.LayoutParams params=(WindowManager.LayoutParams)window.getLayoutParams();
   record.append('\n').append(label).append(".windowParams=").append(params)
    .append("; softInputMode=").append(params.softInputMode).append("; flags=").append(params.flags)
    .append("; gravity=").append(params.gravity);
  }
  appendKeyboardView(record,label+".focusedView",window.findFocus());
 }
 private void appendKeyboardAncestors(StringBuilder record,String label,View view){
  if(view==null){appendKeyboardView(record,label,null);return;}
  int depth=0;
  for(View node=view;node!=null&&depth<12;node=node.getParent() instanceof View?(View)node.getParent():null)
   appendKeyboardView(record,label+".ancestor["+(depth++)+"]",node);
 }
 private void appendKeyboardScrollViews(StringBuilder record,View view){
  if(view instanceof ScrollView){
   appendKeyboardView(record,"scrollBody",view);
   ScrollView scroll=(ScrollView)view;
   if(scroll.getChildCount()>0)appendKeyboardView(record,"scrollBody.content",scroll.getChildAt(0));
  }
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;
   for(int index=0;index<group.getChildCount();index++)appendKeyboardScrollViews(record,group.getChildAt(index));}
 }
 private void appendKeyboardView(StringBuilder record,String label,View view){
  record.append('\n').append(label).append('=');if(view==null){record.append("missing");return;}
  Rect global=new Rect(),local=new Rect(),usable=new Rect();
  boolean globallyVisible=view.getGlobalVisibleRect(global),locallyVisible=view.getLocalVisibleRect(local);
  view.getWindowVisibleDisplayFrame(usable);int[] position=new int[2];view.getLocationInWindow(position);
  record.append(view.getClass().getName()).append("; id=").append(view.getId())
   .append("; screenBounds=").append(screenBounds(view)).append("; windowXY=").append(position[0]).append(',').append(position[1])
   .append("; parentBounds=").append(new Rect(view.getLeft(),view.getTop(),view.getRight(),view.getBottom()))
   .append("; measured=").append(view.getMeasuredWidth()).append('x').append(view.getMeasuredHeight())
   .append("; globalVisible=").append(globallyVisible).append(':').append(global)
   .append("; localVisible=").append(locallyVisible).append(':').append(local).append("; usableFrame=").append(usable)
   .append("; fullyVisible=").append(fullyVisible(view)).append("; shown=").append(view.isShown())
   .append("; attached=").append(view.isAttachedToWindow()).append("; visibility=").append(view.getVisibility())
   .append("; windowFocus=").append(view.hasWindowFocus()).append("; focused=").append(view.isFocused())
   .append("; enabled=").append(view.isEnabled()).append("; layoutRequested=").append(view.isLayoutRequested())
   .append("; scrollXY=").append(view.getScrollX()).append(',').append(view.getScrollY());
  ViewGroup.LayoutParams params=view.getLayoutParams();
  if(params!=null)record.append("; layoutSize=").append(params.width).append('x').append(params.height);
  if(view instanceof TextView){TextView text=(TextView)view;Layout layout=text.getLayout();
   record.append("; text=").append(text.getText().toString().replace('\n',' '));
   if(text instanceof EditText)record.append("; hint=").append(text.getHint());
   if(layout!=null){record.append("; textLayout=").append(layout.getWidth()).append('x').append(layout.getHeight())
     .append("; lineCount=").append(layout.getLineCount());
    if(layout.getLineCount()>0)record.append("; textScreenBounds=").append(textBounds(text));}
  }
 }
 private static boolean imeVisible(WindowInsets insets){
  return insets!=null&&insets.isVisible(WindowInsets.Type.ime());
 }
 private Rect inputMethodWindowBounds(){
  // Insets describe overlap with their own window. A floating dialog resized
  // above the IME can correctly have a zero IME inset while the IME is visible.
  // Only actual accessibility input-method windows supply the screen boundary.
  Rect result=null;List<AccessibilityWindowInfo> windows=instrumentation.getUiAutomation().getWindows();
  try{
   for(AccessibilityWindowInfo window:windows)if(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){
    Rect bounds=new Rect();window.getBoundsInScreen(bounds);
    if(!bounds.isEmpty()){if(result==null)result=new Rect(bounds);else result.union(bounds);}
   }
  }finally{for(AccessibilityWindowInfo window:windows)window.recycle();}
  return result;
 }
 private boolean validInputMethodBounds(Rect bounds){
  return bounds!=null&&bounds.width()>0&&bounds.height()>0&&
   activity.getWindowManager().getMaximumWindowMetrics().getBounds().contains(bounds);
 }
 private void appendAccessibilityWindows(StringBuilder record){
  AccessibilityServiceInfo service=instrumentation.getUiAutomation().getServiceInfo();
  record.append("\nautomationAccessibilityFlags=").append(service==null?"missing":service.flags);
  List<AccessibilityWindowInfo> windows=instrumentation.getUiAutomation().getWindows();
  record.append("\naccessibilityWindowCount=").append(windows.size());
  try{
   for(AccessibilityWindowInfo window:windows){Rect bounds=new Rect();window.getBoundsInScreen(bounds);
    record.append("\naccessibilityWindow=").append(window.getId()).append("; type=").append(window.getType())
     .append("; inputMethod=").append(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD)
     .append("; screenBounds=").append(bounds).append("; layer=").append(window.getLayer())
     .append("; active=").append(window.isActive()).append("; focused=").append(window.isFocused());
   }
  }finally{for(AccessibilityWindowInfo window:windows)window.recycle();}
  record.append("\ninputMethodScreenBounds=").append(inputMethodWindowBounds());
 }
 private void assertProfileActionsAboveKeyboard(View profile){
  assertTrue("Original profile is the focused window",profile.hasWindowFocus());
  WindowInsets insets=profile.getRootWindowInsets();assertTrue("Profile window reports a visible software keyboard",imeVisible(insets));
  Rect imeBounds=inputMethodWindowBounds();assertNotNull("Actual accessibility input-method window is present",imeBounds);
  assertTrue("Actual keyboard has nonempty screen bounds inside the display: "+imeBounds,validInputMethodBounds(imeBounds));
  int imeTop=imeBounds.top;
  for(int id:new int[]{android.R.id.button1,android.R.id.button2}){
   View action=profile.findViewById(id);String label=id==android.R.id.button1?"Review":"Cancel";
   assertTrue(label+" is a native dialog button",action instanceof Button);
   assertEquals(label+" retains its full label",label,((Button)action).getText().toString());
   assertVisible(action,label+" above keyboard");assertTouch(action,label+" above keyboard");assertTextFits((Button)action,label+" above keyboard");
   Rect buttonBounds=screenBounds(action),textBounds=textBounds((Button)action);
   assertTrue(label+" full text is inside the button: "+textBounds+" / "+buttonBounds,buttonBounds.contains(textBounds));
   assertTrue(label+" full bounds are above IME top="+imeTop+": "+buttonBounds,buttonBounds.bottom<=imeTop);
   assertTrue(label+" full text is above IME top="+imeTop+": "+textBounds,textBounds.bottom<=imeTop);
  }
 }
 private boolean fullyVisible(View view){
  if(view==null||!view.isShown()||view.getWidth()<=0||view.getHeight()<=0)return false;
  Rect visible=new Rect(),usable=new Rect();view.getWindowVisibleDisplayFrame(usable);
  return view.getGlobalVisibleRect(visible)&&visible.width()==view.getWidth()&&visible.height()==view.getHeight()&&usable.contains(screenBounds(view));
 }
 private static Rect screenBounds(View view){
  int[] position=new int[2];view.getLocationOnScreen(position);
  return new Rect(position[0],position[1],position[0]+view.getWidth(),position[1]+view.getHeight());
 }
 private static Rect textBounds(TextView view){
  Layout layout=view.getLayout();float left=Float.MAX_VALUE,right=-Float.MAX_VALUE;
  for(int line=0;line<layout.getLineCount();line++){
   left=Math.min(left,layout.getLineLeft(line));right=Math.max(right,layout.getLineRight(line));
  }
  Rect bounds=screenBounds(view);
  int originX=bounds.left+view.getTotalPaddingLeft()-view.getScrollX();
  int originY=bounds.top+view.getTotalPaddingTop()-view.getScrollY();
  return new Rect(originX+(int)Math.floor(left),originY,originX+(int)Math.ceil(right),originY+layout.getHeight());
 }
 private void tap(Finder finder,String label,boolean keyboardRequired)throws Exception {
  tap(finder,label,keyboardRequired,null);
 }
 private void tap(Finder finder,String label,boolean keyboardRequired,String readyEvidence)throws Exception {
  // Inject through Android's input dispatcher. performClick() would wrongly
  // report success for an action covered by the IME or outside the window.
  final int attempt=++tapAttempts;
  AtomicReference<View> observedAction=new AtomicReference<>();AtomicReference<Rect> bounds=new AtomicReference<>();
  AtomicReference<String> lastObservation=new AtomicReference<>("Tap wait not started"),previous=new AtomicReference<>();
  AtomicLong stableSince=new AtomicLong();
  try{
   await(label+" has focused, fully visible, stable native button bounds",()->{
    View action=finder.find();observedAction.set(action);
    if(action==null){lastObservation.set("action=missing");previous.set(null);return false;}
    Rect visible=new Rect(),usable=new Rect();action.getGlobalVisibleRect(visible);action.getWindowVisibleDisplayFrame(usable);
    Rect imeBounds=inputMethodWindowBounds();
    boolean focused=action.hasWindowFocus(),full=fullyVisible(action);
    boolean interactive=action instanceof Button&&action.isEnabled()&&action.isClickable();
    boolean keyboardReady=true;
    if(keyboardRequired){
     View profile=dialog(PROFILE);
     keyboardReady=profile!=null&&imeVisible(profile.getRootWindowInsets())&&validInputMethodBounds(imeBounds);
     if(keyboardReady)for(int id:new int[]{android.R.id.button1,android.R.id.button2}){
      View other=profile.findViewById(id);
      keyboardReady=keyboardReady&&fullyVisible(other)&&screenBounds(other).bottom<=imeBounds.top;
     }
    }
    lastObservation.set("windowFocus="+focused+"; fullyVisible="+full+"; nativeEnabledClickable="+interactive+
     "; keyboardRequired="+keyboardRequired+"; keyboardReady="+keyboardReady+
     "; buttonBounds="+screenBounds(action)+"; visibleBounds="+visible+"; usableFrame="+usable+"; inputMethodScreenBounds="+imeBounds);
    if(!focused||!full||!interactive||!keyboardReady){previous.set(null);return false;}
    String geometry=System.identityHashCode(action)+" / "+screenBounds(action)+" / "+visible+" / "+usable+
     " / root="+screenBounds(action.getRootView())+" / imeScreen="+imeBounds+
     " / text="+(((Button)action).getLayout()==null?"missing":textBounds((Button)action));
    if(!geometry.equals(previous.get())){previous.set(geometry);stableSince.set(SystemClock.elapsedRealtime());return false;}
    return SystemClock.elapsedRealtime()-stableSince.get()>=300;
   },20000);
   if(readyEvidence!=null)captureEvidence(readyEvidence,finder,record->{
    View action=finder.find(),window=action.getRootView();
    record.append("\ntapLabel=").append(label).append("\nkeyboardRequired=").append(keyboardRequired)
     .append("\nlastTapWaitPredicates=").append(lastObservation.get());
    appendKeyboardWindow(record,"readyTapWindow",window);
    appendKeyboardAncestors(record,"readyPositive",window.findViewById(android.R.id.button1));
    appendKeyboardAncestors(record,"readyNegative",window.findViewById(android.R.id.button2));
    appendAccessibilityWindows(record);
   });
   onMain(()->{
    View action=finder.find();assertNotNull(label,action);assertTrue(label+" owns window focus",action.hasWindowFocus());
    if(keyboardRequired)assertProfileActionsAboveKeyboard(requireDialog(PROFILE));
    assertVisible(action,label);assertTouch(action,label);assertTrue(label+" is a native button",action instanceof Button);
    assertTextFits((Button)action,label);bounds.set(screenBounds(action));
   });
   float x=bounds.get().exactCenterX(),y=bounds.get().exactCenterY();long downAt=SystemClock.uptimeMillis();
   MotionEvent down=MotionEvent.obtain(downAt,downAt,MotionEvent.ACTION_DOWN,x,y,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
   try{assertTrue(label+" touch DOWN is dispatched",instrumentation.getUiAutomation().injectInputEvent(down,true));}finally{down.recycle();}
   Thread.sleep(60);
   MotionEvent up=MotionEvent.obtain(downAt,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
   try{assertTrue(label+" touch UP is dispatched",instrumentation.getUiAutomation().injectInputEvent(up,true));}finally{up.recycle();}
   System.out.println("V283_COORDINATE_TAP label="+label+" bounds="+bounds.get()+" x="+x+" y="+y+" keyboardRequired="+keyboardRequired);
   twoFrames();
  }catch(Exception|AssertionError issue){
   captureTapFailure(attempt,label,keyboardRequired,issue,observedAction.get(),lastObservation.get(),previous.get());
   throw issue;
  }
 }
 private void captureTapFailure(int attempt,String label,boolean keyboardRequired,Throwable failure,
   View observedAction,String predicates,String stableGeometry){
  String filename="premium-large-text-tap-failure-"+attempt+".png";
  StringBuilder record=new StringBuilder("file=").append(filename).append("\nfailure=").append(failure)
   .append("\ntapLabel=").append(label).append("\nkeyboardRequired=").append(keyboardRequired)
   .append("\nelapsedRealtime=").append(SystemClock.elapsedRealtime()).append("\nlastTapWaitPredicates=").append(predicates)
   .append("\nlastStableGeometry=").append(stableGeometry);
  try{onMain(()->{
   record.append("\nfontScale=").append(activity.getResources().getConfiguration().fontScale)
    .append("\nwidthDp=").append(activity.getResources().getConfiguration().screenWidthDp)
    .append("\nactivityFinishing=").append(activity.isFinishing()).append("; activityDestroyed=").append(activity.isDestroyed())
    .append("\nactivityCurrentWindowBounds=").append(activity.getWindowManager().getCurrentWindowMetrics().getBounds());
   appendKeyboardAncestors(record,"observedTapTarget",observedAction);
   View window=observedAction==null?null:observedAction.getRootView();
   appendKeyboardWindow(record,"observedTapWindow",window);
   View confirmation=dialog(PROFILE_REVIEW);appendKeyboardWindow(record,"confirmation",confirmation);
   if(confirmation!=null){
    appendKeyboardAncestors(record,"confirmationPositive",confirmation.findViewById(android.R.id.button1));
    appendKeyboardAncestors(record,"confirmationNegative",confirmation.findViewById(android.R.id.button2));
    appendKeyboardScrollViews(record,confirmation);
   }
   appendKeyboardWindow(record,"profile",dialog(PROFILE));
   List<View> windows=WindowInspector.getGlobalWindowViews();record.append("\nglobalWindowCount=").append(windows.size());
   for(int index=0;index<windows.size();index++)appendKeyboardWindow(record,"window["+index+"]",windows.get(index));
   appendAccessibilityWindows(record);
  });}catch(Exception|AssertionError diagnosticIssue){record.append("\ngeometryCaptureIssue=").append(diagnosticIssue);}
  writeFailureEvidence(filename,record,"V283_TAP_FAILURE");
 }
 private void captureProfileKeyboardEvidence(String filename,Finder target)throws Exception {
  captureEvidence(filename,target,record->{
   View profile=requireDialog(PROFILE);assertProfileActionsAboveKeyboard(profile);
   Rect usable=new Rect();profile.getWindowVisibleDisplayFrame(usable);
   WindowInsets insets=profile.getRootWindowInsets();Rect windowBounds=activity.getWindowManager().getCurrentWindowMetrics().getBounds();
   Rect imeBounds=inputMethodWindowBounds();
   assertTrue("Capture retains actual input-method screen bounds",validInputMethodBounds(imeBounds));
   record.append("\nimeVisible=").append(insets.isVisible(WindowInsets.Type.ime()))
    .append("\ndialogImeInsets=").append(insets.getInsets(WindowInsets.Type.ime()))
    .append("\nactivityImeInsets=").append(decor().getRootWindowInsets()==null?"missing":decor().getRootWindowInsets().getInsets(WindowInsets.Type.ime()))
    .append("\nwindowBounds=").append(windowBounds).append("\nusableDisplayFrame=").append(usable)
    .append("\ninputMethodScreenBounds=").append(imeBounds).append("\nimeTop=").append(imeBounds.top);
   for(int id:new int[]{android.R.id.button1,android.R.id.button2}){
    TextView action=(TextView)profile.findViewById(id);Rect visible=new Rect();action.getGlobalVisibleRect(visible);
    record.append("\naction=").append(action.getText()).append("; class=").append(action.getClass().getName())
     .append("; screenBounds=").append(screenBounds(action)).append("; visibleBounds=").append(visible)
     .append("; textScreenBounds=").append(textBounds(action))
     .append("; textLayout=").append(action.getLayout().getWidth()).append('x').append(action.getLayout().getHeight())
     .append("; fullTextFits=true; fullyAboveIme=true");
   }
  });
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
   if(activity.getResources().getConfiguration().fontScale>=1.95f)
    assertEquals(name+" must remain one readable word at200% fonts",1,label.getLayout().getLineCount());
  }assertEquals("Exactly one selected navigation destination",1,count);
 }
 private void assertCompactBrand(){
  TextView title=(TextView)tag("brand:title");assertNotNull("Compact brand title",title);
  assertEquals("Complete club title","Badminton Club",title.getText().toString());
  assertVisible(title,"Compact brand title");assertTextFits(title,"Compact brand title");
  assertEquals("Club title must stay on one line at320dp/200% fonts",1,title.getLayout().getLineCount());
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
  await(label,condition,15000);
 }
 private void await(String label,BooleanSupplier condition,long timeoutMs)throws Exception {
  long deadline=SystemClock.elapsedRealtime()+timeoutMs;AtomicBoolean done=new AtomicBoolean();
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
 private void drainPriorToasts()throws Exception {
  // Photo lifecycle checks deliberately exercise busy feedback. Allow that
  // existing system Toast queue to finish before recording this test's UI.
  AtomicLong lastToast=new AtomicLong(SystemClock.elapsedRealtime());
  instrumentation.getUiAutomation().setOnAccessibilityEventListener(event->{
   if(event.getEventType()==AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED&&
     event.getClassName()!=null&&event.getClassName().toString().contains("Toast"))
    lastToast.set(SystemClock.elapsedRealtime());
  });
  long deadline=SystemClock.elapsedRealtime()+20000;
  try{
   while(SystemClock.elapsedRealtime()-lastToast.get()<4000){
    assertTrue("Prior transient Toast queue must settle before screenshots",SystemClock.elapsedRealtime()<deadline);
    Thread.sleep(100);
   }
  }finally{instrumentation.getUiAutomation().setOnAccessibilityEventListener(null);}
 }
 private void captureEvidence(String filename,Finder target)throws Exception {
  captureEvidence(filename,target,null);
 }
 private interface EvidenceDetails{void append(StringBuilder record);}
 private void captureEvidence(String filename,Finder target,EvidenceDetails details)throws Exception {
  // Geometry changes can precede the RenderThread/compositor. Require an
  // actual committed hardware frame of the relevant activity/dialog root.
  CountDownLatch committed=new CountDownLatch(1);
  onMain(()->{
   View view=target.find();assertNotNull("Evidence target "+filename,view);
   View window=view.getRootView();assertTrue("Evidence root is hardware accelerated",window.isHardwareAccelerated());
   window.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);
   window.postInvalidateOnAnimation();
  });
  assertTrue("Rendered frame commits before "+filename,committed.await(5,TimeUnit.SECONDS));
  instrumentation.getUiAutomation().waitForIdle(250,5000);
  instrumentation.waitForIdleSync();
  AtomicReference<String> geometry=new AtomicReference<>();
  onMain(()->{
   View view=target.find();assertVisible(view,"Capture target "+filename);
   int[] position=new int[2];view.getLocationOnScreen(position);
   StringBuilder record=new StringBuilder("file=").append(filename)
    .append("\nfontScale=").append(activity.getResources().getConfiguration().fontScale)
    .append("\nwidthDp=").append(activity.getResources().getConfiguration().screenWidthDp)
    .append("\ntarget=").append(view.getTag()==null?view.getContentDescription():view.getTag())
    .append("\nscreenBounds=").append(new Rect(position[0],position[1],position[0]+view.getWidth(),position[1]+view.getHeight()));
   for(View ancestor=view;ancestor!=null;ancestor=ancestor.getParent() instanceof View?(View)ancestor.getParent():null)
    if(ancestor instanceof ScrollView)record.append("\nscrollY=").append(ancestor.getScrollY()).append("; viewportHeight=").append(ancestor.getHeight());
   if(details!=null)details.append(record);
   geometry.set(record.append('\n').toString());
  });
  Bitmap image=instrumentation.getUiAutomation().takeScreenshot();
  assertNotNull("Actual Android screenshot "+filename,image);
  try{
   File directory=context.getExternalFilesDir("premium-ui-evidence");assertNotNull("Evidence directory",directory);
   assertTrue("Evidence directory is available",directory.isDirectory()||directory.mkdirs());
   try(FileOutputStream output=new FileOutputStream(new File(directory,filename))){
    assertTrue("Actual Android screenshot encodes",image.compress(Bitmap.CompressFormat.PNG,100,output));
   }
   try(FileOutputStream output=new FileOutputStream(new File(directory,filename+".txt"))){output.write(geometry.get().getBytes(StandardCharsets.UTF_8));}
   System.out.println("V283_SCREENSHOT_CAPTURE "+geometry.get().replace('\n',' '));
  }finally{image.recycle();}
 }
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

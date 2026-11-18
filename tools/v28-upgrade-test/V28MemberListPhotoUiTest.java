package com.vinayak.badminton.nativepreview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real member-list controls with synthetic images; never opens a device gallery. */
@RunWith(AndroidJUnit4.class)
public final class V28MemberListPhotoUiTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String OTHER="ci-upgrade-synthetic-roster-2";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String MONTH="2026-08";
 private static final String SEARCH="CI Synthetic Member";
 private static final int REQUEST=2012;

 @Test public void memberListPhotoActionsPreserveIdentityAndLedger()throws Exception {
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
  Context context=instrumentation.getTargetContext();
  LedgerDatabase db=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db")
   .allowMainThreadQueries().build();
  File source=null,otherSource=null;
  ActivityScenario<MainActivity> scenario=null;
  CapturedPicker capture=new CapturedPicker();
  IntentFilter filter=new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);
  filter.addCategory(Intent.CATEGORY_OPENABLE);
  filter.addDataType("image/*");
  // Null result deliberately leaves the request pending, without launching a
  // gallery. The test can then recreate the real Activity and deliver a result.
  Instrumentation.ActivityMonitor blocker=new Instrumentation.ActivityMonitor(filter,null,true);
  boolean syntheticVerified=false,monitorsInstalled=false;
  try {
   assertNotNull("Synthetic upgrade fixture is required",db.dao().meta());
   assertTrue("Never run this test against a real club ledger",
    db.dao().meta().club.startsWith("CI Synthetic Badminton Club"));
   assertEquals("Expected synthetic roster",15,db.dao().members().size());
   boolean targetFound=false,otherFound=false;
   for(Member member:db.dao().members()){
    if(MEMBER.equals(member.id)){targetFound=true;assertEquals(NAME,member.name);}
    if(OTHER.equals(member.id))otherFound=true;
   }
   assertTrue("Both immutable synthetic member IDs must exist",targetFound&&otherFound);
   syntheticVerified=true;
   String ledgerBefore=canonical(db);
   String status=PremiumV26State.monthStatus(PremiumV26State.feeSummary(
    db.dao().meta().inventoryStockJson,MEMBER,MONTH,MONTH,db.dao().contributions()),
    PremiumV26State.billableMonth(db.dao().meta().inventoryStockJson,MEMBER,MONTH));

   PremiumV28MemberPhotos.remove(context,MEMBER);
   source=portrait(context,Color.rgb(30,125,70));
   otherSource=portrait(context,Color.rgb(30,65,180));
   Uri selected=FileProvider.getUriForFile(context,context.getPackageName()+".files",source);
   Uri otherUri=FileProvider.getUriForFile(context,context.getPackageName()+".files",otherSource);
   PremiumV28MemberPhotos.addOrReplace(context,otherUri,OTHER);
   byte[] otherBefore=Files.readAllBytes(PremiumV28MemberPhotos.file(context,OTHER).toPath());

   instrumentation.addMonitor(capture);
   instrumentation.addMonitor(blocker);
   monitorsInstalled=true;
   scenario=ActivityScenario.launch(MainActivity.class);
   final ActivityScenario<MainActivity> active=scenario;
   scenario.onActivity(activity->{
    setField(activity,"searchText",SEARCH);
    setField(activity,"memberFilter",status);
    setField(activity,"reportMonth",MONTH);
    selectMembers(activity);
   });
   await(active,"Member list must expose Add photo",a->actionText(a,"Add photo"));
   scenario.onActivity(activity->{
    TextView action=photoAction(activity);
    assertEquals("Add photo for "+NAME,action.getContentDescription().toString());
    int minimum=(int)(48*activity.getResources().getDisplayMetrics().density+.5f);
    assertTrue("Photo action needs an accessible 48dp target",action.getMinHeight()>=minimum);
    assertTrue("Photo action must be focusable",action.isFocusable());
    assertNotNull("Missing photos retain member initials",findDescription(
     activity.getWindow().getDecorView(),"Member initials for "+NAME));
    assertTrue("Visible list action is clickable",action.performClick());
    assertEquals("Picker is tied to immutable member ID",MEMBER,field(activity,"pendingPhotoMemberId"));
    assertNull("Photo action must not open member financial history",field(activity,"memberDialog"));
   });
   assertEquals("System gallery launch is intercepted",1,blocker.getHits());
   assertPicker(capture.intent);
   scenario.onActivity(activity->activity.onActivityResult(REQUEST,Activity.RESULT_CANCELED,null));
   scenario.onActivity(activity->assertNull("Cancel clears pending target",field(activity,"pendingPhotoMemberId")));
   assertFalse("Canceled selection adds no portrait",PremiumV28MemberPhotos.has(context,MEMBER));
   assertEquals("Cancel changes no financial record",ledgerBefore,canonical(db));

   // Rebuild creates a fresh visible action, avoiding the existing tap debounce.
   scenario.onActivity(V28MemberListPhotoUiTest::selectMembers);
   await(active,"Add photo remains usable after cancellation",a->actionText(a,"Add photo"));
   scenario.onActivity(activity->assertTrue(photoAction(activity).performClick()));
   assertEquals(2,blocker.getHits());
   scenario.recreate();
   await(active,"Member list survives picker recreation",a->actionText(a,"Add photo"));
   scenario.onActivity(activity->{
    assertEquals("Pending member survives recreation",MEMBER,field(activity,"pendingPhotoMemberId"));
    assertEquals("Search survives recreation",SEARCH,field(activity,"searchText"));
    assertEquals("Status filter survives recreation",status,field(activity,"memberFilter"));
    assertEquals("Selected month survives recreation",MONTH,field(activity,"reportMonth"));
    assertEquals("Return to Members after recreation","Members",field(activity,"tab"));
    activity.onActivityResult(REQUEST,Activity.RESULT_OK,new Intent().setData(selected));
   });
   await(active,"Successful image selection refreshes list action",
    a->actionText(a,"Photo options")&&!Boolean.TRUE.equals(field(a,"memberPhotoInProgress")));
   assertTrue("Correct member receives photograph",PremiumV28MemberPhotos.has(context,MEMBER));
   byte[] savedPhoto=Files.readAllBytes(PremiumV28MemberPhotos.file(context,MEMBER).toPath());
   assertArrayEquals("Other member photograph is untouched",otherBefore,
    Files.readAllBytes(PremiumV28MemberPhotos.file(context,OTHER).toPath()));
   assertEquals("Adding photo changes no financial record",ledgerBefore,canonical(db));
   scenario.onActivity(activity->{
    assertEquals("Photo options for "+NAME,photoAction(activity).getContentDescription().toString());
    assertNotNull("List shows saved photograph",activity.getWindow().getDecorView()
     .findViewWithTag("member-photo-image:"+MEMBER));
    View memberPanel=(View)photoAction(activity).getParent();
    memberPanel.requestRectangleOnScreen(new Rect(0,0,memberPanel.getWidth(),memberPanel.getHeight()),true);
   });
   screenshot(instrumentation,context,"member-list-photo.png");
   scenario.onActivity(activity->assertTrue(photoAction(activity).performClick()));
   awaitAccessible(instrumentation,"Photo · "+NAME,false);
   awaitAccessible(instrumentation,"Photo of "+NAME,false);
   screenshot(instrumentation,context,"member-photo-options.png");
   awaitAccessible(instrumentation,"Change photo",true);
   assertEquals("Change uses the same scoped picker",3,blocker.getHits());
   scenario.onActivity(activity->{
    assertEquals("Changing retains original member ID",MEMBER,field(activity,"pendingPhotoMemberId"));
    activity.onActivityResult(REQUEST,Activity.RESULT_CANCELED,null);
   });
   assertArrayEquals("Canceling change retains original portrait",savedPhoto,
    Files.readAllBytes(PremiumV28MemberPhotos.file(context,MEMBER).toPath()));

   scenario.onActivity(V28MemberListPhotoUiTest::selectMembers);
   await(active,"Photo options allows replacement",a->actionText(a,"Photo options"));
   scenario.onActivity(activity->assertTrue(photoAction(activity).performClick()));
   awaitAccessible(instrumentation,"Change photo",true);
   assertEquals(4,blocker.getHits());
   scenario.onActivity(activity->{
    assertEquals(MEMBER,field(activity,"pendingPhotoMemberId"));
    activity.onActivityResult(REQUEST,Activity.RESULT_OK,new Intent().setData(otherUri));
   });
   await(active,"Replacement finishes and refreshes member list",
    a->actionText(a,"Photo options")&&!Boolean.TRUE.equals(field(a,"memberPhotoInProgress")));
   assertFalse("Successful Change replaces the selected member photograph",Arrays.equals(savedPhoto,
    Files.readAllBytes(PremiumV28MemberPhotos.file(context,MEMBER).toPath())));
   assertArrayEquals("Replacing target does not modify another member photo",otherBefore,
    Files.readAllBytes(PremiumV28MemberPhotos.file(context,OTHER).toPath()));
   assertEquals("Replacing photograph changes no financial record",ledgerBefore,canonical(db));

   scenario.onActivity(V28MemberListPhotoUiTest::selectMembers);
   await(active,"Photo options remains usable",a->actionText(a,"Photo options"));
   scenario.onActivity(activity->assertTrue(photoAction(activity).performClick()));
   awaitAccessible(instrumentation,"Photo · "+NAME,false);
   awaitAccessible(instrumentation,"Remove photo",true);
   awaitAccessible(instrumentation,"Remove local member photograph?",false);
   assertTrue("Removal requires confirmation",PremiumV28MemberPhotos.has(context,MEMBER));
   awaitAccessible(instrumentation,"Remove photo",true);
   await(active,"Confirmed removal restores Add photo",
    a->actionText(a,"Add photo")&&!Boolean.TRUE.equals(field(a,"memberPhotoInProgress")));
   assertFalse("Selected portrait was removed",PremiumV28MemberPhotos.has(context,MEMBER));
   assertArrayEquals("Removal preserves every other member portrait",otherBefore,
    Files.readAllBytes(PremiumV28MemberPhotos.file(context,OTHER).toPath()));
   assertEquals("Add, cancel, recreate, change and remove preserve ledger",ledgerBefore,canonical(db));
   assertEquals(15,db.dao().members().size());
   System.out.println("V28_MEMBER_LIST_PHOTO_UI_PASS");
  } finally {
   if(scenario!=null)scenario.close();
   if(monitorsInstalled){instrumentation.removeMonitor(capture);instrumentation.removeMonitor(blocker);}
   if(syntheticVerified){
    PremiumV28MemberPhotos.remove(context,MEMBER);
    PremiumV28MemberPhotos.remove(context,OTHER);
   }
   if(source!=null)source.delete();
   if(otherSource!=null)otherSource.delete();
   db.close();
  }
 }

 private static final class CapturedPicker extends Instrumentation.ActivityMonitor {
  volatile Intent intent;
  @Override public Instrumentation.ActivityResult onStartActivity(Intent request){
   if(Intent.ACTION_OPEN_DOCUMENT.equals(request.getAction()))intent=new Intent(request);
   return null;
  }
 }
 private static void assertPicker(Intent intent){
  assertNotNull("Photo list action must issue an Android picker intent",intent);
  assertEquals(Intent.ACTION_OPEN_DOCUMENT,intent.getAction());
  assertEquals("image/*",intent.getType());
  assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE));
  assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
 }
 private static String canonical(LedgerDatabase db)throws Exception {
  return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
 }
 private static void screenshot(Instrumentation instrumentation,Context context,String name){
  Bitmap image=null;
  try {
   instrumentation.waitForIdleSync();
   image=instrumentation.getUiAutomation().takeScreenshot();
   if(image==null)return;
   File directory=context.getExternalFilesDir("photo-ui-evidence");
   if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))return;
   try(FileOutputStream output=new FileOutputStream(new File(directory,name))){
    image.compress(Bitmap.CompressFormat.PNG,100,output);
   }
  } catch(Exception issue){
   System.out.println("Optional synthetic UI screenshot unavailable: "+issue.getClass().getSimpleName());
  } finally {if(image!=null)image.recycle();}
 }
 private static File portrait(Context context,int color)throws Exception {
  File directory=new File(context.getCacheDir(),"pdfs");
  assertTrue(directory.isDirectory()||directory.mkdirs());
  File source=new File(directory,"ci-list-photo-"+UUID.randomUUID()+".jpg");
  Bitmap bitmap=Bitmap.createBitmap(600,450,Bitmap.Config.ARGB_8888);
  bitmap.eraseColor(color);
  try(FileOutputStream output=new FileOutputStream(source)){
   assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,88,output));
  } finally {bitmap.recycle();}
  return source;
 }
 private static Object field(MainActivity activity,String name){
  try {Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
  catch(Exception e){throw new AssertionError("Cannot inspect Activity field "+name,e);}
 }
 private static void setField(MainActivity activity,String name,Object value){
  try {Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(activity,value);}
  catch(Exception e){throw new AssertionError("Cannot set fixture UI field "+name,e);}
 }
 private static void selectMembers(MainActivity activity){
  try {Method method=MainActivity.class.getDeclaredMethod("selectTab",String.class);
   method.setAccessible(true);method.invoke(activity,"Members");}
  catch(Exception e){throw new AssertionError("Cannot open Members",e);}
 }
 private static TextView photoAction(MainActivity activity){
  View view=activity.getWindow().getDecorView().findViewWithTag("member-photo-action:"+MEMBER);
  assertTrue("Tagged member photo action must exist",view instanceof TextView);
  return (TextView)view;
 }
 private static boolean actionText(MainActivity activity,String text){
  View view=activity.getWindow().getDecorView().findViewWithTag("member-photo-action:"+MEMBER);
  return view instanceof TextView&&text.contentEquals(((TextView)view).getText());
 }
 private static View findDescription(View view,String description){
  if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
  if(view instanceof ViewGroup){
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++){
    View found=findDescription(group.getChildAt(i),description);if(found!=null)return found;
   }
  }
  return null;
 }
 private interface Ready {boolean check(MainActivity activity);}
 private static void await(ActivityScenario<MainActivity> scenario,String message,Ready ready)throws Exception {
  long deadline=SystemClock.elapsedRealtime()+15000;
  AtomicBoolean done=new AtomicBoolean();
  do {
   scenario.onActivity(activity->done.set(ready.check(activity)));
   if(done.get())return;
   Thread.sleep(100);
  } while(SystemClock.elapsedRealtime()<deadline);
  fail(message);
 }
 private static void awaitAccessible(Instrumentation instrumentation,String label,boolean click)throws Exception {
  long deadline=SystemClock.elapsedRealtime()+15000;
  do {
   AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
   if(root!=null){
    try {
     List<AccessibilityNodeInfo> matches=root.findAccessibilityNodeInfosByText(label);
     boolean done=false;
     for(AccessibilityNodeInfo node:matches){
      try {
       // Native AlertDialog themes may expose button text in ALL CAPS.
       // Keep an exact whole-label match while ignoring that visual casing.
       boolean exact=label.equalsIgnoreCase(node.getText()==null?"":node.getText().toString())||
        label.equalsIgnoreCase(node.getContentDescription()==null?"":node.getContentDescription().toString());
       if(exact&&(!click||(node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))))done=true;
      } finally {node.recycle();}
     }
     if(done)return;
    } finally {root.recycle();}
   }
   Thread.sleep(100);
  } while(SystemClock.elapsedRealtime()<deadline);
  StringBuilder diagnostic=new StringBuilder();
  AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
  if(root!=null){
   try {describeNodes(root,diagnostic,new int[]{35},0);}finally{root.recycle();}
  } else diagnostic.append("No active accessibility window");
  screenshot(instrumentation,instrumentation.getTargetContext(),"member-photo-ui-failure.png");
  fail((click?"Cannot click ":"Cannot find ")+"visible dialog control: "+label+"\n"+diagnostic);
 }
 private static void describeNodes(AccessibilityNodeInfo node,StringBuilder out,int[] remaining,int depth){
  if(node==null||remaining[0]--<=0||depth>10)return;
  out.append(depth).append(' ').append(node.getClassName()).append(" id=").append(node.getViewIdResourceName())
   .append(" text=").append(shortLabel(node.getText())).append(" desc=").append(shortLabel(node.getContentDescription()))
   .append(" clickable=").append(node.isClickable()).append(" enabled=").append(node.isEnabled()).append('\n');
  for(int i=0;i<node.getChildCount()&&remaining[0]>0;i++){
   AccessibilityNodeInfo child=node.getChild(i);
   if(child!=null){try{describeNodes(child,out,remaining,depth+1);}finally{child.recycle();}}
  }
 }
 private static String shortLabel(CharSequence value){
  String label=value==null?"":value.toString().replace('\n',' ');
  return label.length()>100?label.substring(0,100)+"…":label;
 }
}

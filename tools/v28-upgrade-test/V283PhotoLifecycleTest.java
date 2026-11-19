package com.vinayak.badminton.nativepreview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Queued photo work survives Activity recreation; synthetic images and ledger only. */
@RunWith(AndroidJUnit4.class)
public final class V283PhotoLifecycleTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String OTHER="ci-upgrade-synthetic-roster-2";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String MONTH="2026-08";
 private static final String SEARCH="CI Synthetic Member";
 private static final int REQUEST=2012;

 @Test public void queuedPhotoSaveAndFailureSurviveRecreation()throws Exception {
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
  Context context=instrumentation.getTargetContext();
  LedgerDatabase db=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db")
   .allowMainThreadQueries().build();
  ActivityScenario<MainActivity> scenario=null;
  List<QueueHold> holds=new ArrayList<>();
  List<File> sources=new ArrayList<>();
  File targetFile=null,otherFile=null;
  byte[] initialTarget=null,initialOther=null;
  boolean syntheticVerified=false,monitorInstalled=false;
  IntentFilter filter=new IntentFilter(Intent.ACTION_OPEN_DOCUMENT);
  filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("image/*");
  // Suppress the real gallery but retain the real picker request and result path.
  Instrumentation.ActivityMonitor picker=new Instrumentation.ActivityMonitor(filter,null,true);
  try {
   assertNotNull("Synthetic upgrade fixture is required",db.dao().meta());
   assertTrue("Never run lifecycle writes against a real club ledger",
    db.dao().meta().club.startsWith("CI Synthetic Badminton Club"));
   assertEquals("Complete synthetic roster is required",15,db.dao().members().size());
   Member selected=null,second=null;
   for(Member member:db.dao().members()){
    if(MEMBER.equals(member.id)){selected=member;assertEquals(NAME,member.name);}
    if(OTHER.equals(member.id))second=member;
   }
   assertNotNull("Target immutable member exists",selected);
   assertNotNull("Independent immutable member exists",second);
   final Member member=selected,other=second;
   String before=canonical(db);
   targetFile=PremiumV28MemberPhotos.file(context,MEMBER);
   otherFile=PremiumV28MemberPhotos.file(context,OTHER);
   initialTarget=targetFile.isFile()?Files.readAllBytes(targetFile.toPath()):null;
   initialOther=otherFile.isFile()?Files.readAllBytes(otherFile.toPath()):null;
   syntheticVerified=true;
   PremiumV28MemberPhotos.remove(context,MEMBER);
   File valid=portrait(context,Color.rgb(30,125,70));sources.add(valid);
   File independent=portrait(context,Color.rgb(30,65,180));sources.add(independent);
   File invalid=invalidImage(context);sources.add(invalid);
   Uri validUri=uri(context,valid),invalidUri=uri(context,invalid);
   PremiumV28MemberPhotos.addOrReplace(context,uri(context,independent),OTHER);
   byte[] otherBefore=Files.readAllBytes(otherFile.toPath());
   instrumentation.addMonitor(picker);monitorInstalled=true;

   scenario=ActivityScenario.launch(MainActivity.class);
   final ActivityScenario<MainActivity> active=scenario;
   scenario.onActivity(activity->{
    setField(activity,"searchText",SEARCH);setField(activity,"memberFilter","All");
    setField(activity,"reportMonth",MONTH);
    invoke(activity,"selectTab",new Class<?>[]{String.class},"Members");
   });
   await(active,"Initial member action is ready",a->actionText(a,"Add photo"));

   // Deterministically put the save behind a blocked worker. No timing assumption
   // about how quickly a real device decodes the image is needed.
   AtomicReference<MainActivity> firstActivity=new AtomicReference<>();
   scenario.onActivity(firstActivity::set);
   QueueHold saveHold=holdQueue(active);holds.add(saveHold);
   scenario.onActivity(activity->{
    invoke(activity,"pickMemberPhoto",new Class<?>[]{Member.class},member);
    assertEquals("Picker owns the selected immutable ID",MEMBER,field(activity,"pendingPhotoMemberId"));
    activity.onActivityResult(REQUEST,Activity.RESULT_OK,new Intent().setData(validUri));
    assertNull("Result is consumed before worker execution",field(activity,"pendingPhotoMemberId"));
    assertTrue("Queued save acquires process-wide work gate",workGate(activity).get());
    assertTrue("Original Activity records queued work",Boolean.TRUE.equals(field(activity,"memberPhotoInProgress")));
   });
   assertEquals("Exactly one actual picker request",1,picker.getHits());
   assertFalse("Blocked worker has not written the target image",PremiumV28MemberPhotos.has(context,MEMBER));
   scenario.recreate();
   await(active,"Recreated member list is ready",a->actionText(a,"Add photo"));
   scenario.onActivity(activity->{
    assertNotSame("A different Activity must own the screen",firstActivity.get(),activity);
    assertTrue("Original Activity was destroyed",firstActivity.get().isDestroyed());
    assertSame("Photo completion is routed to the current Activity",activity,
     ((Reference<?>)field(activity,"memberPhotoActivity")).get());
    assertEquals("Members context survives recreation","Members",field(activity,"tab"));
    assertEquals("Historical month survives recreation",MONTH,field(activity,"reportMonth"));
    assertEquals("Member query survives recreation",SEARCH,field(activity,"searchText"));
    assertBusyPreventsAnotherPicker(activity,other);
   });
   assertEquals("Recreation cannot launch a competing picker",1,picker.getHits());
   saveHold.releaseAndAwait();
   await(active,"Old worker completion refreshes the new member list",a->
    actionText(a,"Photo options")&&!workGate(a).get()&&
    !Boolean.TRUE.equals(field(a,"memberPhotoInProgress"))&&
    a.getWindow().getDecorView().findViewWithTag("member-photo-image:"+MEMBER)!=null);
   byte[] savedPhoto=Files.readAllBytes(targetFile.toPath());
   assertTrue("Queued save produced a normalized portrait",savedPhoto.length>100);
   assertArrayEquals("Another member's photograph is untouched",otherBefore,Files.readAllBytes(otherFile.toPath()));
   assertEquals("Queued save and recreation preserve all ledger fields",before,canonical(db));
   scenario.onActivity(activity->assertEquals("Successful completion releases busy state",Boolean.FALSE,
    invoke(activity,"memberPhotoBusy",new Class<?>[]{})));
   System.out.println("V283_PHOTO_QUEUED_SAVE_RECREATION_PASS");

   // A second, separately blocked operation fails after another recreation.
   // Its live error must be visible and the previously saved portrait retained.
   AtomicReference<MainActivity> secondActivity=new AtomicReference<>();
   scenario.onActivity(secondActivity::set);
   QueueHold failureHold=holdQueue(active);holds.add(failureHold);
   scenario.onActivity(activity->{
    invoke(activity,"pickMemberPhoto",new Class<?>[]{Member.class},member);
    assertEquals(MEMBER,field(activity,"pendingPhotoMemberId"));
    activity.onActivityResult(REQUEST,Activity.RESULT_OK,new Intent().setData(invalidUri));
    assertTrue("Invalid-image verification is still queued photo work",workGate(activity).get());
   });
   assertEquals("Replacement uses one new actual picker",2,picker.getHits());
   scenario.recreate();
   await(active,"Existing portrait is visible before delayed failure",a->actionText(a,"Photo options"));
   scenario.onActivity(activity->{
    assertNotSame(secondActivity.get(),activity);
    assertTrue("Second old Activity was destroyed",secondActivity.get().isDestroyed());
    assertSame(activity,((Reference<?>)field(activity,"memberPhotoActivity")).get());
    assertBusyPreventsAnotherPicker(activity,other);
   });
   assertEquals("Queued failure also blocks competing picker",2,picker.getHits());
   failureHold.releaseAndAwait();
   await(active,"Failure is reported by a live Activity",a->
    !a.isDestroyed()&&!a.isFinishing()&&dialog(a,"Action not completed")!=null&&
    !workGate(a).get()&&!Boolean.TRUE.equals(field(a,"memberPhotoInProgress")));
   scenario.onActivity(activity->{
    View error=dialog(activity,"Action not completed");
    assertNotNull("A visible error dialog exists after the old Activity was destroyed",error);
    assertNotNull("Error explains the rejected image",
     findTextContaining(error,"Choose a normal JPEG/PNG portrait"));
    View ok=findText(error,"OK");
    assertNotNull("Error remains dismissible",ok);
    assertTrue("Dismiss uses the live dialog's actual button",ok.performClick());
   });
   await(active,"Error dismissal returns to the usable member list",a->
    dialog(a,"Action not completed")==null&&actionText(a,"Photo options"));
   assertArrayEquals("Failed replacement retains the previous photo bytes",savedPhoto,
    Files.readAllBytes(targetFile.toPath()));
   assertArrayEquals("Failed replacement preserves other member identity",otherBefore,
    Files.readAllBytes(otherFile.toPath()));
   assertEquals("Failed replacement and recreation preserve canonical ledger",before,canonical(db));
   // Verify the released gate permits a subsequent real picker and cancellation.
   scenario.onActivity(activity->{
    assertEquals(Boolean.FALSE,invoke(activity,"memberPhotoBusy",new Class<?>[]{}));
    invoke(activity,"pickMemberPhoto",new Class<?>[]{Member.class},other);
    assertEquals("A later picker can own another immutable ID",OTHER,field(activity,"pendingPhotoMemberId"));
    activity.onActivityResult(REQUEST,Activity.RESULT_CANCELED,null);
    assertNull("Later cancellation releases the target",field(activity,"pendingPhotoMemberId"));
   });
   assertEquals("Both completed operations leave picker usable",3,picker.getHits());
   assertArrayEquals(savedPhoto,Files.readAllBytes(targetFile.toPath()));
   assertArrayEquals(otherBefore,Files.readAllBytes(otherFile.toPath()));
   assertEquals(before,canonical(db));
   assertEquals(15,db.dao().members().size());
   System.out.println("V283_PHOTO_FAILURE_RECREATION_RETAINED_PASS");
   System.out.println("V283_PHOTO_LIFECYCLE_PASS");
  } finally {
   // Always unblock old executors before destroying the live screen or cleaning
   // synthetic files, including assertion-failure paths.
   for(QueueHold hold:holds)hold.release.countDown();
   if(scenario!=null)scenario.close();
   for(QueueHold hold:holds)hold.executor.awaitTermination(15,TimeUnit.SECONDS);
   instrumentation.waitForIdleSync();
   if(monitorInstalled)instrumentation.removeMonitor(picker);
   if(syntheticVerified){restorePhoto(targetFile,initialTarget);restorePhoto(otherFile,initialOther);}
   for(File file:sources)file.delete();
   db.close();
  }
 }

 private static void assertBusyPreventsAnotherPicker(MainActivity activity,Member other){
  assertEquals("Queued work remains busy across recreation",Boolean.TRUE,
   invoke(activity,"memberPhotoBusy",new Class<?>[]{}));
  assertNull("Recreated Activity has no separate pending selection",field(activity,"pendingPhotoMemberId"));
  invoke(activity,"pickMemberPhoto",new Class<?>[]{Member.class},other);
  assertNull("Competing member selection cannot replace the queued target",field(activity,"pendingPhotoMemberId"));
  assertTrue("Attempt cannot release the existing work gate",workGate(activity).get());
 }
 private static QueueHold holdQueue(ActivityScenario<MainActivity> scenario)throws Exception {
  AtomicReference<ExecutorService> executor=new AtomicReference<>();
  scenario.onActivity(activity->executor.set((ExecutorService)field(activity,"io")));
  QueueHold hold=new QueueHold(executor.get());
  try {
   assertTrue("Activity worker reached deterministic hold",hold.entered.await(15,TimeUnit.SECONDS));
   return hold;
  } catch(Throwable failure){
   hold.release.countDown();throw failure;
  }
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
    catch(InterruptedException interrupted){Thread.currentThread().interrupt();timedOut.set(true);}
   });
  }
  void releaseAndAwait()throws Exception {
   assertTrue("Recreation shut down only the old executor",executor.isShutdown());
   release.countDown();
   assertTrue("Previously queued photo work drains after recreation",executor.awaitTermination(15,TimeUnit.SECONDS));
   assertFalse("Queue hold was released by the test, never by timing out",timedOut.get());
  }
 }
 private static AtomicBoolean workGate(MainActivity activity){
  return (AtomicBoolean)field(activity,"memberPhotoWork");
 }
 private static String canonical(LedgerDatabase db)throws Exception {
  return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
 }
 private static Uri uri(Context context,File file){
  return FileProvider.getUriForFile(context,context.getPackageName()+".files",file);
 }
 private static File portrait(Context context,int color)throws Exception {
  File directory=new File(context.getCacheDir(),"pdfs");
  assertTrue(directory.isDirectory()||directory.mkdirs());
  File file=new File(directory,"ci-lifecycle-photo-"+UUID.randomUUID()+".jpg");
  Bitmap bitmap=Bitmap.createBitmap(600,450,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color);
  try(FileOutputStream output=new FileOutputStream(file)){
   assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,88,output));
  } finally {bitmap.recycle();}
  return file;
 }
 private static File invalidImage(Context context)throws Exception {
  File directory=new File(context.getCacheDir(),"pdfs");
  assertTrue(directory.isDirectory()||directory.mkdirs());
  File file=new File(directory,"ci-lifecycle-invalid-"+UUID.randomUUID()+".jpg");
  // Longer than the incomplete-file threshold, but not any supported image.
  byte[] invalid=new byte[256];java.util.Arrays.fill(invalid,(byte)0x41);
  Files.write(file.toPath(),invalid);return file;
 }
 private static void restorePhoto(File file,byte[] original)throws Exception {
  if(original==null){if(file.exists())assertTrue("Clean up synthetic photo",file.delete());}
  else Files.write(file.toPath(),original);
 }
 private static Object field(MainActivity activity,String name){
  try {Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
  catch(Exception issue){throw new AssertionError("Cannot inspect Activity field "+name,issue);}
 }
 private static void setField(MainActivity activity,String name,Object value){
  try {Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(activity,value);}
  catch(Exception issue){throw new AssertionError("Cannot set synthetic Activity field "+name,issue);}
 }
 private static Object invoke(MainActivity activity,String name,Class<?>[] types,Object... args){
  try {Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,args);}
  catch(Exception issue){throw new AssertionError("Cannot invoke Activity method "+name,issue);}
 }
 private static boolean actionText(MainActivity activity,String text){
  View view=activity.getWindow().getDecorView().findViewWithTag("member-photo-action:"+MEMBER);
  return view instanceof TextView&&text.contentEquals(((TextView)view).getText());
 }
 private static View dialog(MainActivity activity,String title){
  for(View root:WindowInspector.getGlobalWindowViews())
   if(root!=activity.getWindow().getDecorView()&&root.isAttachedToWindow()&&root.isShown()&&findText(root,title)!=null)
    return root;
  return null;
 }
 private static View findText(View view,String expected){
  if(view instanceof TextView&&expected.equalsIgnoreCase(((TextView)view).getText().toString()))return view;
  if(view instanceof ViewGroup){
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++){
    View found=findText(group.getChildAt(i),expected);if(found!=null)return found;
   }
  }
  return null;
 }
 private static View findTextContaining(View view,String expected){
  if(view instanceof TextView&&((TextView)view).getText().toString().contains(expected))return view;
  if(view instanceof ViewGroup){
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++){
    View found=findTextContaining(group.getChildAt(i),expected);if(found!=null)return found;
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
   Thread.sleep(50);
  } while(SystemClock.elapsedRealtime()<deadline);
  fail(message);
 }
}

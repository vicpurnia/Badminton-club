package com.vinayak.badminton.nativepreview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.SystemClock;
import android.view.KeyEvent;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Delayed recovery UI and export preparation; synthetic fixture only, no imports. */
@RunWith(AndroidJUnit4.class)
public final class V283RecoveryLifecycleTest {
 private static final String CLUB="CI Synthetic Badminton Club — no real data";
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String MONTH="2026-08";
 private static final int PICK_LEDGER=2001,SAVE_JSON=2002,SAVE_PDF=2003,VERIFY_JSON=2005,TEST_RESTORE=2006;
 private Instrumentation instrumentation;
 private Context context;
 private ActivityScenario<MainActivity> scenario;
 private final List<QueueHold> holds=new ArrayList<>();
 private final List<File> temporaryFiles=new ArrayList<>();
 private final StringBuilder evidence=new StringBuilder();

 @Test public void delayedRecoveryAndExportsRespectActivityOwnership()throws Exception {
  instrumentation=InstrumentationRegistry.getInstrumentation();
  context=instrumentation.getTargetContext();
  LedgerDatabase persisted=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db")
   .allowMainThreadQueries().build();
  LedgerDatabase isolated=null;
  MainActivity detachedOwner=null,reviewOwner=null;
  Object detachedOriginal=null,reviewOriginal=null;
  IntentFilter filter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);
  filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("*/*");
  // Keep the actual request path, without opening a document provider or writing remotely.
  Instrumentation.ActivityMonitor picker=new Instrumentation.ActivityMonitor(filter,null,true);
  boolean monitorInstalled=false;
  try {
   requireSyntheticFixture(persisted);
   final String original=canonical(persisted);
   final File source=newTemporaryFile("review-source",".json");
   Files.write(source.toPath(),MigrationCodec.exportVerified(persisted).getBytes(StandardCharsets.UTF_8));
   assertEquals("Review source is the complete synthetic ledger",15,
    MigrationCodec.inspect(new String(Files.readAllBytes(source.toPath()),StandardCharsets.UTF_8)).players);
   final Uri sourceUri=FileProvider.getUriForFile(context,context.getPackageName()+".files",source);
   final File delayedPdf=newTemporaryFile("delayed-save",".pdf");
   final File delayedPreview=newTemporaryFile("delayed-preview",".pdf");
   final File freshPdf=newTemporaryFile("fresh-save",".pdf");
   final AtomicBoolean saveRendered=new AtomicBoolean(),previewRendered=new AtomicBoolean();
   final Set<String> existingJsonStages=jsonStages();
   instrumentation.addMonitor(picker);monitorInstalled=true;
   scenario=ActivityScenario.launch(MainActivity.class);
   scenario.onActivity(activity->{
    setField(activity,"reportMonth",MONTH);
    invoke(activity,"selectTab",new Class<?>[]{String.class},"Members");
   });
   await("Initial synthetic member list",a->findText(a.getWindow().getDecorView(),NAME)!=null);

   QueueHold delayed=holdQueue();
   MainActivity old=delayed.owner;
   scenario.onActivity(activity->{
    // These real worker paths are held until their original Activity is destroyed.
    invoke(activity,"backupNow",new Class<?>[]{});
    invoke(activity,"verifyPrivateBackup",new Class<?>[]{});
    activity.onActivityResult(VERIFY_JSON,Activity.RESULT_OK,new Intent().setData(sourceUri));
    activity.onActivityResult(TEST_RESTORE,Activity.RESULT_OK,new Intent().setData(sourceUri));
    invoke(activity,"startVerifiedExport",new Class<?>[]{});
    invoke(activity,"savePdf",new Class<?>[]{String.class,MainActivity.RunnablePdf.class},
     "CI_delayed_recovery.pdf",(MainActivity.RunnablePdf)()->{
      writePdf(delayedPdf);saveRendered.set(true);return delayedPdf;
     });
    invoke(activity,"previewPdf",new Class<?>[]{MainActivity.RunnablePdf.class},
     (MainActivity.RunnablePdf)()->{
      writePdf(delayedPreview);previewRendered.set(true);return delayedPreview;
     });
    assertTrue("Queued JSON owns its gate",jsonGate(activity).active());
    assertTrue("Queued PDF owns its gate",pdfGate(activity).active());
    assertNull("JSON preparation has not run before recreation",field(activity,"pendingExport"));
    assertNull("PDF preparation has not run before recreation",field(activity,"pendingReportPdf"));
   });
   assertFalse("PDF worker is deterministically blocked",saveRendered.get());
   assertFalse("Preview worker is deterministically blocked",previewRendered.get());
   scenario.recreate();
   await("New owner renders the member list",a->findText(a.getWindow().getDecorView(),NAME)!=null);
   scenario.onActivity(activity->{
    assertNotSame("Recreation supplies a new screen owner",old,activity);
    assertTrue("Old owner was destroyed before work is released",old.isDestroyed());
    assertEquals("Selected screen survives recreation","Members",field(activity,"tab"));
    assertEquals("Selected month survives recreation",MONTH,field(activity,"reportMonth"));
   });
   delayed.releaseAndAwait();
   instrumentation.waitForIdleSync();
   assertEquals("Destroyed owner cannot open either Documents picker",0,picker.getHits());
   assertTrue("Queued PDF actually rendered after recreation",saveRendered.get());
   assertTrue("Queued receipt preview actually rendered after recreation",previewRendered.get());
   assertFalse("Abandoned PDF save removes its own generated file",delayedPdf.exists());
   assertFalse("Abandoned receipt preview removes its own generated file",delayedPreview.exists());
   assertEquals("Abandoned JSON removes only its new staging file",existingJsonStages,jsonStages());
   scenario.onActivity(activity->{
    assertNull("Old JSON pending source is released",field(old,"pendingExport"));
    assertNull("Old PDF pending source is released",field(old,"pendingReportPdf"));
    assertFalse("Old JSON gate is released",jsonGate(old).active());
    assertFalse("Old PDF gate is released",pdfGate(old).active());
    assertFalse("New owner has an independent JSON gate",jsonGate(activity).active());
    assertFalse("New owner has an independent PDF gate",pdfGate(activity).active());
    assertNoRecoveryDialog();
   });
   assertTrue("Accepted backup work still finishes after old owner destruction",
    BackupGuardian.verifyLatestPrivate(context).contains("JSON import validation passed"));
   assertEquals("All delayed operations preserve the full canonical ledger",original,canonical(persisted));
   passed("V283_RECOVERY_OLD_OWNER_COMPLETIONS_PASS",
    "Delayed backup, private verification, selected JSON verification and restore rehearsal are silent after recreation; no stale dialog or picker. JSON/PDF staging and receipt-preview files are removed.");

   // Exercise actual fresh launches and cancellation after the old gates have drained.
   scenario.onActivity(activity->invoke(activity,"startVerifiedExport",new Class<?>[]{}));
   await("Fresh owner opens a verified JSON export picker",a->
    field(a,"pendingExport") instanceof File&&picker.getHits()==1);
   instrumentation.waitForIdleSync();
   assertEquals("One fresh JSON picker launches",1,picker.getHits());
   AtomicReference<File> freshJson=new AtomicReference<>();
   scenario.onActivity(activity->{
    freshJson.set((File)field(activity,"pendingExport"));
    assertTrue("Fresh JSON gate owns its picker",jsonGate(activity).active());
   });
   assertTrue("Fresh JSON source is present",freshJson.get().isFile());
   assertEquals("Fresh source still verifies all members",15,
    MigrationCodec.inspect(new String(Files.readAllBytes(freshJson.get().toPath()),StandardCharsets.UTF_8)).players);
   scenario.onActivity(activity->{
    activity.onActivityResult(SAVE_JSON,Activity.RESULT_CANCELED,null);
    assertNull("Fresh cancellation clears pending JSON",field(activity,"pendingExport"));
    assertFalse("Fresh cancellation releases JSON gate",jsonGate(activity).active());
   });
   assertFalse("Fresh cancellation deletes staged JSON",freshJson.get().exists());
   scenario.onActivity(activity->invoke(activity,"savePdf",new Class<?>[]{String.class,MainActivity.RunnablePdf.class},
    "CI_fresh_recovery.pdf",(MainActivity.RunnablePdf)()->{writePdf(freshPdf);return freshPdf;}));
   await("Fresh owner opens a PDF export picker",a->
    field(a,"pendingReportPdf") instanceof File&&picker.getHits()==2);
   instrumentation.waitForIdleSync();
   assertEquals("Fresh PDF picker also launches",2,picker.getHits());
   assertTrue("Fresh PDF is staged before cancellation",freshPdf.isFile());
   scenario.onActivity(activity->{
    assertTrue("Fresh PDF gate owns its picker",pdfGate(activity).active());
    activity.onActivityResult(SAVE_PDF,Activity.RESULT_CANCELED,null);
    assertNull("Fresh cancellation clears pending PDF",field(activity,"pendingReportPdf"));
    assertFalse("Fresh cancellation releases PDF gate",pdfGate(activity).active());
   });
   assertFalse("Fresh cancellation deletes its generated PDF",freshPdf.exists());
   scenario.onActivity(activity->invoke(activity,"verifyPrivateBackup",new Class<?>[]{}));
   await("Live owner still presents a successful backup verification",a->dialog("Backup integrity check")!=null);
   scenario.onActivity(activity->{
    View result=dialog("Backup integrity check");
    assertNotNull("Live verification explains successful validation",findContaining(result,"JSON import validation passed"));
    assertTrue("Live verification remains dismissible",findText(result,"OK").performClick());
   });
   await("Live verification closes normally",a->dialog("Backup integrity check")==null);
   assertEquals("Fresh picker use and cancellation are read only",original,canonical(persisted));
   passed("V283_RECOVERY_FRESH_OWNER_RETRY_PASS",
    "Live JSON and PDF pickers each launch once and cancel cleanly; a new backup verification remains visible and dismissible.");

   // Use an isolated empty database only on the obsolete test Activity. The
   // normal onCreate of its replacement reconnects to the unchanged fixture.
   isolated=Room.inMemoryDatabaseBuilder(context,LedgerDatabase.class).allowMainThreadQueries().build();
   QueueHold importHold=holdQueue();
   detachedOwner=importHold.owner;
   detachedOriginal=field(detachedOwner,"database");
   final LedgerDatabase empty=isolated;
   scenario.onActivity(activity->{
    setField(activity,"database",empty);
    activity.onActivityResult(PICK_LEDGER,Activity.RESULT_OK,new Intent().setData(sourceUri));
   });
   scenario.recreate();
   await("Replacement keeps the persisted synthetic roster",a->findText(a.getWindow().getDecorView(),NAME)!=null);
   assertTrue("Import review's original Activity is destroyed",detachedOwner.isDestroyed());
   importHold.releaseAndAwait();
   instrumentation.waitForIdleSync();
   Object oldImportGate=field(detachedOwner,"importGate");
   assertEquals("Actual import worker staged one verified review",1L,field(oldImportGate,"sequence"));
   assertNull("Expired owner revokes the exact staged preview",field(oldImportGate,"pending"));
   assertEquals("Expired review cannot retain an active token",0L,field(oldImportGate,"activeSequence"));
   scenario.onActivity(activity->assertNull("No obsolete import confirmation is presented",dialog("Verified ledger ready")));
   assertEquals("Isolated database remains empty without confirmation",0,isolated.dao().metaCount());
   assertEquals("Production-shaped fixture is never replaced",original,canonical(persisted));
   setField(detachedOwner,"database",detachedOriginal);
   detachedOriginal=null;
   passed("V283_RECOVERY_IMPORT_REVIEW_ABANDONED_PASS",
    "Verified JSON review stages against an isolated empty in-memory database, then revokes its exact token after Activity recreation; neither database is imported.");

   // The same empty store lets the real dialog's system Back cancellation be
   // observed without ever presenting an import against the persisted ledger.
   QueueHold liveReviewHold=holdQueue();
   reviewOwner=liveReviewHold.owner;
   reviewOriginal=field(reviewOwner,"database");
   scenario.onActivity(activity->{
    setField(activity,"database",empty);
    activity.onActivityResult(PICK_LEDGER,Activity.RESULT_OK,new Intent().setData(sourceUri));
   });
   liveReviewHold.release.countDown();
   await("Live isolated review becomes visible",a->dialog("Verified ledger ready")!=null);
   Object liveImportGate=field(reviewOwner,"importGate");
   assertNotNull("Visible review owns a verified preview",field(liveImportGate,"pending"));
   assertEquals("Visible review has one precise token",1L,field(liveImportGate,"activeSequence"));
   instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
   await("System Back dismisses the import review",a->dialog("Verified ledger ready")==null);
   assertNull("System Back revokes the preview",field(liveImportGate,"pending"));
   assertEquals("System Back revokes the active token",0L,field(liveImportGate,"activeSequence"));
   assertEquals("System Back never imports the isolated database",0,isolated.dao().metaCount());
   final Object restoreReview=reviewOriginal;
   scenario.onActivity(activity->setField(activity,"database",restoreReview));
   reviewOriginal=null;
   assertEquals("Both import-review exits preserve persisted ledger",original,canonical(persisted));
   passed("V283_RECOVERY_IMPORT_REVIEW_BACK_PASS",
    "Real system Back dismisses the visible isolated import review and clears its exact token; no import is accepted.");

   scenario.onActivity(activity->invoke(activity,"selectTab",new Class<?>[]{String.class},"Reports"));
   await("Live Reports remains usable after all delayed completions",a->
    a.getWindow().getDecorView().findViewWithTag("reports:search")!=null);
   scenario.onActivity(activity->{
    assertEquals("Final live screen is Reports","Reports",field(activity,"tab"));
    assertFalse(activity.isDestroyed());assertFalse(activity.isFinishing());
    assertNoRecoveryDialog();
   });
   requireSyntheticFixture(persisted);
   assertEquals("Complete canonical ledger unchanged at end",original,canonical(persisted));
   assertEquals("Only the two intentional fresh picker requests occurred",2,picker.getHits());
   passed("V283_RECOVERY_LIFECYCLE_PASS","Synthetic ledger unchanged; active Reports screen usable; no external document was written.");
   captureEvidence();
  } finally {
   // Drain queued work before closing an injected database or deleting its source.
   for(QueueHold hold:holds)hold.release.countDown();
   if(scenario!=null)scenario.close();
   for(QueueHold hold:holds)hold.executor.awaitTermination(30,TimeUnit.SECONDS);
   instrumentation.waitForIdleSync();
   if(detachedOriginal!=null)setField(detachedOwner,"database",detachedOriginal);
   if(reviewOriginal!=null)setField(reviewOwner,"database",reviewOriginal);
   if(monitorInstalled)instrumentation.removeMonitor(picker);
   if(isolated!=null)isolated.close();
   for(File file:temporaryFiles)file.delete();
   persisted.close();
  }
 }

 private static void requireSyntheticFixture(LedgerDatabase db){
  assertNotNull("Synthetic fixture is required",db.dao().meta());
  assertEquals("Never run against a real club ledger",CLUB,db.dao().meta().club);
  assertEquals("Full synthetic roster is required",15,db.dao().members().size());
  Member found=null;for(Member member:db.dao().members())if(MEMBER.equals(member.id))found=member;
  assertNotNull("Synthetic immutable ID is present",found);
  assertEquals("Synthetic member identity is exact",NAME,found.name);
 }
 private File newTemporaryFile(String role,String suffix)throws Exception {
  File directory=new File(context.getCacheDir(),"pdfs");
  assertTrue("Provider-visible synthetic cache exists",directory.isDirectory()||directory.mkdirs());
  File file=new File(directory,"ci-v283-recovery-"+role+"-"+UUID.randomUUID()+suffix);
  temporaryFiles.add(file);return file;
 }
 private static void writePdf(File file)throws Exception {
  PdfDocument pdf=new PdfDocument();
  try {
   PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(842,595,1).create());
   Paint paint=new Paint();paint.setTextSize(24);
   page.getCanvas().drawText("CI synthetic recovery lifecycle PDF — no real club data",32,55,paint);
   pdf.finishPage(page);
   try(FileOutputStream output=new FileOutputStream(file)){pdf.writeTo(output);}
  } finally {pdf.close();}
  assertTrue("A real PDF was rendered",file.isFile()&&file.length()>100);
 }
 private Set<String> jsonStages(){
  Set<String> names=new TreeSet<>();
  File[] files=context.getCacheDir().listFiles((directory,name)->name.startsWith("Badminton_verified_export_")&&name.endsWith(".json"));
  if(files!=null)for(File file:files)names.add(file.getAbsolutePath());
  return names;
 }
 private QueueHold holdQueue()throws Exception {
  AtomicReference<MainActivity> owner=new AtomicReference<>();
  scenario.onActivity(owner::set);
  QueueHold hold=new QueueHold(owner.get());holds.add(hold);
  assertTrue("Existing worker tasks drain before deterministic hold",hold.entered.await(15,TimeUnit.SECONDS));
  return hold;
 }
 private static final class QueueHold {
  final MainActivity owner;
  final ExecutorService executor;
  final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  final AtomicBoolean timedOut=new AtomicBoolean();
  QueueHold(MainActivity owner){
   this.owner=owner;executor=(ExecutorService)field(owner,"io");
   executor.execute(()->{
    entered.countDown();
    try {if(!release.await(45,TimeUnit.SECONDS))timedOut.set(true);}
    catch(InterruptedException interrupted){Thread.currentThread().interrupt();timedOut.set(true);}
   });
  }
  void releaseAndAwait()throws Exception {
   assertTrue("Recreation shuts down only the old executor",executor.isShutdown());
   release.countDown();
   assertTrue("Already accepted recovery work drains after recreation",executor.awaitTermination(30,TimeUnit.SECONDS));
   assertFalse("Test releases the worker before its safety timeout",timedOut.get());
  }
 }
 private static PremiumV2795JsonExportGuard jsonGate(MainActivity activity){
  return (PremiumV2795JsonExportGuard)field(activity,"jsonExportGate");
 }
 private static PremiumV2794PdfSaveGuard pdfGate(MainActivity activity){
  return (PremiumV2794PdfSaveGuard)field(activity,"pdfSaveGate");
 }
 private static String canonical(LedgerDatabase db)throws Exception {
  return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(db)).ledger);
 }
 private static Object field(Object target,String name){
  try {Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
  catch(Exception issue){throw new AssertionError("Cannot inspect field "+name,issue);}
 }
 private static void setField(Object target,String name,Object value){
  try {Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
  catch(Exception issue){throw new AssertionError("Cannot set synthetic field "+name,issue);}
 }
 private static Object invoke(MainActivity activity,String name,Class<?>[] types,Object... args){
  try {Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,args);}
  catch(Exception issue){throw new AssertionError("Cannot invoke Activity method "+name,issue);}
 }
 private static View dialog(String title){
  for(View root:WindowInspector.getGlobalWindowViews())
   if(root.isAttachedToWindow()&&root.isShown()&&findText(root,title)!=null)return root;
  return null;
 }
 private static void assertNoRecoveryDialog(){
  for(String title:new String[]{"Verified backup status","Backup integrity check","Verified backup — read only",
    "Restore rehearsal — no account changes","Receipt preview","Verified ledger ready","Action not completed"})
   assertNull("No stale window remains: "+title,dialog(title));
 }
 private static View findText(View view,String expected){
  if(view==null)return null;
  if(view instanceof TextView&&expected.equals(((TextView)view).getText().toString()))return view;
  if(view instanceof ViewGroup){
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++){View found=findText(group.getChildAt(i),expected);if(found!=null)return found;}
  }
  return null;
 }
 private static View findContaining(View view,String expected){
  if(view==null)return null;
  if(view instanceof TextView&&((TextView)view).getText().toString().contains(expected))return view;
  if(view instanceof ViewGroup){
   ViewGroup group=(ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++){View found=findContaining(group.getChildAt(i),expected);if(found!=null)return found;}
  }
  return null;
 }
 private interface Ready {boolean check(MainActivity activity);}
 private void await(String message,Ready ready)throws Exception {
  long deadline=SystemClock.elapsedRealtime()+15000;
  AtomicBoolean complete=new AtomicBoolean();
  do {
   scenario.onActivity(activity->complete.set(ready.check(activity)));
   if(complete.get())return;
   Thread.sleep(50);
  }while(SystemClock.elapsedRealtime()<deadline);
  fail(message);
 }
 private void passed(String marker,String detail){
  evidence.append(marker).append('\n').append(detail).append("\n\n");
  System.out.println(marker);
 }
 private void captureEvidence()throws Exception {
  CountDownLatch committed=new CountDownLatch(1);
  scenario.onActivity(activity->{
   View root=activity.getWindow().getDecorView();
   assertTrue("Reports evidence is hardware accelerated",root.isHardwareAccelerated());
   root.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);
   root.postInvalidateOnAnimation();
  });
  assertTrue("Reports frame commits before capture",committed.await(5,TimeUnit.SECONDS));
  instrumentation.getUiAutomation().waitForIdle(250,5000);
  instrumentation.waitForIdleSync();
  File directory=context.getExternalFilesDir("premium-ui-evidence");
  assertNotNull("Evidence directory is available",directory);
  assertTrue(directory.isDirectory()||directory.mkdirs());
  Bitmap screenshot=instrumentation.getUiAutomation().takeScreenshot();
  assertNotNull("Reports screenshot is available",screenshot);
  try(FileOutputStream output=new FileOutputStream(new File(directory,"premium-recovery-after-recreation.png"))){
   assertTrue("Reports screenshot encodes",screenshot.compress(Bitmap.CompressFormat.PNG,100,output));
  }finally{screenshot.recycle();}
  Files.write(new File(directory,"premium-recovery-lifecycle.txt").toPath(),evidence.toString().getBytes(StandardCharsets.UTF_8));
 }
}

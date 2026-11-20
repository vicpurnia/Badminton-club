package com.vinayak.badminton.nativepreview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Run LAST: retained native forms, synthetic metadata writes, no cash writes or resets. */
@RunWith(AndroidJUnit4.class)
public final class V283FormWorkflowTest {
 private static final String MEMBER="ci-upgrade-synthetic-member-20261114";
 private static final String OTHER="ci-upgrade-synthetic-roster-2";
 private static final String NAME="CI Synthetic Member (no real club data)";
 private static final String OTHER_NAME="CI Synthetic Player 02";
 private static final String MONTH="2026-08";
 private static final String PROFILE_NAME="CI Synthetic Retained Profile";
 private static final String POLICY="Audited fee policy";
 private static final String WAIVER="Record authorized noncash concession";
 private static final String THRESHOLD="Low-stock alert · recorded counts only";
 private static final String PROFILE="Member profile · audited";
 private static final String ENCRYPT="Encrypt verified migration backup";
 private Instrumentation instrumentation;
 private MainActivity activity;
 private LedgerDatabase database;
 private final List<QueueHold> holds=new ArrayList<>();

 @Test public void rejectedFormsRetainValuesAndSafeRetriesCommitOnce()throws Exception {
  instrumentation=InstrumentationRegistry.getInstrumentation();
  Context context=instrumentation.getTargetContext();
  database=Room.databaseBuilder(context,LedgerDatabase.class,"badminton_room_v18.db")
   .allowMainThreadQueries().build();
  IntentFilter filter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);
  filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("application/octet-stream");
  // Observe the real encrypted export launch; never open or grant a real provider.
  Instrumentation.ActivityMonitor picker=new Instrumentation.ActivityMonitor(filter,null,true);
  boolean monitorInstalled=false;
  ActivityScenario<MainActivity> scenario=null;
  try {
   requireSyntheticFixture();
   assertEquals("This final test starts before any synthetic profile writes",0,eventCount(PremiumV26State.PROFILE_KEY));
   assertEquals("This final test starts before any synthetic threshold writes",0,eventCount(PremiumV26State.STOCK_KEY));
   assertTrue("No fee policy is installed by this test",PremiumV26State.policies(raw(),knownMembers()).isEmpty());
   assertTrue("No concession is installed by this test",PremiumV26State.waivers(raw(),knownMembers()).isEmpty());
   final String before=canonical();
   final String originalRows=preservedRoomRows();
   final String originalAudit=auditRows();
   final int originalAuditCount=database.dao().auditEvents().size();
   final List<Member> members=database.dao().members();
   final Member member=findMember(members,MEMBER);
   instrumentation.addMonitor(picker);monitorInstalled=true;
   scenario=ActivityScenario.launch(MainActivity.class);
   scenario.onActivity(value->activity=value);
   onMain(()->{setField("reportMonth",MONTH);invoke("selectTab",new Class<?>[]{String.class},"Members");});
   await("Synthetic member screen",()->findText(decor(),NAME)!=null);

   onMain(()->invoke("editFeePolicy",new Class<?>[]{List.class},members));
   awaitDialog(POLICY);
   setText(POLICY,"Monthly amount in INR","100000001");
   setText(POLICY,"Effective YYYY-MM",MONTH);
   setText(POLICY,"Required reason, 8–200 characters","CI retained fee policy reason");
   onMain(()->findAll(requireDialog(POLICY),Spinner.class).get(0).setSelection(indexOf(members,MEMBER)+1));
   FormState policy=snapshot(POLICY);
   clickDialog(POLICY,"Review");
   expectError("Check fee, effective YYYY-MM and reason.",policy);
   setText(POLICY,"Monthly amount in INR","1800");
   confirmCancelAndBack(POLICY,"Confirm effective-dated fee");
   clickDialog(POLICY,"Cancel");awaitClosed(POLICY);
   assertEquals("Invalid policy and both review exits are read only",before,canonical());
   System.out.println("V283_FEE_POLICY_RETAINED_PASS");

   onMain(()->invoke("editFeeWaiver",new Class<?>[]{List.class},members));
   awaitDialog(WAIVER);
   setText(WAIVER,"Fee month YYYY-MM",MONTH);
   setText(WAIVER,"Concession INR","0");
   setText(WAIVER,"Authorized concession reason 8–200 chars","CI retained concession reason");
   onMain(()->findAll(requireDialog(WAIVER),Spinner.class).get(0).setSelection(indexOf(members,OTHER)));
   FormState waiver=snapshot(WAIVER);
   clickDialog(WAIVER,"Review");
   expectError("Check concession, month and reason.",waiver);
   setText(WAIVER,"Concession INR","25");
   confirmCancelAndBack(WAIVER,"Confirm concession, not a cash receipt");
   // The fixture deliberately has no fee policy. Reach the actual transactional
   // rejection, then wait for its visible error before asserting retained input.
   FormState rejectedWaiver=snapshot(WAIVER);
   clickDialog(WAIVER,"Review");awaitDialog("Confirm concession, not a cash receipt");
   requireSyntheticFixture();
   clickDialog("Confirm concession, not a cash receipt","Save");
   expectError("Fee must be configured and remaining concession cannot exceed its rate",rejectedWaiver);
   clickDialog(WAIVER,"Cancel");awaitClosed(WAIVER);
   assertEquals("Rejected concession cannot add an event or cash receipt",before,canonical());
   System.out.println("V283_CONCESSION_RETAINED_ASYNC_FAILURE_PASS");

   onMain(()->invoke("openStockThreshold",new Class<?>[]{}));awaitDialog(THRESHOLD);
   setText(THRESHOLD,"Individual shuttles","1000001");
   setText(THRESHOLD,"Required audit reason (8–200 chars)","CI retained stock threshold reason");
   FormState threshold=snapshot(THRESHOLD);
   clickDialog(THRESHOLD,"Review");
   expectError("Set a quantity up to one million and meaningful reason.",threshold);
   setText(THRESHOLD,"Individual shuttles","17");
   confirmCancelAndBack(THRESHOLD,"Confirm reorder threshold");
   clickDialog(THRESHOLD,"Cancel");awaitClosed(THRESHOLD);
   assertEquals("Threshold validation and review cancellation are read only",before,canonical());
   System.out.println("V283_THRESHOLD_RETAINED_PASS");

   onMain(()->invoke("addMember",new Class<?>[]{}));awaitDialog("New player");
   String tooLong=new String(new char[101]).replace('\u0000','N');
   setText("New player","Full player name",tooLong);
   FormState longName=snapshot("New player");
   requireSyntheticFixture();clickDialog("New player","Add");
   expectError("Name must be 1–100 characters",longName);
   setText("New player","Full player name",OTHER_NAME);
   FormState duplicateName=snapshot("New player");
   requireSyntheticFixture();clickDialog("New player","Add");
   expectError("Possible duplicate member:",duplicateName);
   clickDialog("New player","Cancel");awaitClosed("New player");
   assertEquals("Rejected player additions preserve all original rows",before,canonical());
   System.out.println("V283_NEW_MEMBER_RETAINED_ASYNC_REJECTION_PASS");

   onMain(()->invoke("openMemberProfile",new Class<?>[]{Member.class},member));awaitDialog(PROFILE);
   setProfileName(PROFILE_NAME);
   setText(PROFILE,"Optional contact","CI-only retained contact");
   setText(PROFILE,"Joining date YYYY-MM-DD","2026-08-01");
   setText(PROFILE,"Optional notes","CI-only retained profile notes");
   onMain(()->findAll(requireDialog(PROFILE),CheckBox.class).get(0).setChecked(false));
   confirmCancelAndBack(PROFILE,"Save member profile?");
   onMain(()->findAll(requireDialog(PROFILE),CheckBox.class).get(0).setChecked(true));
   setText(PROFILE,"Joining date YYYY-MM-DD","2026-02-30");
   FormState badDate=snapshot(PROFILE);
   clickDialog(PROFILE,"Review");awaitDialog("Save member profile?");
   requireSyntheticFixture();clickDialog("Save member profile?","Save");
   expectError("Joining date must be a valid calendar day",badDate);
   assertEquals("Invalid joining date cannot write a profile event",before,canonical());
   setText(PROFILE,"Joining date YYYY-MM-DD","2026-08-01");
   setProfileName(OTHER_NAME);
   FormState duplicateProfile=snapshot(PROFILE);
   clickDialog(PROFILE,"Review");awaitDialog("Save member profile?");
   requireSyntheticFixture();clickDialog("Save member profile?","Save");
   expectError("Possible duplicate member name; profile change canceled",duplicateProfile);
   assertEquals("Duplicate profile rejection preserves every original field",before,canonical());
   System.out.println("V283_PROFILE_RETAINED_ASYNC_FAILURE_PASS");

   // Correct the very same retained form. A repeated confirmation callback
   // cannot enqueue another metadata write or dismiss before the first succeeds.
   setProfileName(PROFILE_NAME);
   clickDialog(PROFILE,"Review");awaitDialog("Save member profile?");
   requireSyntheticFixture();
   QueueHold profileHold=holdQueue();
   try {
    clickTwice("Save member profile?","Save");
    assertSavingForm(PROFILE);
    assertEquals("Queued profile has not committed before its worker runs",before,canonical());
   } finally {profileHold.release.countDown();}
   drainQueue();awaitClosed(PROFILE);
   assertEquals("Corrected retry creates exactly one profile event",1,eventCount(PremiumV26State.PROFILE_KEY));
   PremiumV26State.Profile saved=PremiumV26State.profiles(raw(),knownMembers()).get(MEMBER);
   assertNotNull(saved);assertEquals(PROFILE_NAME,saved.name);
   assertEquals("CI-only retained contact",saved.contact);assertEquals("2026-08-01",saved.joined);
   assertEquals("CI-only retained profile notes",saved.notes);assertTrue(saved.active);
   assertEquals("Exactly one profile audit event",originalAuditCount+1,database.dao().auditEvents().size());
   assertEquals("Safe profile retry preserves original Room records",originalRows,preservedRoomRows());
   System.out.println("V283_PROFILE_CORRECTED_RETRY_ONCE_PASS");

   onMain(()->invoke("openStockThreshold",new Class<?>[]{}));awaitDialog(THRESHOLD);
   setText(THRESHOLD,"Individual shuttles","1000001");
   setText(THRESHOLD,"Required audit reason (8–200 chars)","CI safe threshold retry reason");
   FormState badThreshold=snapshot(THRESHOLD);
   clickDialog(THRESHOLD,"Review");
   expectError("Set a quantity up to one million and meaningful reason.",badThreshold);
   setText(THRESHOLD,"Individual shuttles","17");
   clickDialog(THRESHOLD,"Review");awaitDialog("Confirm reorder threshold");
   requireSyntheticFixture();
   QueueHold thresholdHold=holdQueue();
   try {clickTwice("Confirm reorder threshold","Save");assertSavingForm(THRESHOLD);}
   finally {thresholdHold.release.countDown();}
   drainQueue();awaitClosed(THRESHOLD);
   assertEquals("Corrected threshold retry commits exactly one event",1,eventCount(PremiumV26State.STOCK_KEY));
   assertEquals(17L,PremiumV26State.reorderThreshold(raw()));
   assertEquals("Corrected save keeps the typed audit reason","CI safe threshold retry reason",
    new JSONObject(raw()).getJSONArray(PremiumV26State.STOCK_KEY).getJSONObject(0).getString("reason"));
   assertEquals("Two successful metadata forms create exactly two audit rows",originalAuditCount+2,database.dao().auditEvents().size());
   assertEquals("Threshold warning is not an inventory or cash adjustment",originalRows,preservedRoomRows());
   assertEquals("All original audit events remain byte-equivalent",originalAudit,auditRowsExcludingNewMetadata());
   System.out.println("V283_THRESHOLD_CORRECTED_RETRY_ONCE_PASS");

   final String beforeEncrypted=canonical();
   onMain(()->invoke("startEncryptedExport",new Class<?>[]{}));awaitDialog(ENCRYPT);
   setText(ENCRYPT,"Strong password, 12+ characters","CI-only-password-20261119");
   setText(ENCRYPT,"Repeat password","CI-only-password-mismatch");
   FormState mismatch=snapshot(ENCRYPT);
   clickDialog(ENCRYPT,"Encrypt & save");
   expectError("Passwords must match and contain at least 12 characters.",mismatch);
   onMain(()->assertTrue("Mismatch retains the same encrypted export session",encryptedGate().inProgress()));
   assertEquals("Invalid password cannot open a picker",0,picker.getHits());
   clickDialog(ENCRYPT,"Cancel");awaitClosed(ENCRYPT);
   onMain(()->assertFalse("Explicit Cancel releases encrypted gate",encryptedGate().inProgress()));

   onMain(()->invoke("startEncryptedExport",new Class<?>[]{}));awaitDialog(ENCRYPT);
   setText(ENCRYPT,"Strong password, 12+ characters","CI-only-password-20261119");
   setText(ENCRYPT,"Repeat password","CI-only-password-mismatch");
   FormState secondMismatch=snapshot(ENCRYPT);
   clickDialog(ENCRYPT,"Encrypt & save");
   expectError("Passwords must match and contain at least 12 characters.",secondMismatch);
   setText(ENCRYPT,"Repeat password","CI-only-password-20261119");
   clickTwice(ENCRYPT,"Encrypt & save");
   await("One encrypted export reaches its actual picker",()->picker.getHits()==1&&field("pendingEncryptedExport") instanceof File);
   awaitClosed(ENCRYPT);
   AtomicReference<File> staged=new AtomicReference<>();
   onMain(()->{
    assertTrue("One live picker owns the encrypted gate",encryptedGate().inProgress());
    staged.set((File)field("pendingEncryptedExport"));
    assertNotNull(staged.get());assertTrue("Encrypted staging is nonempty",staged.get().isFile()&&staged.get().length()>0);
    activity.onActivityResult(2010,Activity.RESULT_CANCELED,null);
    assertNull("Picker cancel clears the stage reference",field("pendingEncryptedExport"));
    assertFalse("Picker cancel releases encrypted gate",encryptedGate().inProgress());
   });
   assertFalse("Picker cancel removes its private encrypted stage",staged.get().exists());
   drainQueue();assertEquals("Corrected export launches exactly one picker",1,picker.getHits());
   assertEquals("Encrypted retries and cancellation never modify the ledger",beforeEncrypted,canonical());
   onMain(()->assertNull("No unexpected error was hidden",dialog("Action not completed")));
   assertEquals("Original cash, stock, roster and metadata remain unchanged",originalRows,preservedRoomRows());
   assertEquals(15,database.dao().members().size());assertEquals(2,database.dao().contributions().size());
   assertEquals(1,database.dao().expenses().size());assertEquals(1,database.dao().stockUsages().size());
   for(QueueHold hold:holds)assertFalse("Queue holds must be released explicitly",hold.timedOut.get());
   System.out.println("V283_ENCRYPTED_FORM_RETRY_AND_CANCEL_PASS");
   System.out.println("V283_RETAINED_FORM_WORKFLOWS_PASS: six forms; exactly two synthetic metadata events; original financial records unchanged");
  } finally {
   for(QueueHold hold:holds)hold.release.countDown();
   if(scenario!=null)scenario.close();
   for(QueueHold hold:holds)hold.executor.awaitTermination(15,TimeUnit.SECONDS);
   if(monitorInstalled)instrumentation.removeMonitor(picker);
   database.close();
  }
 }

 private void requireSyntheticFixture(){
  assertNotNull("Synthetic fixture is required before any submit",database.dao().meta());
  assertEquals("Never write a real club ledger","CI Synthetic Badminton Club — no real data",database.dao().meta().club);
  List<Member> members=database.dao().members();assertEquals("Exact synthetic roster required",15,members.size());
  assertEquals(NAME,findMember(members,MEMBER).name);assertEquals(OTHER_NAME,findMember(members,OTHER).name);
  for(Member member:members)assertTrue("Only CI member identities may be edited",
   MEMBER.equals(member.id)||member.id.matches("ci-upgrade-synthetic-roster-([2-9]|1[0-5])"));
  Contribution cash=null;
  for(Contribution item:database.dao().contributions())if("pay-ci-upgrade-synthetic-20261114".equals(item.id))cash=item;
  assertNotNull("Original known synthetic cash receipt is required",cash);
  assertEquals(MEMBER,cash.memberId);assertEquals(1379L,cash.amount);assertEquals(MONTH,cash.feeMonth);
 }
 private static Member findMember(List<Member> members,String id){
  for(Member member:members)if(id.equals(member.id))return member;
  throw new AssertionError("Missing synthetic member "+id);
 }
 private static int indexOf(List<Member> members,String id){
  for(int i=0;i<members.size();i++)if(id.equals(members.get(i).id))return i;
  throw new AssertionError("Missing synthetic member "+id);
 }
 private Set<String> knownMembers(){Set<String> ids=new HashSet<>();for(Member member:database.dao().members())ids.add(member.id);return ids;}
 private String raw(){return database.dao().meta().inventoryStockJson;}
 private int eventCount(String key)throws Exception {JSONArray values=new JSONObject(raw()).optJSONArray(key);return values==null?0:values.length();}
 private String canonical()throws Exception {return MigrationCodec.canonical(MigrationCodec.inspect(MigrationCodec.exportVerified(database)).ledger);}
 private String auditRows()throws Exception{return auditRows(false);}
 private String auditRowsExcludingNewMetadata()throws Exception{return auditRows(true);}
 private String auditRows(boolean excludeNew)throws Exception {
  JSONArray rows=new JSONArray();
  for(AuditEvent event:database.dao().auditEvents()){
   if(excludeNew&&("Member profile".equals(event.action)||"Shuttle threshold".equals(event.action)))continue;
   rows.put(new JSONObject().put("id",event.id).put("at",event.createdAt).put("action",event.action).put("details",event.details));
  }
  return MigrationCodec.canonical(rows);
 }
 /** Preserve every column of every original user table, allowing ONLY the two tested additive metadata arrays. */
 private String preservedRoomRows()throws Exception {
  androidx.sqlite.db.SupportSQLiteDatabase sql=database.getOpenHelper().getReadableDatabase();
  TreeSet<String> tables=new TreeSet<>();
  try(Cursor cursor=sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table','room_table_modification_log','android_metadata','audit_events')")){
   while(cursor.moveToNext())tables.add(cursor.getString(0));
  }
  JSONObject snapshot=new JSONObject();
  for(String table:tables){
   assertTrue(table.matches("[A-Za-z0-9_]+"));JSONArray rows=new JSONArray();
   try(Cursor cursor=sql.query("SELECT * FROM \""+table+"\" ORDER BY rowid")){
    String[] names=cursor.getColumnNames();
    while(cursor.moveToNext()){
     JSONObject row=new JSONObject();
     for(int i=0;i<names.length;i++){
      Object value;
      switch(cursor.getType(i)){
       case Cursor.FIELD_TYPE_NULL:value=JSONObject.NULL;break;
       case Cursor.FIELD_TYPE_INTEGER:value=cursor.getLong(i);break;
       case Cursor.FIELD_TYPE_FLOAT:value=cursor.getDouble(i);break;
       case Cursor.FIELD_TYPE_BLOB:value=android.util.Base64.encodeToString(cursor.getBlob(i),android.util.Base64.NO_WRAP);break;
       default:value=cursor.getString(i);break;
      }
      if("ledger_meta".equals(table)&&"inventoryStockJson".equals(names[i])){
       JSONObject metadata=new JSONObject(String.valueOf(value));
       metadata.remove(PremiumV26State.PROFILE_KEY);metadata.remove(PremiumV26State.STOCK_KEY);
       value=MigrationCodec.canonical(metadata);
      }
      row.put(names[i],value);
     }
     rows.put(row);
    }
   }
   snapshot.put(table,rows);
  }
  return MigrationCodec.canonical(snapshot);
 }

 private void confirmCancelAndBack(String form,String confirmation)throws Exception {
  FormState original=snapshot(form);
  clickDialog(form,"Review");awaitDialog(confirmation);
  clickDialog(confirmation,"Cancel");awaitClosed(confirmation);assertRetained(original);
  clickDialog(form,"Review");awaitDialog(confirmation);
  backUntilClosed(confirmation);assertRetained(original);
 }
 private void expectError(String message,FormState original)throws Exception {
  await("Actual rejection: "+message,()->dialog("Action not completed")!=null&&
   findTextContaining(requireDialog("Action not completed"),message)!=null);
  assertRetained(original);
  clickDialog("Action not completed","OK");awaitClosed("Action not completed");
  assertRetained(original);
 }
 private void setText(String title,String hint,String text){onMain(()->{
  EditText input=findHint(requireDialog(title),hint);assertNotNull("Field "+hint,input);input.setText(text);
 });}
 private void setProfileName(String text){onMain(()->findAll(requireDialog(PROFILE),EditText.class).get(0).setText(text));}
 private FormState snapshot(String title){AtomicReference<FormState> state=new AtomicReference<>();onMain(()->state.set(new FormState(title,requireDialog(title))));return state.get();}
 private void assertRetained(FormState state){onMain(()->{
  View root=requireDialog(state.title);assertSame("Original form remains attached: "+state.title,state.root,root);
  List<EditText> fields=findAll(root,EditText.class);assertEquals(state.inputs.size(),fields.size());
  for(int i=0;i<fields.size();i++){
   assertSame("Original input view retained",state.inputs.get(i),fields.get(i));
   assertEquals("Typed value retained in "+state.title+" field "+i,state.values.get(i),fields.get(i).getText().toString());
   assertTrue("Rejected input is editable again",fields.get(i).isEnabled());
  }
  List<Spinner> spinners=findAll(root,Spinner.class);
  for(int i=0;i<spinners.size();i++)assertEquals("Selected identity retained",state.selections.get(i).intValue(),spinners.get(i).getSelectedItemPosition());
  List<CheckBox> checks=findAll(root,CheckBox.class);
  for(int i=0;i<checks.size();i++)assertEquals("Checkbox state retained",state.checked.get(i),Boolean.valueOf(checks.get(i).isChecked()));
  assertTrue("Form can be reviewed again",root.findViewById(android.R.id.button1).isEnabled());
  assertTrue("Form can be explicitly canceled",root.findViewById(android.R.id.button2).isEnabled());
 });}
 private void assertSavingForm(String title){onMain(()->{
  View root=requireDialog(title);
  assertFalse("In-flight save cannot be repeated",root.findViewById(android.R.id.button1).isEnabled());
  assertFalse("In-flight save cannot be canceled as if it were unsubmitted",root.findViewById(android.R.id.button2).isEnabled());
  for(EditText input:findAll(root,EditText.class))assertFalse("Submitted values cannot change while commit is pending",input.isEnabled());
 });}
 private static final class FormState {
  final String title;final View root;final List<EditText> inputs;
  final List<String> values=new ArrayList<>();final List<Integer> selections=new ArrayList<>();final List<Boolean> checked=new ArrayList<>();
  FormState(String title,View root){
   this.title=title;this.root=root;inputs=findAll(root,EditText.class);
   for(EditText input:inputs)values.add(input.getText().toString());
   for(Spinner spinner:findAll(root,Spinner.class))selections.add(spinner.getSelectedItemPosition());
   for(CheckBox checkbox:findAll(root,CheckBox.class))checked.add(checkbox.isChecked());
  }
 }
 private QueueHold holdQueue()throws Exception {
  AtomicReference<ExecutorService> executor=new AtomicReference<>();onMain(()->executor.set((ExecutorService)field("io")));
  QueueHold hold=new QueueHold(executor.get());holds.add(hold);
  assertTrue("Worker reached bounded deterministic hold",hold.entered.await(15,TimeUnit.SECONDS));return hold;
 }
 private void drainQueue()throws Exception {
  CountDownLatch drained=new CountDownLatch(1);
  onMain(()->((ExecutorService)field("io")).execute(drained::countDown));
  assertTrue("Submitted work drains within the bound",drained.await(20,TimeUnit.SECONDS));
  instrumentation.waitForIdleSync();
 }
 private static final class QueueHold {
  final ExecutorService executor;final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  final AtomicBoolean timedOut=new AtomicBoolean();
  QueueHold(ExecutorService executor){this.executor=executor;executor.execute(()->{
   entered.countDown();try{if(!release.await(30,TimeUnit.SECONDS))timedOut.set(true);}
   catch(InterruptedException issue){Thread.currentThread().interrupt();timedOut.set(true);}
  });}
 }
 private PremiumV2793ExportGate encryptedGate(){return (PremiumV2793ExportGate)field("encryptedExportGate");}
 private View decor(){return activity.getWindow().getDecorView();}
 private void clickDialog(String title,String label)throws Exception {click(title,label,false);}
 private void clickTwice(String title,String label)throws Exception {click(title,label,true);}
 private void click(String title,String label,boolean twice)throws Exception {
  awaitDialog(title);onMain(()->{
   View action=clickable(findText(requireDialog(title),label));assertNotNull("Action "+label,action);
   action.requestRectangleOnScreen(new Rect(0,0,action.getWidth(),action.getHeight()),true);
  });twoFrames();
  onMain(()->{
   View action=clickable(findText(requireDialog(title),label));assertNotNull(label,action);assertTrue(label+" is enabled",action.isEnabled());
   assertTrue(label+" dispatches through the real control",action.performClick());
   if(twice)action.performClick();
  });
 }
 private void backUntilClosed(String title)throws Exception {
  AtomicBoolean closed=new AtomicBoolean();
  for(int attempt=0;attempt<3&&!closed.get();attempt++){
   await("Dialog focus before Back: "+title,()->{
    assertActivityAlive();View target=dialog(title);closed.set(target==null);return closed.get()||target.hasWindowFocus();
   });
   if(closed.get())break;
   instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
   long deadline=SystemClock.elapsedRealtime()+1500;
   do{onMain(()->{assertActivityAlive();closed.set(dialog(title)==null);});if(closed.get())break;Thread.sleep(50);}
   while(SystemClock.elapsedRealtime()<deadline);
  }
  assertTrue("Back must close only the confirmation: "+title,closed.get());twoFrames();
 }
 private void awaitDialog(String title)throws Exception{await("Dialog "+title,()->dialog(title)!=null);}
 private void awaitClosed(String title)throws Exception{await("Closed "+title,()->dialog(title)==null);twoFrames();}
 private View requireDialog(String title){View result=dialog(title);assertNotNull("Dialog "+title,result);return result;}
 private View dialog(String title){
  List<View> windows=WindowInspector.getGlobalWindowViews();
  for(int i=windows.size()-1;i>=0;i--){View root=windows.get(i);
   if(root!=decor()&&root.isAttachedToWindow()&&root.isShown()&&findText(root,title)!=null)return root;
  }return null;
 }
 private static View findText(View root,String text){
  if(root instanceof TextView&&text.equalsIgnoreCase(((TextView)root).getText().toString()))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=findText(group.getChildAt(i),text);if(found!=null)return found;}}
  return null;
 }
 private static View findTextContaining(View root,String text){
  if(root instanceof TextView&&((TextView)root).getText().toString().contains(text))return root;
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=findTextContaining(group.getChildAt(i),text);if(found!=null)return found;}}
  return null;
 }
 private static EditText findHint(View root,String hint){
  for(EditText input:findAll(root,EditText.class))if(hint.contentEquals(input.getHint()==null?"":input.getHint()))return input;
  return null;
 }
 private static <T extends View> List<T> findAll(View root,Class<T> type){List<T> result=new ArrayList<>();collect(root,type,result);return result;}
 private static <T extends View> void collect(View root,Class<T> type,List<T> result){
  if(type.isInstance(root))result.add(type.cast(root));
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),type,result);}
 }
 private static View clickable(View value){for(View view=value;view!=null;view=view.getParent() instanceof View?(View)view.getParent():null)if(view.isClickable())return view;return null;}
 private void assertActivityAlive(){assertNotNull(activity);assertFalse("Activity must remain alive",activity.isFinishing()||activity.isDestroyed());}
 private void await(String label,BooleanSupplier ready)throws Exception {
  long end=SystemClock.elapsedRealtime()+15000;AtomicBoolean done=new AtomicBoolean();
  do{onMain(()->{assertActivityAlive();done.set(ready.getAsBoolean());});if(done.get())return;Thread.sleep(100);}while(SystemClock.elapsedRealtime()<end);
  fail("Timed out: "+label+"\n"+windowDiagnostic());
 }
 private void onMain(Runnable work){AtomicReference<Throwable> failure=new AtomicReference<>();
  instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable issue){failure.set(issue);}});
  if(failure.get()!=null)throw new AssertionError("UI assertion failed",failure.get());
 }
 private Object invoke(String name,Class<?>[] types,Object...values){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,values);}catch(Exception issue){throw new AssertionError(name,issue);}}
 private Object field(String name){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}catch(Exception issue){throw new AssertionError(name,issue);}}
 private void setField(String name,Object value){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(activity,value);}catch(Exception issue){throw new AssertionError(name,issue);}}
 private void twoFrames()throws Exception {
  CountDownLatch drawn=new CountDownLatch(1);onMain(()->Choreographer.getInstance().postFrameCallback(first->Choreographer.getInstance().postFrameCallback(second->drawn.countDown())));
  assertTrue("UI draws two frames",drawn.await(5,TimeUnit.SECONDS));instrumentation.waitForIdleSync();
 }
 private String windowDiagnostic(){StringBuilder output=new StringBuilder();onMain(()->{
  for(View root:WindowInspector.getGlobalWindowViews())if(root.isShown()){
   output.append("WINDOW\n");int remaining=30;
   for(TextView text:findAll(root,TextView.class)){if(remaining--<=0)break;String value=text.getText().toString().replace('\n',' ');if(!value.isEmpty())output.append(value.substring(0,Math.min(120,value.length()))).append('\n');}
  }
 });return output.toString();}
}

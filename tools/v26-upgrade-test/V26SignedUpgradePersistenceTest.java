package com.vinayak.badminton.nativepreview;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Synthetic ONLY: signed v25.9 -> signed v26.9 same-package persistence.
 * Builds as a separate Android instrumentation APK in CI. Never in release APK.
 */
@RunWith(AndroidJUnit4.class)
public final class V26SignedUpgradePersistenceTest {
 private static final String DB_NAME="badminton_room_v18.db";
 private static final String MEMBER_ID="ci-upgrade-synthetic-member-20261114";
 private static final String PAYMENT_ID="pay-ci-upgrade-synthetic-20261114";
 private static final String EXPENSE_ID="ci-upgrade-synthetic-expense-20261114";
 private static final String USAGE_ID="ci-upgrade-synthetic-usage-20261114";
 private static final String MONTH="2026-08";
 private static final String META_AUDIT="{\"ciUnknownMetadata\":{\"member\":\"synthetic\",\"shuttles\":24}}";
 private static final String LEGACY_KEY="ci-upgrade-legacy-2025-12";
 private static final String DONATED_KEY="ci-upgrade-legacy-pack-2025-12";

 private LedgerDatabase open() {
  Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
  return Room.databaseBuilder(target,LedgerDatabase.class,DB_NAME)
    .allowMainThreadQueries().build();
 }

 private static final String SNAPSHOT_FILE="ci-synthetic-all-room-records.sha256";
 private static final String JSON_FINGERPRINT_FILE="ci-synthetic-verified-ledger.sha256";
 private static String fullRoomDigest(LedgerDatabase db)throws Exception{
  // Include every user table and field, even ones not anticipated by explicit
  // transaction assertions. Ignore SQLite/Room internal implementation tables.
  java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");
  androidx.sqlite.db.SupportSQLiteDatabase sql=db.getOpenHelper().getReadableDatabase();
  TreeSet<String> tables=new TreeSet<>();
  try(Cursor c=sql.query("SELECT name FROM sqlite_master WHERE type='table' "+
       "AND name NOT LIKE 'sqlite_%' AND name NOT IN "+
       "('room_master_table','room_table_modification_log','android_metadata')")){
   while(c.moveToNext())tables.add(c.getString(0));
  }
  assertTrue("Expected multiple user ledger tables",tables.size()>=6);
  for(String name:tables){
   assertTrue("Untrusted SQLite table identifier",name.matches("[A-Za-z0-9_]+"));
   md.update(("TABLE:"+name+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
   try(Cursor c=sql.query("SELECT * FROM \""+name+"\" ORDER BY rowid")){
    String[] keys=c.getColumnNames();
    md.update(("COLUMNS:"+String.join("|",keys)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    while(c.moveToNext()){
     for(int i=0;i<keys.length;i++){
      String token;
      switch(c.getType(i)){
       case Cursor.FIELD_TYPE_NULL:token="NULL";break;
       case Cursor.FIELD_TYPE_INTEGER:token="INT:"+c.getLong(i);break;
       case Cursor.FIELD_TYPE_FLOAT:token="FLOAT:"+Double.toString(c.getDouble(i));break;
       case Cursor.FIELD_TYPE_BLOB:
        token="BLOB:"+android.util.Base64.encodeToString(c.getBlob(i),android.util.Base64.NO_WRAP);break;
       default:token="TEXT:"+c.getString(i);break;
      }
      byte[] utf8=token.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      md.update(java.nio.ByteBuffer.allocate(4).putInt(utf8.length).array());
      md.update(utf8);
     }
     md.update((byte)'\n');
    }
   }
  }
  StringBuilder out=new StringBuilder();
  for(byte value:md.digest())out.append(String.format(Locale.ROOT,"%02x",value&0xff));
  return out.toString();
 }
 private static void preserveFingerprint(LedgerDatabase db)throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String fingerprint=fullRoomDigest(db);
  java.io.File file=new java.io.File(context.getFilesDir(),SNAPSHOT_FILE);
  try(java.io.FileOutputStream stream=new java.io.FileOutputStream(file)){
   stream.write(fingerprint.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   stream.getFD().sync(); // Ensure baseline is actually persisted before APK replacement
  }
  assertTrue("Baseline fingerprint must be persisted",file.isFile()&&file.length()==64);
 }
 private static void assertFingerprintUnchanged(LedgerDatabase db)throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  java.io.File baseline=new java.io.File(context.getFilesDir(),SNAPSHOT_FILE);
  assertTrue("Upgrade must preserve original private baseline evidence",baseline.isFile());
  byte[] bytes=java.nio.file.Files.readAllBytes(baseline.toPath());
  String old=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
  assertEquals("All user-table fields must be byte-equivalent after signed in-place upgrade",
   old,fullRoomDigest(db));
 }
 private static void preserveVerifiedLedgerDigest(LedgerDatabase db)throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String exported=MigrationCodec.exportVerified(db);
  MigrationCodec.Preview preview=MigrationCodec.inspect(exported);
  assertEquals("Only synthetic member must be exported",1,preview.players);
  assertEquals("Only synthetic payment must be exported",1,preview.payments);
  assertEquals("Original historical legacy cash+noncash must be exported",2,
   preview.ledger.getJSONArray("legacy").length());
  java.io.File file=new java.io.File(context.getFilesDir(),JSON_FINGERPRINT_FILE);
  try(java.io.FileOutputStream stream=new java.io.FileOutputStream(file)){
   stream.write(MigrationCodec.sha256(MigrationCodec.canonical(preview.ledger))
    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
   stream.getFD().sync();
  }
  assertEquals(64L,file.length());
 }
 private static void assertVerifiedLedgerDigestUnchanged(LedgerDatabase db)throws Exception{
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  java.io.File baseline=new java.io.File(context.getFilesDir(),JSON_FINGERPRINT_FILE);
  assertTrue("Pre-upgrade verified ledger fingerprint must survive",baseline.isFile());
  String previous=new String(java.nio.file.Files.readAllBytes(baseline.toPath()),
   java.nio.charset.StandardCharsets.UTF_8);
  MigrationCodec.Preview verified=MigrationCodec.inspect(MigrationCodec.exportVerified(db));
  String current=MigrationCodec.sha256(MigrationCodec.canonical(verified.ledger));
  assertEquals("Every canonical verified ledger field must survive the signed update",
   previous,current);
  // Future backup comparison is available from a verified in-memory ledger.
  assertTrue("Expected tested historical fixture in verified export",
   verified.ledger.has("legacy")&&verified.ledger.getJSONArray("legacy").length()==2);
 }
 private static long queryCount(LedgerDatabase db,String table){
  try(android.database.Cursor cursor=db.getOpenHelper().getReadableDatabase().query("SELECT COUNT(*) FROM "+table)){
   assertTrue(cursor.moveToFirst());return cursor.getLong(0);
  }
 }
 private static void assertRoomOne(LedgerDatabase db){
  assertEquals("Room schema must retain version 1",1,
    db.getOpenHelper().getReadableDatabase().getVersion());
 }

 @Test public void seedOnSignedV25()throws Exception{
  LedgerDatabase db=open();
  try{
   LedgerDao dao=db.dao();
   assertRoomOne(db);
   assertTrue("The test phone must have no original members",dao.members().isEmpty());
   assertTrue("The test phone must have no original payments",dao.contributions().isEmpty());
   assertTrue("The test phone must have no original expenses",dao.expenses().isEmpty());
   assertTrue("The test phone must have no original usage",dao.stockUsages().isEmpty());
   assertNull("No full ledger metadata must exist before CI seed",dao.meta());
   assertTrue("No historical legacy records must exist before CI seed",dao.legacy().isEmpty());
   assertTrue("No historical controls must exist before CI seed",dao.controls().isEmpty());

   Member m=new Member();
   m.id=MEMBER_ID;m.name="CI Synthetic Member (no real club data)";
   dao.insertMember(m);

   Contribution c=new Contribution();
   c.id=PAYMENT_ID;c.memberId=MEMBER_ID;c.feeMonth=MONTH;
   c.receivedDate="2026-08-17";c.kind="cash";c.method="CI-only cash";
   c.amount=1379;c.approximate=false;c.historical=false;
   dao.insertPayment(c);

   Expense e=new Expense();
   e.id=EXPENSE_ID;e.month=MONTH;e.date="2026-08-19";
   e.category="Shuttle pack";e.quantity=2;e.amount=2067;
   dao.insertExpense(e);

   StockUsage u=new StockUsage();
   u.id=USAGE_ID;u.month=MONTH;u.date="2026-08-21";
   u.shuttles=7;u.note="CI-only synthetic shuttle use";
   dao.insertStock(Collections.singletonList(u));

   assertEquals(1,dao.members().size());
   assertEquals(1,dao.contributions().size());
   assertEquals(1,dao.expenses().size());
   assertEquals(1,dao.stockUsages().size());

   LedgerMeta meta=new LedgerMeta();
   meta.club="CI Synthetic Badminton Club — no real data";
   meta.openingMonth="2026-01";
   meta.openingBalance=17890L;
   meta.receiptPrefix="CI";
   meta.inventoryStockJson=META_AUDIT; // Unknown original v25 JSON key must survive.
   // Simulate the original migrated source JSON's historical legacy section.
   // ExportVerified rebuilds current transactions and control totals from Room
   // but preserves historical data only when present in sourceLedgerJson.
   org.json.JSONArray legacySource=new org.json.JSONArray()
    .put(new org.json.JSONObject().put("playerId",MEMBER_ID).put("period","2025-12")
     .put("kind","cash").put("amount",489))
    .put(new org.json.JSONObject().put("playerId",MEMBER_ID).put("period","2025-12")
     .put("kind","packs").put("amount",1));
   org.json.JSONObject original=new org.json.JSONObject()
    .put("legacy",legacySource)
    .put("legacyTotals",new org.json.JSONObject())
    .put("periods",new org.json.JSONArray().put("2025-12"))
    .put("history",new org.json.JSONArray());
   meta.sourceLedgerJson=MigrationCodec.canonical(original);
   dao.insertMeta(meta);

   MonthlyControl control=new MonthlyControl();
   control.month=MONTH;control.cash=1379L;control.memberCashAtImport=1379L;
   dao.insertControls(Collections.singletonList(control));

   LegacyEntry historical=new LegacyEntry();
   historical.key=LEGACY_KEY;historical.memberId=MEMBER_ID;
   historical.period="2025-12";historical.kind="cash";historical.amount=489L;
   LegacyEntry donated=new LegacyEntry();
   donated.key=DONATED_KEY;donated.memberId=MEMBER_ID;
   donated.period="2025-12";donated.kind="packs";donated.amount=1L;
   dao.insertLegacy(Arrays.asList(historical,donated));

   AuditEvent audit=new AuditEvent();
   audit.id="ci-upgrade-audit-20261114";
   audit.action="CI original synthetic audit event";
   audit.createdAt="2026-08-21T12:00:00";
   audit.details="Owner-signed upgrade preservation fixture; not a real transaction";
   dao.audit(audit);

   assertNotNull(dao.meta());
   assertEquals(1,dao.controls().size());
   assertEquals(2,dao.legacy().size());
   assertEquals(1,queryCount(db,"audit_events"));
   preserveFingerprint(db);
   preserveVerifiedLedgerDigest(db);

   System.out.println("SYNTHETIC_V25_SEED_PASSED: member, cash, expense, usage, opening meta, controls, legacy, noncash pack and audit");
  }finally{db.close();}
 }

 @Test public void verifyAfterSignedV26Upgrade()throws Exception{
  LedgerDatabase db=open();
  try{
   LedgerDao dao=db.dao();
   assertRoomOne(db);
   assertEquals("Original member count must not change",1,dao.members().size());
   Member m=dao.members().get(0);
   assertEquals(MEMBER_ID,m.id);
   assertEquals("CI Synthetic Member (no real club data)",m.name);

   assertEquals("Cash row count after in-place signed upgrade",1,dao.contributions().size());
   Contribution cash=dao.contributions().get(0);
   assertEquals(PAYMENT_ID,cash.id);
   assertEquals(MEMBER_ID,cash.memberId);
   assertEquals(MONTH,cash.feeMonth);
   assertEquals("2026-08-17",cash.receivedDate);
   assertEquals("cash",cash.kind);
   assertEquals("CI-only cash",cash.method);
   assertEquals(1379L,cash.amount);
   assertFalse(cash.approximate);

   assertEquals(1,dao.expenses().size());
   Expense e=dao.expenses().get(0);
   assertEquals(EXPENSE_ID,e.id);
   assertEquals(MONTH,e.month);
   assertEquals("Shuttle pack",e.category);
   assertEquals(2L,e.quantity);
   assertEquals(2067L,e.amount);

   assertEquals(1,dao.stockUsages().size());
   StockUsage u=dao.stockUsages().get(0);
   assertEquals(USAGE_ID,u.id);
   assertEquals(MONTH,u.month);
   assertEquals(7L,u.shuttles);

   LedgerMeta meta=dao.meta();
   assertNotNull("Original synthetic opening ledger metadata disappeared",meta);
   assertEquals("2026-01",meta.openingMonth);
   assertEquals(17890L,meta.openingBalance);
   assertEquals("CI Synthetic Badminton Club — no real data",meta.club);
   assertEquals("CI",meta.receiptPrefix);
   assertEquals(META_AUDIT,meta.inventoryStockJson);
   assertEquals(1,dao.controls().size());
   assertEquals(MONTH,dao.controls().get(0).month);
   assertEquals(1379L,dao.controls().get(0).cash);
   assertEquals(1379L,dao.controls().get(0).memberCashAtImport);
   assertEquals(2,dao.legacy().size());
   long cashLegacy=0,noncashPacks=0;
   for(LegacyEntry old:dao.legacy()){
    if(LEGACY_KEY.equals(old.key)){
     assertEquals("cash",old.kind);assertEquals("2025-12",old.period);
     cashLegacy=old.amount;
    }
    if(DONATED_KEY.equals(old.key)){
     assertEquals("packs",old.kind);noncashPacks=old.amount;
    }
   }
   assertEquals(489L,cashLegacy);
   assertEquals(1L,noncashPacks);
   assertEquals(1L,queryCount(db,"audit_events"));
   assertFingerprintUnchanged(db);
   assertVerifiedLedgerDigestUnchanged(db);
   // Real v26 production recovery code is exercised with a complete synthetic
   // verified export. Temporary in-memory Room is discarded after comparison.
   String source=MigrationCodec.exportVerified(db);
   char[] privateTestPassword="ci-synthetic-strong-password-only".toCharArray();
   try{
    String sealed=PremiumBackupCrypto.encrypt(source,privateTestPassword);
    String recovered=PremiumBackupCrypto.decrypt(sealed,privateTestPassword);
    assertEquals("Encrypted backup must preserve verified JSON bytes",source,recovered);
    String rehearsal=BackupRecoveryV259.dryRun(
     InstrumentationRegistry.getInstrumentation().getTargetContext(),recovered);
    assertTrue("Entire synthetic migration must pass exact temporary restore",
     rehearsal.contains("RESTORE DRY-RUN PASSED"));
   }finally{
    Arrays.fill(privateTestPassword,(char)0);
   }
   System.out.println("SIGNED_V25_TO_V26_UPGRADE_PERSISTENCE_PASSED synthetic member, fee-period cash, expense, usage, opening balance, monthly control, legacy cash, noncash donated pack and audit metadata; Room v1");
  }finally{db.close();}
 }
}

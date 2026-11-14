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

 private LedgerDatabase open() {
  Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
  return Room.databaseBuilder(target,LedgerDatabase.class,DB_NAME)
    .allowMainThreadQueries().build();
 }

 private static void assertRoomOne(LedgerDatabase db){
  assertEquals("Room schema must retain version 1",1,
    db.getOpenHelper().getReadableDatabase().getVersion());
 }

 @Test public void seedOnSignedV25(){
  LedgerDatabase db=open();
  try{
   LedgerDao dao=db.dao();
   assertRoomOne(db);
   assertTrue("The test phone must have no original members",dao.members().isEmpty());
   assertTrue("The test phone must have no original payments",dao.contributions().isEmpty());
   assertTrue("The test phone must have no original expenses",dao.expenses().isEmpty());
   assertTrue("The test phone must have no original usage",dao.stockUsages().isEmpty());

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
   System.out.println("SYNTHETIC_V25_SEED_PASSED exact member/payment/expense/stock rows; no production data");
  }finally{db.close();}
 }

 @Test public void verifyAfterSignedV26Upgrade(){
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

   assertNull("Synthetic test intentionally does not create full ledger metadata",dao.meta());
   System.out.println("SIGNED_V25_TO_V26_UPGRADE_PERSISTENCE_PASSED synthetic member, fee-month cash, expense and shuttle usage; Room v1");
  }finally{db.close();}
 }
}

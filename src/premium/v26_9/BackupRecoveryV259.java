package com.vinayak.badminton.nativepreview;

import android.content.Context;
import androidx.room.Room;
import java.util.*;
import org.json.*;

/**
 * Full non-destructive restore rehearsal into an in-memory Room database.
 * Never writes, replaces, clears, or imports into the installed club database.
 */
final class BackupRecoveryV259 {
 private BackupRecoveryV259(){}
 static String dryRun(Context context,String json) throws Exception {
  if(json==null||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BackupGuardian.MAX_BACKUP_BYTES)
   throw new IllegalArgumentException("The selected JSON is unavailable or exceeds the verified backup limit");
  MigrationCodec.Preview original=MigrationCodec.inspect(json);
  LedgerDatabase temporary=Room.inMemoryDatabaseBuilder(
    context.getApplicationContext(),LedgerDatabase.class).build();
  try{
   MigrationCodec.importChecked(temporary,original);
   LedgerDao dao=temporary.dao();
   LedgerMeta meta=dao.meta();
   if(meta==null)throw new IllegalStateException("In-memory import did not initialize the ledger");
   String reenveloped=MigrationCodec.exportVerified(temporary);
   MigrationCodec.Preview restored=MigrationCodec.inspect(reenveloped);
   if(restored.players!=original.players||
     restored.payments!=original.payments||
     restored.expenses!=original.expenses||
     restored.stockUsage!=original.stockUsage||
     restored.openingBalance!=original.openingBalance||
     restored.closingBalance!=original.closingBalance||
     !restored.openingMonth.equals(original.openingMonth)||
     !restored.differences.equals(original.differences))
    throw new IllegalStateException("Restored balances or record counts differ from the verified backup");
   JSONObject before=original.ledger.optJSONObject("inventoryStock");
   JSONObject after=restored.ledger.optJSONObject("inventoryStock");
   String beforeRaw=before==null?"{}":before.toString();
   String afterRaw=after==null?"{}":after.toString();
   PremiumV26State.validate(beforeRaw,null);
   PremiumV26State.validate(afterRaw,null);
   String[] protectedKeys={
    "vinayakCashCorrectionsV1","vinayakPhysicalCountsV1","vinayakMonthlyCloseEventsV1",
    "vinayakFeePoliciesV26","vinayakFeeWaiversV26","vinayakMemberProfilesV26",
    "vinayakStockThresholdsV26"
   };
   for(String key:protectedKeys){
    Object a=before==null?null:before.opt(key);
    Object b=after==null?null:after.opt(key);
    if(a==null&&b==null)continue;
    if(a==null||b==null)throw new IllegalStateException("Restore altered metadata: "+key);
    String left=MigrationCodec.canonical(new JSONObject().put("value",a));
    String right=MigrationCodec.canonical(new JSONObject().put("value",b));
    if(!left.equals(right))
     throw new IllegalStateException("Audited history differs after in-memory restore: "+key);
   }
   List<Contribution> entries=dao.contributions();
   Map<String,CashCorrectionsV256.State> corrected=CashCorrectionsV256.states(
     meta.inventoryStockJson,entries);
   int canceled=0,adjusted=0;
   for(CashCorrectionsV256.State state:corrected.values()){
    if(state.effective==0)canceled++;
    else if(state.corrected())adjusted++;
   }
   int physical=StockReconciliationV257.read(meta.inventoryStockJson).size();
   Map<String,MonthlyCloseV258.MonthState> closings=MonthlyCloseV258.read(meta.inventoryStockJson);
   int closed=0;
   for(MonthlyCloseV258.MonthState state:closings.values())if(state.closed())closed++;
   InventoryCheckpoint.Config checkpoint=InventoryCheckpoint.read(meta.inventoryStockJson);
   return "RESTORE DRY-RUN PASSED — the installed ledger was not modified.\n"+
    "\nPlayers: "+restored.players+
    "\nCash and noncash contribution entries: "+restored.payments+
    "\nExpense entries: "+restored.expenses+
    "\nShuttle usage entries: "+restored.stockUsage+
    "\nOpening cash: INR "+restored.openingBalance+
    "\nClosing cash: INR "+restored.closingBalance+
    "\nLast recorded month: "+restored.lastMonth+
    "\nUnresolved reconciliation months: "+restored.differences.size()+
    "\nAdjusted payments: "+adjusted+
    "\nCanceled payments: "+canceled+
    "\nPhysical stock-count events: "+physical+
    "\nConfirmed opening stock: "+(checkpoint==null?"not configured":checkpoint.openingShuttles+" shuttles / "+checkpoint.perPack+" per pack")+
    "\nClosed financial months: "+closed+
    "\nAudited fee policy events: "+PremiumV26State.policies(meta.inventoryStockJson,null).size()+
    "\nAuthorized fee concessions: "+PremiumV26State.waivers(meta.inventoryStockJson,null).size()+
    "\nMember profiles retained: "+PremiumV26State.profiles(meta.inventoryStockJson,null).size()+
    "\n\nA separate in-memory Room database was created, verified, and discarded. Nothing was imported into the real app.";
  }finally{
   temporary.close();
  }
 }
}

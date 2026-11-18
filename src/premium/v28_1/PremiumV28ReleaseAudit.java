package com.vinayak.badminton.nativepreview;

import android.content.Context;
import java.util.*;

/**
 * Read-only in-app acceptance / backup readiness snapshot.
 * A local self-rehearsal is not an independently verified external backup and
 * cannot approve an actual-phone upgrade. Does not mutate any ledger table.
 */
final class PremiumV28ReleaseAudit {
 private PremiumV28ReleaseAudit(){}

 static String review(Context ctx,LedgerDatabase database)throws Exception {
  LedgerDao dao=database.dao();
  LedgerMeta meta=dao.meta();
  if(meta==null)throw new IllegalStateException("Import verified financial history first.");
  String full=MigrationCodec.exportVerified(database);
  MigrationCodec.Preview current=MigrationCodec.inspect(full);
  // Existing in-memory test must independently re-import and check canonical
  // original fields, then discard the temporary database without a merge.
  String rehearsal=BackupRecoveryV259.dryRun(ctx,full);
  if(!rehearsal.startsWith("RESTORE DRY-RUN PASSED"))
   throw new IllegalStateException("The in-memory restore rehearsal did not pass");
  int photos=PremiumV28MemberPhotos.count(ctx);
  String external=BackupGuardian.status(ctx);
  String readiness=BackupGuardian.recoveryReadiness(ctx);
  StringBuilder result=new StringBuilder();
  result.append("READ-ONLY LIVE LEDGER AUDIT\n")
   .append("Member records: ").append(current.players).append('\n')
   .append("Original + later cash/noncash entries: ").append(current.payments).append('\n')
   .append("Expense records: ").append(current.expenses).append('\n')
   .append("Shuttle usage records: ").append(current.stockUsage).append('\n')
   .append("Opening balance: INR ").append(current.openingBalance).append('\n')
   .append("Closing balance: INR ").append(current.closingBalance).append('\n')
   .append("Latest reported period: ").append(current.lastMonth).append('\n')
   .append("Unresolved historical difference records: ")
   .append(current.differences.size()).append('\n')
   .append("\nLOCAL RESTORE REHEARSAL\n")
   .append("PASS — reconstructed a temporary in-memory database; discarded it.\n")
   .append("This does not prove a physical phone backup exists.\n\n")
   .append("BACKUP STATUS\n").append(external).append("\n\n")
   .append("RECOVERY READINESS\n").append(readiness).append("\n\n")
   .append("LOCAL MEMBER PHOTOS\n")
   .append(photos).append(" photo(s) in the app-private directory.\n")
   .append("These are intentionally separate from JSON and encrypted FINANCIAL backups.")
   .append(" They stay on the same phone for in-place upgrades, but are not restored")
   .append(" on a different device by a ledger JSON import.\n\n")
   .append("NOT A PRODUCTION APPROVAL\n")
   .append("On the owner's real phone: verify an independent external copy,")
   .append(" compare every original member ID and record, preserve the noncash")
   .append(" donated pack and any unresolved historical cash difference,")
   .append(" then rehearse empty-device recovery and Android printing/sharing.")
   .append(" No financial data was altered by this review.");
  return result.toString();
 }
}

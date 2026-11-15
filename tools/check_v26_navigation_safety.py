#!/usr/bin/env python3
"""Release-blocking source checks for asynchronous native navigation (no real data)."""
from pathlib import Path
import re

source = Path("src/premium/v26_9/MainActivity.java").read_text(encoding="utf-8")
required = [
    "screenEpoch++;tab=name;",
    'currentScreen(screen,"Home",period)',
    'currentScreen(screen,"Collect",viewedMonth)',
    'currentScreen(screen,"Shuttles",selected)',
    'currentScreen(screen,"Members",viewedMonth)',
    'selectTab("Home");',
    'selectTab("Collect");',
    'selectTab("Shuttles");',
    "memberRenderGeneration",
]
for item in required:
    assert item in source, f"Native-screen stale-result guard missing: {item}"

# Regression protection for the signed v25.9.1 Players/Receipts shortcut hotfix.
# The v26 candidate MUST retain these callbacks; a plain metric(...) is not tappable.
for item in [
    'dashboardShortcutMetric("Players"',
    'dashboardShortcutMetric("Receipts"',
    '"View all players",()->selectTab("Members")',
    'this::openHomeMonthlyReceipts',
    'focusMonthlyReceipts=true',
    'card.setClickable(true)',
    'card.setFocusable(true)',
    'pressable(card,action)',
    'pageScroll.scrollTo(0,0)',
    'pageScroll.smoothScrollTo(',
    'currentScreen(screen,"Collect",viewedMonth)&&monthlyReceiptAnchor.getParent()==content',
]:
    assert item in source, f"v25.9.1 Home shortcut regression: {item}"
assert source.count('LinearLayout a=metric("Players"') == 0, "Players summary must be a shortcut"
assert source.count('LinearLayout b=metric("Receipts"') == 0, "Receipts summary must be a shortcut"

calls = []
for number, line in enumerate(source.splitlines(), 1):
    if "private void selectTab(" in line:
        continue
    if "private void showHome(" in line or "private void showPayments(" in line or "private void showExpenses(" in line:
        continue
    for name in ["showHome", "showPayments", "showExpenses"]:
        if re.search(r"\b" + name + r"\s*\(\s*\)\s*;", line):
            calls.append(f"line {number}: {name}() invoked directly outside navigation")
if calls:
    raise AssertionError("Post-save navigation bypasses the selected tab epoch: " + "; ".join(calls))

# Background rendering must check that the selected screen/month still matches.
for marker, count in [
    ('currentScreen(screen,"Home",period)', 3),
    ('currentScreen(screen,"Collect",viewedMonth)', 1),
    ('currentScreen(screen,"Shuttles",selected)', 1),
    ('currentScreen(screen,"Members",viewedMonth)', 2),
]:
    observed = source.count(marker)
    assert observed >= count, f"Missing expected {marker}: {observed} < {count}"

# Guard remains a client-side rendering check; financial writes still use Room transactions.
assert source.count("database.runInTransaction(") >= 5
assert "requireOpenMonth(original.feeMonth)" in source
assert "PremiumV26StockGuard.requireEditableStockMonth(" in source
# Financial mutation safety: an earlier open month cannot alter a later closed snapshot.
assert "PremiumV26ClosingGuard.requireWritable(meta.inventoryStockJson" in source
assert "PremiumV26PaymentOnce.alreadyCommitted(dao.contributions()" in source
assert 'final String submissionId="pay-"+UUID.randomUUID();' in source
assert "reviewing.compareAndSet(false,true)" in source
assert 'if(!created[0])' in source
assert 'requireOpenMonth(fee)' in source

# Receipt batch exports must not retain hundreds of abandoned cached PDF files.
assert "uncompressedBytes>35_000_000L" in source
assert "if(pdf!=null)pdf.delete();" in source
assert "zip.delete(); // Never leave a broken ZIP" in source

recovery = Path("src/premium/v26_9/BackupRecoveryV259.java").read_text(encoding="utf-8")
assert "MigrationCodec.canonical(allBefore).equals(MigrationCodec.canonical(allAfter))" in recovery
assert "PremiumV26BackupCompare.compare(current.ledger,result.ledger).summary()" in source
comparison = Path("src/premium/v26_9/PremiumV26BackupCompare.java").read_text(encoding="utf-8")
assert "TreeSet<String> names" in comparison
assert "MigrationCodec.canonical(current)" in comparison
assert "MigrationCodec.canonical(backup)" in comparison
assert "No records were changed." in comparison

patch=Path("src/premium/v26_9/patch_v26.py").read_text(encoding="utf-8")
assert 'sourceAuditPresent=state.has("nativeAuditHistory")' in patch
assert 'if(e.id.startsWith("import-"))continue;' in patch
assert 'if(sourceAuditPresent||audit.length()>0)state.put("nativeAuditHistory",audit)' in patch
assert 'if s.count(audit_filter)!=1:' in patch
tests=Path("tools/v26-upgrade-test/V26SignedUpgradePersistenceTest.java").read_text(encoding="utf-8")
assert "preserveVerifiedLedgerDigest(db);" in tests
assert "assertVerifiedLedgerDigestUnchanged(db);" in tests
assert "assertFingerprintUnchanged(db);" in tests
assert "BackupRecoveryV259.dryRun(" in tests
assert "SYNTHETIC_ROSTER_SIZE=15" in tests
assert "Original synthetic 15-player count" in tests
assert "rejectedTampering" in tests
navigation=Path("tools/v26-upgrade-test/check_populated_navigation.py").read_text(encoding="utf-8")
assert 'last_member="CI Synthetic Player 15"' in navigation
assert 'tap_home_card("Players"' in navigation
assert 'tap_home_card("Receipts"' in navigation
print("v26 navigation / 15-member signed upgrade / encrypted tamper / Home tap / chronological accounting safety: PASS")

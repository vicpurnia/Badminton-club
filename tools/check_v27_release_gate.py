#!/usr/bin/env python3
"""v27.0 fail-closed, non-mutating release invariants.

This script checks tracked source before assembling a debug candidate.
It is not a substitute for signed in-place upgrade/physical-device tests.
"""
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
def read(path):
    p=ROOT/path
    if not p.is_file():
        raise AssertionError(f"Required historical source missing: {path}")
    return p.read_text(encoding="utf-8")

def contains(path,*terms):
    content=read(path)
    for term in terms:
        if term not in content:
            raise AssertionError(f"{path}: missing required compatibility anchor {term!r}")

def main():
    classic="src/premium/v23_1/ClassicLedgerV231.java"
    contains(classic,"new PdfDocument.PageInfo.Builder(842,595,1)","purchaseDetails","2026-02","pdf.finishPage(page)")
    contains("src/premium/v26_9/MainActivity.java",
             "v26.9 DEVELOPMENT CANDIDATE","requireOpenMonth(original.feeMonth)",
             "database.runInTransaction","PremiumV26PaymentOnce",
             "PremiumV26ClosingGuard","BackupGuardian.save",
             "TEST RESTORE WITHOUT CHANGING DATA",
             "ClassicLedgerV231.master")
    contains("src/premium/v26_9/PremiumV26PaymentOnce.java",
             "alreadyCommitted","Duplicate original payment ID")
    contains("src/premium/v26_9/PremiumV26ClosingGuard.java",
             "rolls forward into CLOSED","No ledger changes were committed.")
    contains("src/premium/v26_9/PremiumV26BackupCompare.java",
             "class PremiumV26BackupCompare")
    contains("src/premium/v26_9/PremiumBackupCrypto.java",
             "PBKDF2WithHmacSHA256","AES/GCM/NoPadding")
    contains("src/premium/v26_9/PremiumV26StockGuard.java",
             "requireEditableStockMonth","requireEditableOpeningCheckpoint")
    contains("src/premium/v26_9/PremiumV26State.java",
             "vinayakFeePoliciesV26","vinayakFeeWaiversV26",
             "vinayakMemberProfilesV26","vinayakStockThresholdsV26")
    contains("V26_0_TO_26_9_DEVELOPMENT_AND_ACCEPTANCE.md",
             "unresolved February 2026","₹1,000",
             "com.vinayak.badminton.premiumv24","version=1")
    contains("src/premium/v25_9/BackupGuardian.java",
             "verifySavedCopy","MAX_BACKUP_BYTES")
    contains("src/premium/v25_9/BackupRecoveryV259.java",
             "Room.inMemoryDatabaseBuilder","temporary.close()")
    workflow=read(".github/workflows/build-v27-candidate.yml")
    assert "build-v26-candidate.yml" not in workflow.split("paths:",1)[-1].split("permissions:",1)[0]
    for term in ("src/premium/v26_9/**","tools/check_v27_release_gate.py",
                 "27.1-dev.","28000+int(os.environ"):
        if term not in workflow:
            raise AssertionError("v27 CI missing "+term)
    assert "android:label=" in workflow
    assert "com.vinayak.badminton.premiumv24" in read("V27_0_TO_27_9_DEVELOPMENT.md")
    # Source is the proven v26 code until new functionality passes separate tests.
    # These gates deliberately don't claim the v27.1..9 features exist.
    print("PASS: v27.0 source identity, history, cash, stock, backups and CI wiring")
if __name__=="__main__": main()

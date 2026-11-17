#!/usr/bin/env python3
"""v27.9 development-only integrated build safety gates.

This does NOT certify real device, owner's signature, historical migration,
complete user-facing features or acceptance as stable.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=(root/"MainActivity.java").read_text()
anchors={
 "PremiumV27MemberNames.java": ["Normalizer.Form.NFKC","requireUnique"],
 "PremiumV27FeeAudit.java": ["unassigned","Math.subtractExact"],
 "PremiumV27ReceiptArchive.java": ["ZipInputStream","promote","MAX_ARCHIVE_BYTES"],
 "PremiumV27ExpenseVoucher.java": ["SELF-GENERATED RECORD","Original expenditure ledger"],
 "PremiumV27StockVariance.java": ["SHORTFALL:","EXCESS:","MATCH:"],
 "PremiumV27CloseReview.java": ["SHA-256","requireUnchanged"],
 "PremiumV27TrendAudit.java": ["requireOrdered","out of order"],
 "PremiumV27BackupStaging.java": ["SHA-256","MessageDigest.isEqual"],
}
for name,terms in anchors.items():
 file=root/name
 if not file.is_file():raise AssertionError("Missing v27 module "+name)
 contents=file.read_text()
 for term in terms:
  if term not in contents:raise AssertionError("v27 module "+name+" lacks "+term)
for hook in ("PremiumV27MemberNames.key","PremiumV27FeeAudit.review",
 "PremiumV27ReceiptArchive.promote","PremiumV27ExpenseVoucher.make",
 "PremiumV27StockVariance.describe","PremiumV27CloseReview.requireUnchanged",
 "PremiumV27TrendAudit.requireOrdered","PremiumV27BackupStaging.verify"):
 if hook not in activity:raise AssertionError("Missing app integration: "+hook)
gradle=Path("app/build.gradle").read_text()
if "premiumv24" not in gradle or "27.9-dev." not in gradle:raise AssertionError("APK identity/version incompatible")
if "version=1" not in (root/"LedgerDatabase.java").read_text():
 raise AssertionError("Historical Room schema changed")
if "new PdfDocument.PageInfo.Builder(842,595,1)" not in (root/"ClassicLedgerV231.java").read_text():
 raise AssertionError("Original Classic landscape report changed")
for original in ("src/premium/v26_9/MainActivity.java","src/premium/v26_9/PremiumV26State.java"):
 if not Path(original).is_file():raise AssertionError("v26 historical fallback missing")
if "v27.9 DEVELOPMENT CANDIDATE" not in activity:
 raise AssertionError("Development-mode release label missing")
print("PASS v27.9 integrated additive hooks, report baseline, original Room/package and development labeling")

#!/usr/bin/env python3
"""v28 development-only, fail-closed additive photo and audit source safety.

This is NOT physical owner-phone/data-transfer acceptance.
"""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
activity=(root/"MainActivity.java").read_text(encoding="utf-8")
photo=(root/"PremiumV28MemberPhotos.java").read_text(encoding="utf-8")
audit=(root/"PremiumV28ReleaseAudit.java").read_text(encoding="utf-8")
for term in ("PICK_MEMBER_PHOTO_V28", "ACTION_OPEN_DOCUMENT","pickMemberPhoto(m)",
             "deleteMemberPhoto(m)","memberAvatar(item.member,item.name)",
             "memberAvatar(m,displayName)", "PremiumV28MemberPhotos.addOrReplace",
             "v28 BACKUP & RELEASE READINESS", "PremiumV28ReleaseAudit.review",
             "v28.1 DEVELOPMENT CANDIDATE"):
 if term not in activity:raise AssertionError("Missing required v28 UI hook "+term)
for term in ("member-photos","getFilesDir()", "SHA-256", "BitmapFactory",
             "getContentResolver().openInputStream", "ATOMIC_MOVE", "REPLACE_EXISTING",
             "center", "ExifInterface"):
 if term=="center" and "center-crop" in photo:continue
 if term not in photo:raise AssertionError("Missing local portrait-safety clause "+term)
for term in ("BackupRecoveryV259.dryRun","MigrationCodec.inspect",
             "BackupGuardian.recoveryReadiness","PremiumV28MemberPhotos.count",
             "not restored", "No financial data was altered"):
 if term not in audit:raise AssertionError("Missing release-readiness clause "+term)
if 'version=1' not in (root/"LedgerDatabase.java").read_text():
 raise AssertionError("Historical Room schema changed")
if 'new PdfDocument.PageInfo.Builder(842,595,1)' not in (root/"ClassicLedgerV231.java").read_text():
 raise AssertionError("Original Classic one-page A4 landscape format changed")
gradle=Path("app/build.gradle").read_text()
if "premiumv24" not in gradle or "28.1-dev." not in gradle:
 raise AssertionError("Developer package identity/version was changed")
for name in ("src/premium/v26_9/MainActivity.java","src/premium/v25_9/BackupGuardian.java",
             "src/premium/v27_9_9/PremiumV2799PdfShare.java"):
 if not Path(name).is_file():raise AssertionError("Required verified v27 baseline absent "+name)
print("PASS v28 additive member-photo and backup audit wiring, original Room v1/Classic PDF intact")

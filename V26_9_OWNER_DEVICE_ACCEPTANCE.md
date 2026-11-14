# Badminton Premium v26.9 — Owner Device Acceptance (signed TEST APK only)

**Purpose:** Permit a controlled test of the v26.9 candidate on the existing Badminton Premium package ID without mistaking a successful build for a completed release. This document contains no actual member identifiers beyond the previously documented historical acceptance landmarks.

**Build source:** `dev/v26-0-to-26-9-20261114` (draft PR).  
**Android application ID:** `com.vinayak.badminton.premiumv24` — unchanged from v25.9.  
**Signing certificate:** the same permanent owner-controlled certificate, verified against `BADMINTON_EXPECTED_CERT_SHA256`.  
**Candidate status:** owner-signed TEST build, NOT stable. It must never automatically replace the user's installed APK.

## Important safety rules

1. Never uninstall Badminton Premium, choose Clear storage/data or import into a database that already contains a ledger.
2. From the currently installed v25.9, make a fresh **verified external JSON backup** before accepting any test APK. Confirm both verification and a successful in-memory restore rehearsal.
3. Retain a second, untouched copy outside app-private storage, plus the previously verified baseline backup. Do not publish a member ledger, PIN, password or `.p12` signing key to GitHub.
4. **Only** the `Badminton-Premium-v26.9-SIGNED-TEST-CANDIDATE_NOT-PRODUCTION` artifact is eligible for owner-controlled same-package upgrade testing. The `DEVELOPMENT-DEBUG` artifact is not signed by the permanent release key and must never be offered as an in-place upgrade over the installed stable app.
5. A TEST APK is not the final stable release. If a gate fails, stop and retain the original verified backups. Do not attempt uninstall/reinstall as a workaround.

## A. Read-only pre-install snapshot — existing v25.9

- [ ] Confirm current app is the previously accepted permanently signed v25.9.
- [ ] Export verified external JSON (saved **outside** app-private storage) and reopen/verify it.
- [ ] Complete `TEST RESTORE WITHOUT CHANGING DATA` on the backup.
- [ ] Record the current count of original members (**documented baseline: 15**), latest genuine balance and last closed month.
- [ ] Check February 2026's noncash donated pack from Kanishk remains **+1 pack**, not INR cash; preserve historical INR 8,500 +1 pack documentation.
- [ ] Record the unresolved February 2026 **INR 1,000** reconciliation difference explicitly, without "correcting" it.
- [ ] Check any receipt correction, inventory physical-count and monthly closing histories before upgrade.
- [ ] Keep a screenshot or paper ledger summary privately for before/after comparison.

## B. Signed TEST artifact verification

- [ ] Open the owner's GitHub Actions **Build Badminton Premium v26 SIGNED TEST Candidate (NOT STABLE)** workflow for the exact candidate SHA.
- [ ] Confirm its automated checks: previous accounting regressions, v26 synthetic suite, Android build, APK package ID + version code, expected permanent signing certificate SHA-256 all **PASS**.
- [ ] Verify the artifact name includes `SIGNED-TEST-CANDIDATE_NOT-PRODUCTION`.
- [ ] Confirm the package ID is still `com.vinayak.badminton.premiumv24`, and candidate `versionCode` exceeds v25.9.
- [ ] Do not install a debug artifact or a downloaded unsigned preview as an update to the signed app.

## C. Controlled device acceptance

Prefer a spare Android phone with a verified restored **copy** of the ledger for destructive workflow tests. A replica must never be merged into or used to replace the production ledger.

- [ ] Test the signed APK's in-place installation over a signed v25.9 installation **without uninstall/clear-data**.
- [ ] App opens normally; no indefinite loading, crashes, frozen controls or lost ledger access.
- [ ] The documented original 15 members, historic receipts, noncash donation, unresolved difference and latest closing balance are unchanged absent a deliberate test transaction.
- [ ] Compare month-end and original Classic one-page A4 landscape PDF before/after.
- [ ] New fees and concession controls preserve all original cash amounts and do not charge a newly joined member before the joining month.
- [ ] Status labels correctly distinguish paid cash, partially paid, unpaid, fully waived/settled, unconfigured and not due.
- [ ] Date-specific cash evidence stays independent of contribution **fee month**; unknown or approximate imported days remain undated.
- [ ] Test cash correction, partial refund, cancellation and audited restoration **in a test replica**; verify audit event, closing-balance impact, void/reissued PDFs and duplicate-action guards.
- [ ] Month-end closing rejects payment changes, backdated fee rules and stock changes until an authorized audit reopening.
- [ ] Physical stock count blocks backdated purchase/donation/usage across a previously confirmed physical count; no fabricated quantity is inserted.
- [ ] Rename/archive/reactivate a test member; original receipt IDs and original member names remain historically intact.
- [ ] Full-screen PDF preview, share via WhatsApp, save to Downloads and member ZIP work on the phone.
- [ ] A4 independent dashboard PDF and CSV reconcile with the original ledgers; verify no difference was silently balanced away.

## D. Backup / new phone migration

- [ ] Export an optional password-protected encrypted backup; store its password separately.
- [ ] Read the saved file back and confirm byte verification passed.
- [ ] Wrong password and tampered ciphertext reject without touching the installed ledger.
- [ ] Test in-memory restore; counts, original balances, correction events, stock counts, fees, member profiles and close events match exactly.
- [ ] On a **separate empty** Android installation, restore the verified backup. Import over existing accounts must be refused.
- [ ] Ensure private storage providers, SAF folder permissions and multi-GB device storage constraints do not cause silent loss.
- [ ] Close/reopen app and verify durable persistence again.

## E. Final release gate

- [ ] 100% critical accounting, stock, migration, PDF sharing and recovery workflows accepted on real Android.
- [ ] No known critical crash, stale balance, data loss, unexplained INR amount difference or silent inventory correction.
- [ ] Exact same permanent signing identity and application package verified.
- [ ] Only after these checks, label a **separately built and accepted** production v26.9 release. Keep the PR draft if any test is incomplete.

**Never declare stable merely because GitHub Actions says the APK compiled and passed synthetic tests.**


## Additional v26.9 hardening — closing roll-forward and duplicate payment

The following checks have been implemented in the draft source and synthetic regression suite, but still require physical-device acceptance:

- **Forward protected closing:** If March is CLOSED, a cash adjustment, expense, donation or shuttle usage entered for February is blocked even when February itself appears OPEN. The later month must be reopened with a recorded audit before any earlier month can modify its opening/closing balance. Verify on a test replica, not by creating fictitious transactions on the real club ledger.
- **Payment form replay:** Saving a cash payment uses a stable ID for that form. Repeat the same confirmation callback and verify there is one original cash entry, one audit event and no extra collection. A genuinely separate payment form receives a fresh ID and remains permitted.
- **Receipt ZIP storage:** Export a member's historical recorded receipt bundle on a low-storage test device; failed/oversize ZIP exports must not be shared as complete, and temporary individual generated PDFs must be deleted.
- **All inventory metadata:** In-memory restore now checks canonical equality of the *entire* inventory/audit metadata object, including opening physical checkpoint and any future extension keys, instead of relying only on a fixed list.

These improvements are **not** evidence of installed-phone data continuity. The production release gate remains open.

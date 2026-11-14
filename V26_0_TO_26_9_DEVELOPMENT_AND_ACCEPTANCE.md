# Badminton Premium — v26.0 through v26.9 development and acceptance record

**Branch:** `dev/v26-0-to-26-9-20261114`  
**Baseline:** signed v25.9 source on `main`  
**Release status:** SOURCE DEVELOPMENT CANDIDATE — NOT A SIGNED PRODUCTION RELEASE  
**Package:** unchanged `com.vinayak.badminton.premiumv24`; **Room schema:** unchanged `version=1`

This document maps the ten requested upgrades to code and release gates. It does not certify on-device acceptance or imply that ten separate signed APKs have been published.

| Upgrade | Implemented development work | Limitations / mandatory acceptance |
| --- | --- | --- |
| **v26.0 Release safety** | Separate PR development branch, fail-closed integration patch, original v25.9 reconstructed build, added synthetic core regression suite, debug Android GitHub Actions | Real device in-place preservation, external picker and originals still require acceptance |
| **v26.1 Corrections 2.0** | Original cash immutable; audited correction/refund/cancel chain reused, v26 audited `RESTORE` of a canceled original, projected closing balance in confirmation | Test duplicate rapid actions and refunds against a real known payment; no fabricated corrections to production history |
| **v26.2 Dues / arrears** | Effective-date global and member-specific fee rules, validated concessions, assigned-month dues, partial payments and future-month contributions, separate cash and obligations | Month not explicitly configured shows *Unconfigured*, not assumed ₹2,000; advance reallocation across fee months is not automated |
| **v26.3 Receipt centre** | Reissued, corrected and void receipts still based on original IDs; in-app full-screen first-page PDF viewer, zoom, existing share/save, member receipt ZIP | Android file-provider, PDF viewer memory use, PDF pages and WhatsApp chooser require physical-device check; batch ZIP includes recorded positive original payments only |
| **v26.4 Monthly closing** | Existing audited close/reopen and separate A4 monthly review preserved; v26 fee completeness preview and closed-period drift warning | Reopening must remain compulsory for cash edits, stock/usage and backdated fee policy; simulate interrupted save and subsequent-month changes |
| **v26.5 Inventory** | Existing mixed pack factors and physical stock checkpoints preserved; usage classification (consumed/damaged/lost), audited reorder threshold and warning from calculated/confirmed quantities | Reorder threshold is a *warning*, not a new purchase, fabricated consumption or hidden stock correction; verify count and purchase-cost reports on device |
| **v26.6 Members** | Audited profile overlay (display name, joining date, contact, notes, active/archived), duplicate-name rejection, archived members removed from new-payment selection | Historical `Member.id` and original cash/receipt member name are untouched; test reactivation, backups and unusual name collisions |
| **v26.7 Dashboard** | Independent monthly finance and shuttle trend bars, A4 landscape analytics PDF and CSV; unchanged Classic Master PDF | Full dashboard needs visual review; ensure displayed months and closed reconciliation totals mathematically agree |
| **v26.8 Encrypted recovery** | Optional authenticated AES-256-GCM + PBKDF2 export, 12+ character password, saved-file byte re-read, wrong-password/tamper rejection, read-only in-memory rehearsal, import only to empty app | Password unrecoverable if lost; physically test storage providers and moving an encrypted backup between devices; no cloud sync or blind merge |
| **v26.9 UX and RC** | Shared tap debounce, premium/consistent buttons, explicit statuses, native full-screen PDF preview, candidate labels, regression-gated debug build | Final visual polish, Android accessibility, talkback, large-font/device QA, permanent key signature + upgrade acceptance still pending |

## Release hardening completed after initial v26.9 candidate

- **Concession-aware status:** Member filters derive status from the *net fee due* after authorized waivers. No-pay zero-fee months say *Settled*, never *Paid*.
- **Stock anchor lock:** A confirmed physical count protects every stock-affecting month up to that count. New backdated purchases, donated packs or used/lost/damaged shuttles are blocked, as are opening-stock edits that would invalidate the count.
- **Immutable membership billing origin:** Newly added members begin accruing fees from their creation month, not from earlier global fee rules. Later joining-date/profile edits cannot silently erase earlier assessments.
- **Fee policy/waiver consistency:** Reject fee-rate changes that would invalidate an earlier authorized concession; reject cumulative waivers above the effective fee rather than silently capping them.
- **Month-end review:** Separately shows members with an assessed fee, members not due (archived or pre-joining), and those without a configured fee.
- **Members UX:** Reconciled status calculations run on a background executor with stale-result suppression; long filters scroll rather than compress into unusable narrow chips.
- **Signed test candidate workflow:** `.github/workflows/build-v26-signed-test-candidate.yml` is separate from v25.9 stable signing. It runs Java/Android tests, signs with the owner's existing GitHub-secret keystore, verifies the certificate SHA-256, and uploads a **TEST ONLY** APK. It does not publish, merge or install an app.

The signed TEST APK is only an acceptance vehicle. A CI-green/signed APK is **not** proof of data-preserving in-place Android installation; that still requires a verified external backup and actual-device inspection before production approval.

## Source-of-truth guarantees

1. **Main stays untouched** until development PR review and production acceptance. Original signing private key is never committed or transmitted in source or chat.
2. Preserve `com.vinayak.badminton.premiumv24` and the original signing certificate. The candidate CI produces only a debug APK; it **must not be installed over the owner's signed stable app**.
3. The documented original 15-member history is not recreated or rewritten. The recorded Kanishk February 2026 donated shuttle pack stays **noncash**; its historical ₹8,500 +1 pack total stays separate from receipts.
4. Preserve the unresolved February 2026 **₹1,000** cash reconciliation difference as an exception, not an adjustment or guessed payment.
5. Preserve immutable payments, receipt dates (if unknown, display unknown), corrections, audited stock counts and fee policy history.
6. Keep the A4 **one-page landscape Classic Master Ledger** generated by existing `ClassicLedgerV231` unchanged; independent new PDF exports must not alter it.
7. All money is integer INR, not floating-point. Dues are *accrual obligations* and cannot be used to manufacture received cash.
8. Closing/reopening status, physical stock anchors and corrections must be read from validated verified metadata and survive export/import. Room schema version stays 1; no destructive migration.
9. Never uninstall the signed app, clear data or replace the live database merely to test a new release.
10. **Never label a debug build as production** or issue a final v26.9 signed stable APK solely because compilation succeeds.

## Automated CI gates

- Reconstruct prior source from the existing checksum-verified archive; overlay existing v25.9 and additive v26 classes.
- Run previous Java accounting and shuttle quantity regression checks, and verify original migration/inventory/month-close hooks.
- Compile and execute the **synthetic** v26 tests: member fees, policy precedence, waiver, arrears, correction chain (refund/cancel/audited restoration), original immutable cash, archival, reorder threshold, AES-GCM roundtrip, password rejection and tamper rejection.
- Validate source anchors for Room v1, original Classic A4, and verification before decrypted import.
- Compile `assembleDebug` and upload an explicitly development-only artifact; no private signing secrets in PR CI.

## Device acceptance — real Android (mandatory before release)

Use a **verified external JSON backup and the owner's signed v25.9 installation**. Keep the previous backup until all acceptance checks pass.

1. Compare original member count, recent historical cash, closing balance, Feb 2026 noncash donation and unresolved ₹1,000 difference **before and after same-package signed upgrade**.
2. Correct a real known existing payment once, review effect, share/preview/save corrected PDF, attempt duplicate action; reverse with new audited event, verify downstream month balances; do not create fake financial events.
3. Configure effective-dated fees; test partial payment, unpaid fee, future-month advance, member-specific concession and closed-month restrictions.
4. Preview original/reissued/void PDFs, WhatsApp shares and member ZIP from multiple Android versions; verify no invented original receipt date.
5. Close completed month, reject edits while closed; reopen with audit reason; verify audit persistence after restart and dry-run backup restore.
6. Reconcile purchased/donated mixed shuttle packs, damaged/lost usage, counted physical stock and reorder warning. Preserve real discrepancies.
7. Rename/archive/reactivate a synthetic testing member, verify all linked receipt IDs intact; duplicate profile should reject.
8. Compare every dashboard total and CSV row against existing month-end summaries; inspect independent A4 PDF and the **unchanged** Classic Master.
9. Encrypt a verified backup, move to second phone, reject wrong password and tampered file, dry-run and import into an empty app; reject restore over existing records.
10. Test rapid double taps, rotation, font scaling, storage permission denial, device low space, safe exit/reopen and recovery from interrupted write.

**Release gate:** no known critical cash, stock, data-loss or signature defects; 100% of critical real-device workflows performed. If any gate fails, leave PR draft and keep signed v25.9 as the stable release.


## v26.9 follow-on defect corrections — development only

- Closed-month roll-forward protection: transactions entered for an earlier month cannot silently alter a later protected CLOSE snapshot.
- Payment idempotency: one immutable payment form ID, repeat-confirmation guard and transaction-level replay detection before inserting cash or the audit entry.
- Member receipt ZIP export: bounded total size, temporary PDF cleanup and deletion of incomplete archives.
- Restore metadata equality: compare the full verified inventory/audit JSON in temporary memory, preserving future metadata extensions beyond the known event keys.
- Run synthetic Java tests for closing chronology, duplicate payment submissions and conflicting IDs; source checks must retain the wiring in all CI pipelines.

Production acceptance and original private-key signed upgrade testing remain separate from automated compilation.

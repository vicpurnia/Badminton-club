# Badminton Premium APK — v27.0–v27.9 development ledger

**Status:** DEVELOPMENT / TEST ONLY — not approved as a production APK.  
**Source:** v26.9 dev commit `8c90ee11246330f37d31b8d40a458c92f265fec1`.  
**Production:** `main` remains the signed stable v25.9.1 source.  
**Identity:** `com.vinayak.badminton.premiumv24`; retain original certificate and Room `version=1`. Never uninstall or clear existing data.

## Actual implementation progress

| Version | Objective | Evidence / status |
|---|---|---|
| 27.0 | Isolated source release-safety branch; automated identity/history checks; build-time candidate version & APK debug CI; re-run previous Java regression and Android compile | **Code added; GitHub CI status must be checked. Not an installation sign-off.** |
| 27.1 | Unicode-normalized duplicate-member protection for new names and audited profile renames, without changing original IDs or stored names | **Implemented as additive Android build overlay; synthetic Java test + debug APK workflow passed (run 43777067582). Real-phone acceptance pending.** |
| 27.2 | Split read-only assigned versus unassessed/non-billable cash; display concessions, arrears and original-period surplus independently; no automated advance reassignment | **Implemented; debug Android and synthetic test CI PASS, run 43777620179.** |
| 27.3 | Stage member receipt ZIP, stream-verify all entries, enforce safe names/sizes/count and promote before share; existing PDF engine unchanged | **Implemented; Android/debug and ZIP integrity CI PASS, run 43777786150.** |
| 27.4 | Internal PDF expense voucher offered after successful shuttle purchase, explicitly not a vendor invoice and never a second expense | **Implemented; Android CI pending. External bill scan/attachment not implemented.** |
| 27.5 | Shuttle inventory traceability and physical count dispute handling | Pending source implementation |
| 27.6 | Financial month closing and immutable audit verification | Pending source implementation |
| 27.7 | Premium responsive analytics and independent exports | Pending source implementation |
| 27.8 | One-tap external verified backups and cross-phone recovery rehearsal | Pending source implementation |
| 27.9 | Visual accessibility and performance finalization, complete regression and physical owner-phone acceptance | Pending source implementation |

Do not treat version goals as finished implementations or invent signed APK artifacts. Never silently reassign fee payments to a different month, re-create a cash payment, synthesize missing receipt dates, or retroactively change a closed accounting period.

## Protected historical facts and scope

- Original 15 members, IDs, cash events, historic receipts and cash-versus-obligation separation remain unchanged.
- Kanishk's February 2026 donated shuttle pack is a **noncash** stock entry, never fee revenue.
- The unresolved February 2026 ₹1,000 discrepancy is retained as a flagged reconciliation variance, never silently zeroed.
- Preserve the original one-page A4 landscape Classic Master Ledger exactly. Any new reports are separately named.
- Preserve v26 guards for double-submission, stock physical anchor, chronological CLOSED month, AES-GCM backup authenticity, identity and dry-run restore.
- Signed stable release requires the owner's existing private signing key, real installed v25.9.1 backup proof, signed in-place upgrade and real Android UI/storage/share/PDF review. A successful emulator/CI compilation cannot replace these gates.

## v27.0–27.1 testing

`.github/workflows/build-v27-candidate.yml` reconstructs the archived Android project, reuses v26 overlay and regression tests, verifies the safety anchors, compiles a separate **debug-only** `27.0-dev` APK artifact, and does not publish or merge production.

`tools/check_v27_release_gate.py` fails on absent protected source contracts. The v26.9 source is preserved unchanged on this branch as a known-working fallback; build-time candidate labeling alone is not a v27.1+ feature.

**Never install the debug APK over the signed production installation.**

## Verified v27.1 CI record

- [GitHub Actions v27.1 successful debug build](https://github.com/vicpurnia/Badminton-club/actions/runs/43777067582): prior v26 accounting/stock/AES synthetic tests, source-identity gates, Unicode duplicate identity test, Android debug compilation and artifact upload passed.
- Development artifact: `Badminton-Premium-v27.1-MEMBER-SAFETY-DEBUG`. Do not install over the owner's signed stable app.
- v27.2–v27.9 remain **planned**, not completed or claimed as shipped.

# Badminton Premium v27.9 — Owner-phone release-acceptance sheet

**State:** TEST ONLY. Not a production release; do not merge PR #5 or replace the stable download merely because CI passes.  
**Stable source:** `main` v25.9.1, app ID `com.vinayak.badminton.premiumv24`, original permanent signing identity.  
**Candidate:** GitHub Actions owner-signed **v27.9 TEST** (not the debug APK); use the most recent fully green signed-APK run, confirm package/versionCode and original signing certificate.  
**Synthetic signed upgrade:** [previous verified baseline run #43779574426](https://github.com/vicpurnia/Badminton-club/actions/runs/43779574426). Follow-on artifact and file-provider tests are separately tracked and must be green.

## Before touching the installed owner's phone

1. Record the installed app version and Android version. Do not uninstall, clear application data, reset the Room database or install a debug APK over the owner's signed app.
2. Use the app's own **BACK UP NOW**, then verify the external JSON backup was written to a user-chosen folder and **read back** successfully. Keep a second copy offline on another device. Do not treat an app-private backup as recoverable after uninstall.
3. Run the **read-only restore rehearsal** against the external JSON. Check complete verified field comparison; do not perform a live restore over an existing ledger.
4. Record a *pre-upgrade* baseline from actual original records: 15 historical members and their IDs; original receipt IDs and date-evidence flags; cash, corrections and closing balance; archived members; expense history; stock physical counts and audit; existing fee policies/concessions; Feb 2026 Kanishk donated shuttle pack as **noncash**; unresolved Feb 2026 **INR 1,000** difference.
5. Keep a verified working **original signed stable v25.9.1 APK** and external backup available. Do not attempt an upgrade if the backup or comparison fails.

## Controlled phone test (owner decision, not performed by CI or ChatGPT)

- Install only a certificate-verified, same-package owner-signed **TEST** APK **over the existing app** as a normal Android update. No uninstall or clear-data.
- Immediately reopen and verify all recorded pre-upgrade ledger fields match before creating new entries. Any discrepancy is a **STOP / no production approval**.
- Open **Home, Members, Collect, Shuttles, Reports** and member fee centre; scroll to the 15th player; search member names with case/space/Unicode variants (do not create duplicates).
- Compare dues, waivers, unconfigured months, partial payments, advances and actual cash; ensure no fee is fabricated or silently moved from one month to another.
- Generate original/reissued/corrected/void receipt PDFs where supported; preview, share via Android chooser/WhatsApp if installed, and save through Android **Documents / SAF**. Verify the actual saved pages, amounts, names and file sizes.
- Export a member receipt ZIP, move it outside the app, and open it with a separate ZIP reader; verify **all** receipt PDFs are present and readable.
- Generate the **internal expense voucher** after a recorded shuttle purchase. Verify it states it is **not a supplier tax invoice** and does not create an additional expense. External paper-bill scanning is not implemented in v27.9.
- Compare physical stock, pack sizes, usage and count variance. Do not silently reconcile or overwrite a recorded stock shortage/surplus.
- Open/close/reopen a completed test month only with proper audit reasons; ensure closed-month drift and stale review edits are blocked.
- Verify the original **Classic Master Ledger remains one-page A4 landscape**, separate from independent analytics exports. Print a physical A4 test page and inspect legibility.
- Export encrypted backup with a unique strong password, verify the file via SAF read-back, move to a **separate empty test phone**, reject wrong password/tampering, rehearse read-only restore, then import **only into the empty app** and compare all original records. Never restore over a populated phone.
- Exercise Android Back/rotation, multiple rapid taps, share sheet cancellation, a denied Documents permission, large font/accessibility settings, and interrupted/low-storage save, checking that data is not silently lost.

## Release sign-off table

| Gate | CI evidence or human check | State |
|---|---|---|
| Original Android app ID / permanent cert unchanged | Automated signed TEST workflow | Previously passed |
| Signed v25.9.1 -> v27.9 same-package synthetic persistence | [Run 43779574426](https://github.com/vicpurnia/Badminton-club/actions/runs/43779574426) | PASS — synthetic only |
| New v27 receipt ZIP / PDF FileProvider / voucher / AES staging | New signed emulator workflow | Reverify latest green run |
| Exact owner's ledger before/after signed phone update | Owner's device | **NOT DONE** |
| SAF/WhatsApp/print, accessibility and actual phone battery/rotation | Owner's device | **NOT DONE** |
| Cross-device encrypted recovery of verified copy into empty phone | Owner's second device | **NOT DONE** |
| Production merge and stable release | Only after all mandatory gates and explicit owner acceptance | **BLOCKED** |

**Release policy:** Any failed, missing or inconsistent safety-critical acceptance leaves PR #5 DRAFT, production `main` unchanged, and stable v25.9.1 the approved installation. A synthetic test never certifies the owner's historical data.

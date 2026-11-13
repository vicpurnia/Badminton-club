# Badminton Premium Native v23.1 — November 2026

### Purpose
Improve the green one-page Classic Master Ledger on Android without altering historical cash balances or member payments.

### Corrected PDF
- Native **A4 landscape**: 842 × 595 PDF points, with a one-page orientation assertion before direct Save/Share.
- Increased player names and monthly cash values where they fit; stronger two-line cash/pack grand totals.
- Four balance cards retain original opening, collections, expenses and closing values.
- Shuttle-purchase details use two separate lines. Actual **day-of-month** is shown only when a valid date exists in the ledger. Otherwise, a clear “purchase dates not recorded” label is used. Amounts are not invented.
- Unresolved February 2026 cash reconciliation difference of ₹1,000 remains visible separately from the February missing-pack warning.

### Verified noncash repair
- A dedicated confirmation in the Reports screen adds **one donated shuttle pack** for member p11 (archived February 2026).
- Requires both member ID and expected name to match; checks legacy and contribution entries; applies only once inside a database transaction; writes an audit entry.
- The day of donation is **unknown** and stored as an approximate placeholder, not a verified date.
- This correction **does not** add cash or alter reported cash collections, expenditure or closing balance.
- Until the user confirms the correction on their installed app, the report **continues to flag the missing pack**. A code update alone does not silently mutate the user's local ledger.

### Installation / important
- Uses a **separate app ID `premiumv231`** because GitHub's ephemeral debug signing certificate differs across workflow runs. **This is NOT an in-place update**: it installs alongside the old v23 app and starts with an empty private database. Export a verified ledger JSON from your currently installed version, import it into v23.1, reconcile all balances, and retain the older app until the import has been checked. Do **not** uninstall the existing app to resolve any installation or signature error.
- Open **Reports → Save Classic PDF to phone**; choose a folder. Avoid using Android **Print → Save as PDF** on a shared PDF, which can re-scale a landscape file to a portrait page (as happened in the uploaded example).
- Never assume the imported expense records include a date. If the app displays “purchase dates not recorded”, review the source entries before assigning dates.

### Verification
- Build workflow: `.github/workflows/build-native-v23_1.yml`.
- Existing Java accounting regression tests run before compilation.
- Workflow performs static source assertions on PDF size, historical correction safeguards, and new renderer.
- Android installation, data import and PDF export still require device testing. GitHub's accounting tests and compilation validate the code and APK build, not a real on-phone migration.

The repository contains code only; do not commit real member financial exports or signing keys.

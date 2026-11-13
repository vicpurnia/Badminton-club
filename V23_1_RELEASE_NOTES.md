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
- Keeps the same Android application ID as v23 (**premiumv23**), with higher versionCode 22. This is an in-place v23 upgrade, not a new empty app. Backup the verified ledger before upgrading.
- Open **Reports → Save Classic PDF to phone**; choose a folder. Avoid using Android **Print → Save as PDF** on a shared PDF, which can re-scale a landscape file to a portrait page (as happened in the uploaded example).
- Never assume the imported expense records include a date. If the app displays “purchase dates not recorded”, review the source entries before assigning dates.

### Verification
- Build workflow: `.github/workflows/build-native-v23_1.yml`.
- Existing Java accounting regression tests run before compilation.
- Workflow performs static source assertions on PDF size, historical correction safeguards, and new renderer.
- Real Android installation and PDF export must still be checked on a device; no successful build is asserted here prior to observing the workflow.

The repository contains code only; do not commit real member financial exports or signing keys.

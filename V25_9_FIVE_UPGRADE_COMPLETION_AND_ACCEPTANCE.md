# Badminton Premium v25.9 — Five-upgrade completion and acceptance notes

## Scope and release continuity

This is the cumulative upgrade from the **previously signed, preserved-data v25.4 stable baseline**. The GitHub release workflow retains the Android application ID `premiumv24`, increases versionCode and checks the APK signing fingerprint against the same permanent owner-controlled release certificate. The existing Room database remains **schema version 1**; no uninstall, clearing app data or data migration is required by the new features.

The latest release incorporates v25.5 through v25.9.

### v25.5 — Premium Receipt Centre
- Search historical and present cash payments by member, period, original payment ID, amount, date or available payment method.
- Preview, share or save individual recorded receipt copies and clearly labeled legacy-period statements.
- No cash transaction is created by a receipt action, and missing original historical dates are never invented.

### v25.6 — Audited cash corrections, cancellations and refunds
- Correct a recorded payment's **recognized cash amount**, record an actual refund as a reduction of recognized cash, or cancel an incorrect original entry.
- Require explicit review, an 8–200-character reason and a balance-impact confirmation.
- Preserve the original immutable payment and append corrections under `vinayakCashCorrectionsV1` in the existing verified metadata, with atomic Room audit events.
- Financial summaries, Classic A4 landscape cash cells, monthly cashbook and receipt PDF status use the effective recorded amount; canceled entries appear as VOID, not a newly valid receipt.
- Historical 2025 *aggregate* period records remain immutable and are shared only as statements.
- Existing verified migration JSON can be imported without the correction property; JSON export/import is adjusted so official cash totals are not double-counted.
- A recorded cash amount of zero cannot be created as an ordinary payment; cancellations are always explicitly documented correction events.
- **Release limitation:** corrections are audited but the final acceptance of cash refunds and installed-phone round-trip backup must still be verified on a phone.

### v25.7 — Physical shuttle stock and mixed packs
- Recording new purchased/donated packs permits an explicitly confirmed shuttles-per-pack quantity (1–60); when absent, the operator-confirmed opening pack size remains the transparent fallback.
- Record physical *month-end* stock counts only for completed calendar months and only after the verified opening baseline.
- A physical count becomes an audited, append-only stock anchor for later months. Purchases, donations, usage and cash history are not rewritten.
- The screen distinguishes physically counted balance and calculated stock. A deficit remains visible; missing historic usage is never fabricated.

### v25.8 — Monthly review and audited closing
- Review selected month's opening balance, official collections, member cash, expenses, closing balance and any unresolved difference.
- Generate a separate one-page **A4 landscape month-end review** without changing the approved Classic Master Ledger layout.
- Close a completed month only with an audit reason. Reopening requires another audit event.
- Block payment additions, cancellations, refunds, expense entries, donated packs, usage and physical stock transactions for a closed month until reopened.
- The unresolved February 2026 ₹1,000 variance remains explicitly visible and is **not silently reconciled** by closing a month.

### v25.9 — Recovery rehearsal and independent backup checks
- **Verify Latest External Backup** independently reopens the newest JSON in the user-selected Storage Access Framework folder, validates its file digest prefix, verified JSON envelope and financial summary. This does not import data.
- **Test Restore Without Changing Data** imports a selected verified JSON into a temporary in-memory Room database, exports and re-validates it, compares balances, record counts, adjustments, inventory and month-close state, and discards the test database.
- No restoration process writes to or replaces the installed ledger.
- Backup files are unencrypted member financial records; store them in a private location.

## Financial integrity invariants

- The historical 15-member baseline is preserved without inserting fictitious data.
- November 2026 reference: ₹4,350 closing credit before any later real transactions.
- Kanishk's February 2026 +1 donated shuttle pack (noncash) remains; historical total ₹8,500 +1 pack.
- February 2026 ₹1,000 cash reconciliation discrepancy remains explicitly unresolved.
- Original cash entries, donated-pack history and all prior audit events stay intact.
- Classic Master remains A4 landscape and one-page within its supported reporting width.

## Build and rollout

The GitHub workflows check Java accounting regressions, stock arithmetic, audited metadata integration, Android release compilation and the permanent certificate SHA-256 before uploading a signed APK. These CI checks cannot substitute for installing and inspecting the APK on an Android device.

Safe rollout:
1. From the currently installed signed Badminton Premium, **export a verified JSON** to a secure location outside app-private storage.
2. **Do not uninstall or clear app data.** Install the new signed APK over the installed app.
3. Check member count, old receipts, February donation, unresolved reconciliation and the latest actual closing balance.
4. Verify an external backup and perform a dry-run restore on the phone using the new recovery tools.
5. For v25.6 financial corrections, use a genuine known transaction and check before/after totals and PDF status. Do **not** create fictitious corrective events in production.
6. For v25.7, enter only real verified physical quantities; don't fabricate a month-end stock count.
7. Create a new verified external backup after acceptance.

**Release readiness boundary:** GitHub compilation and signing can be verified remotely. Actual Android in-place data preservation, the external storage provider, receipt chooser, and the in-memory restore rehearsal still require on-device acceptance. Keep the previous verified JSON until those tests pass.

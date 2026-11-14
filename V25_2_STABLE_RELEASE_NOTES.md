# Badminton Premium v25.2 — User-confirmed opening stock

An in-place update to the signed v25.1 stable Android app. This version uses the **same application ID and permanently pinned certificate** as v24–v25.1, with a higher version code. There is **no Room schema change** and no replacement signing key.

## What it adds

- **Configure confirmed opening stock** on the Shuttle screen. The operator enters a real count of *individual shuttlecocks at the beginning of the selected month* (before that month's purchases, donations or usage), the confirmed shuttles-per-pack factor, and an optional note describing how the count was established.
- The checkpoint is never auto-filled with an imagined quantity or assumed pack size. It cannot predate the ledger opening month or be placed in the future.
- On confirmation, the app writes the configuration to the existing `LedgerMeta.inventoryStockJson` field and adds an audit event in a single Room database transaction.
- Existing source inventory properties are preserved. The checkpoint is stored under a namespaced `vinayakStockCheckpointV1` object within the historical `inventoryStock` JSON.
- **Verified account JSON exports now include the current checkpoint**, and import and read-only backup comparison validate/compare it. Existing files lacking a checkpoint remain valid.
- A new stock calculation displays **remaining individual shuttles** from the chosen month onward: starting count + purchased/donated packs × confirmed shuttles-per-pack − recorded individual-shuttle usage. Stock flows before the checkpoint are excluded. A negative result is shown as a **recorded stock deficit**, not quietly hidden.
- The app distinguishes a calculated balance from a fresh physical count; the two should be reconciled as needed.
- A checkpoint can be explicitly replaced after reviewing a warning. Previous user confirmation is retained in append-only audit history, and transaction records are never rewritten.

## Not changed

Member contributions, expenses, donations, historical 2025 data, February 2026 unresolved ₹1,000 reconciliation item, Kanishk's February 2026 donated pack, classic A4 landscape report, and existing Room database schema are unchanged by the feature.

## Important reporting assumptions

- Acquisitions are in **packs**, while usage is in **individual shuttles**. The factor is supplied by the user, not guessed.
- The checkpoint is as of the **start of a full calendar month**. If only today's physical count is known, do not pretend that it was also the count at the beginning of this month. Pick a month with a real documented start count, or do not configure a checkpoint.
- This assumes the same shuttles-per-pack factor applies to all recorded packs after the checkpoint. If pack sizes vary, the calculated remaining balance requires further data capture.
- A shortage may indicate missing usage, a mistaken baseline or missing acquisitions; the app does not automatically change financial entries to compensate.
- The historical 2025 donations are not added again to any checkpoint from 2026 onward, avoiding double-counting stock acquired before the checkpoint.

## Installation and phone verification

1. Create a **verified JSON export to an external/private location** from the currently installed signed v25.1 (or earlier) app. Keep the previous app installed.
2. Install the successfully verified **v25.2 signed APK over the existing stable app**. Do not uninstall the app, clear data or recreate the keystore.
3. Verify existing member histories, historical balances and the donated pack before configuring stock.
4. Only when a true start-of-month count and pack factor are available, set the new stock checkpoint and review the calculated position.
5. Export a new verified JSON and open **Compare Saved JSON with Current Ledger**; confirm the stock checkpoint matches. On a new phone, restore from that new JSON, not an older pre-checkpoint copy.

CI tests cover accounting regression, stock arithmetic edge cases (including deficits/overflow rejection), Room/JSON source integration, Android release compilation, and signing certificate pin. They **do not substitute for on-phone installation, data retention, or real inventory reconciliation**.

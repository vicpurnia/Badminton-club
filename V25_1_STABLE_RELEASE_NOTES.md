# Badminton Premium v25.1 — Shuttle usage ledger

## Scope
Builds on the data-preserving signed v25 stable app. Same Android application ID (`com.vinayak.badminton.premiumv24`), same permanently pinned release certificate, and higher versionCode. **No Room schema migration or historical data rewrite.**

## New capabilities
1. Shuttle screen has **Record shuttle usage**. Enter reporting month, optional real YYYY-MM-DD usage date, individual shuttles used (1–1000), and optional note. Each new entry requires explicit confirmation.
2. Usage is added to the already-existing Room `stock_usage` table using its documented `StockUsage` entity; no new tables or database migrations.
3. Entries are append-only with a unique ID and an audit-event record committed in the same database transaction. Each save triggers a verified JSON backup; backup failures are surfaced to the user.
4. Shuttle screen separately shows individual shuttles used in the selected month, all-time logged usage, and a read-only list of usage entries with actual/unknown dates.
5. Verified JSON export and reimport already serialize `stockUsage`. v25.1 also displays the usage-entry count when verifying, comparing, or importing backup files.
6. The app refuses member, cash, purchase, donation and usage writes until a verified ledger has been imported; no orphan transactions should be created in an uninitialized database.

## Important distinction
Purchased/donated units are **packs**. Usage units are **individual shuttlecocks**. The app deliberately **does not** calculate remaining packs or shuttles without a verified opening stock and known shuttles-per-pack factor. No pack size or historical usage is invented.

## Financial invariants
- Stock usage is noncash; adding a record does not change collections, expenditure, monthly closing balance or receipts.
- Kanishk's restored February 2026 +1 donated pack and the ₹1,000 unresolved February cash difference are not modified.
- All existing member records, previous usage history, purchases, donations, and classic A4 landscape reports remain in place.

## Installation and safety
1. Export a verified JSON from your currently installed signed v24.3/v25 app to a secure location outside its app-private storage.
2. Install v25.1 **over** the existing stable app (do not uninstall, clear data, or replace the signing key).
3. Confirm the existing ledger and February historical entries, and compare the saved backup with the current ledger. Confirm the counts of any previous stockUsage entries match.
4. Open Shuttles, record **one genuine small usage entry** (not fictitious test consumption in production), verify it appears under the selected month, and export a new verified backup.
5. Test that the backup opens in the read-only verification screen before deleting any older backup.

## Verification limits
GitHub regression suite, Android release compilation and SHA-256 certificate pin verification cover the build, not the phone's actual data preservation or particular Android document provider behavior. Real-device usage entry and backup restoration still require on-device confirmation.

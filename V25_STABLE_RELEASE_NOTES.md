# Badminton Premium v25 — Member History & Shuttle Desk

## Purpose

A daily-use improvement based on the verified, data-preserving v24.3 stable baseline. **All changes are additive UI and entry workflows** using the existing ledger entities; no Room schema migration or changes to historical cash controls, outstanding February 2026 reconciliation discrepancy, or classic A4 PDF calculation are introduced.

### Member account — complete history
- Member account now shows *all recorded monthly contributions*, not only the most recent 12.
- It displays separate imported historical 2025 periods in their original code (FM25 / AM25 / JJ25 / AS25 / ON25 / Dec25 where present).
- Donations are counted from **both imported legacy records and new contributions**, so the noncash pack total includes 2025 donated packs.
- Cash and packs are shown separately. Approximate receipt dates are not falsely displayed as known exact dates.

### Shuttle Desk — acquisition ledger
- Two actions: **Record shuttle purchase** and **Record donated shuttle pack**.
- Purchases record month, optional *actual* YYYY-MM-DD purchase date, pack quantity, unit price, total cash expense, and an audit record. Dates are validated and cannot be forced into a different month.
- Donated packs record the member, month, pack count, optional actual date, and audit record. Donation kind is `packs`, not `cash`, so it must not change cash collections, expenditure or closing balances.
- Each new transaction triggers the existing verified backup flow. There is a confirmation dialog before new expense or donation entries; previously imported entries are never rewritten.
- The shuttle screen shows monthly spending, purchased packs, donated packs and total **acquired** packs. It lists dated purchase and noncash donation entries separately.
- Historical 2025 donated packs are shown separately, rather than pretending they belong to a specific selected 2026 month.
- **Not a live stock balance:** shuttle usage/issue counts have not been recorded. The app does not guess remaining packs. A future verified inventory-use ledger would require a separate schema/export design and explicit user review.

### Existing verification baseline
- November 2026 closing balance previously verified: **₹4,350 credit**.
- Kanishk February 2026 noncash donation: **₹8,500 +1 pack**.
- February 2026 **₹1,000 unresolved historical cash reconciliation** retained.
- Classic PDF remains genuine A4 landscape, using the existing renderer.

### Signing and installation
The v25 stable release workflow uses the same Android application ID **`premiumv24`** and the permanent SHA-256-pinned signing certificate as prior stable v24.x releases. It raises the version code. This is an in-place upgrade; do **not** uninstall v24.3 or clear its data. Save a verified external JSON backup first.

The first stable installation on a new phone still needs verified JSON import. Installation, backup-provider readback, and actual noncash donation behavior must be checked on a phone; passing the CI Java accounting suite and Android compilation does not by itself establish those device checks.

The public repository contains only code and synthetic tests, not member finance exports, keystores or passwords.

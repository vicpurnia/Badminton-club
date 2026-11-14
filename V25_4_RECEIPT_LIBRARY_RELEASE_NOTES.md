# Badminton Premium v25.4 — Searchable Receipt Library

## Why this release

Signed v25.3 can re-share an existing payment from a member's history. v25.4 makes older cash receipts easier to locate without navigating every member or manually switching months.

## Receipt Library

- Open from **Collect → Find or share any past receipt** or **Reports → RECEIPT LIBRARY · ALL MONTHS**.
- Search all available recorded cash payments by **member name, fee month / original historical period code, entry ID, or amount**. Searches accept plain numbers and formatted rupee amounts.
- Filter **All**, **Recorded**, or **Historical**. Results show 25 initially, with a **Show next 25 receipts** action for further records. No financial records are trimmed or changed.
- Each recorded cash entry has **Share receipt** and **Save PDF**. The PDF uses the existing record's member, exact amount, period, original entry ID, and available payment metadata.
- Each archived **2025 period-level cash total** has **Share statement** and **Save PDF**. It is explicitly a **Historical Contribution Statement**, not a newly dated receipt.
- Noncash donated packs are deliberately excluded from all cash-receipt results.
- All actions are read-only: generating, saving or sharing a PDF does **not** insert or modify a payment, regenerate a cash transaction, or affect balances.

## Data and financial safeguards

Do not invent historical payment dates, methods or individual receipt IDs for archive-period totals that did not retain them. When individual receipt date/method are unknown, the PDF states so. The PDF generation date is not the date of payment.

This upgrade preserves:
- the existing Android application ID and owner-controlled permanent signing certificate
- all member contributions, donations, purchases, stock-usage entries and the confirmed inventory checkpoint
- the original one-page Classic Master A4 landscape report
- Kanishk's February 2026 +1 donated shuttle pack
- the outstanding historical February 2026 ₹1,000 cash reconciliation discrepancy

The app uses the same Room database; no schema migration or re-import is needed for an in-place upgrade.

## Installation and acceptance

1. Export a verified JSON copy from the currently installed signed stable app to an external location.
2. Install the signed v25.4 APK *over* v24–v25.3. Do not uninstall the earlier stable app or clear data.
3. In Receipt Library, search a member with several paid months; verify correct amounts and months. Share and Save one real recorded receipt.
4. Filter Historical, select an archived 2025 cash period, and confirm the PDF is labeled **Historical Contribution Statement** with an unknown exact date/method.
5. Confirm member balances, the Classic Master report, and the verified backup are unchanged.

GitHub CI covers Android compilation, Java accounting and inventory checks, the previous receipt renderer, and the signing-certificate fingerprint; actual Android share-sheet and save-provider behavior require on-device verification.

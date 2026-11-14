# Badminton Premium v25.3 — Re-share receipts for previously recorded payments

## What changed

- **Collect → Share previous paid receipt** opens the existing Members list. Select a player to see cash entries and earlier imported periods.
- **Members → [Player] → Recorded monthly entries → Share receipt** creates and shares a PDF for any *existing, positive cash contribution*. This includes recorded and imported cash payments, not just the current month.
- **Members → [Player] → Imported historical periods → Share statement** generates a PDF **Historical Contribution Statement** for a positive cash amount in an original imported period such as FM25 or ON25.
- **Collect → Monthly receipts** also uses the safer shareable recorded-payment PDF for its existing cash records.
- PDFs can be sent via the Android share chooser (WhatsApp, email, Bluetooth, etc., when installed).
- The history remains **read-only**: tapping share does not call `insertPayment`, create a receipt transaction, or modify balances.

## Dates, source IDs and historical integrity

A known original individual ledger entry ID is shown on the recorded-payment PDF. Its original amount, member, fee month, payment date and method are taken from the actual entry. Where the original entry has an approximate or unavailable exact date, the PDF explicitly says **Exact payment date not recorded**; an unknown method says **Not recorded**. The PDF generation timestamp is labeled separately and must never be presented as the date money was received.

The 2025 import stores some prior payments as **period totals** rather than individual dated receipts. Because a period-total entry does not document each payment date or method, this version shares a **Historical Contribution Statement**, not a fabricated backdated receipt. Original period codes are preserved. Donated shuttle packs are excluded from cash receipts.

## Installation and regression requirements

Same permanently signed Android application ID as stable v24–v25.2. APK is intended for **in-place installation** without uninstalling, clearing data or importing historical records again. Export and separately verify the current JSON backup first.

The unchanged accounting rules preserve all members, paid totals, Kanishk's February donated pack, the unresolved February ₹1,000 discrepancy and the A4 landscape Classic Master Ledger.

GitHub tests: Java accounting regressions, stock arithmetic tests, source/Room/verified-JSON checks, signed release compilation, and pinned certificate fingerprint. Real-device share chooser and printer rendering still require phone verification.

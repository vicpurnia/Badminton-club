# Badminton Premium v25.9.1 — Home Dashboard Tap Fix

## Issue reproduced in source

In the signed v25.9 Home screen, the **Players (15)** and **Receipts (2)** count cards were rendered using a plain `metric(...)` view with **no click listener**, even though they visually resembled shortcuts. This is consistent with the user's Android screenshot: pressing those numbers did nothing.

## Fix

- **Players** card now opens the existing **Members** tab and its searchable list.
- **Receipts** card now opens **Collect** and automatically scrolls to the **Monthly receipts** section for the selected reporting month. The home count and destination refer to the same month.
- The cards now have a tappable chevron hint, press ripple, accessibility content description, focusability, and full-card click target.
- When switching tabs, page scrolling is reset to the top. The Receipts shortcut performs its own controlled scroll **after** the monthly receipts list has been rendered.
- The change does not alter source transaction records, historical balances, v25.9 corrections, month closings, inventory, backup formatting, PDF calculations or the Room schema.

## In-place upgrade safety

Signed stable v25.9.1 continues using the **same Android package ID, database name and permanent certificate**, and increments versionCode through the existing stable workflow. Install it **over v25.9 without uninstalling or clearing data**. Export a verified external JSON backup first.

## Acceptance on Android

1. Open Home. Confirm the selected November 2026 count is still **15 Players** and **2 Receipts** for the supplied historic reference, unless genuine payments have since changed.
2. Tap anywhere inside the **Players** card → the Members tab must open with searchable player rows.
3. Return Home, tap anywhere inside **Receipts** → the Collect tab must open and move to November 2026's receipt entries, including the two currently counted payments.
4. Change the viewing month, return Home, and repeat Receipts: it must navigate to **that newly selected month**, not the original November 2026.
5. Check that the newly shown PDF share links work and that all original cash totals and stock records remain intact.
6. Verify another external backup. Do not use production payment corrections merely as a test.

GitHub Android compilation and certificate verification cannot prove physical touchscreen behavior; the final tap-acceptance is completed on the user's phone.

# Badminton Premium v24.2 — verified manual export and backup comparison

## Scope
This is an **in-place update of the permanently signed v24 stable app**. The package ID remains `premiumv24`; the release workflow reuses the owner's privately configured signing key and rejects a certificate fingerprint different from `BADMINTON_EXPECTED_CERT_SHA256`. The workflow assigns the next increasing Android version code. Do not create a replacement signing key.

## Changes

1. **Verified Account Export now reads the saved file back.** After Android writes JSON to the user-selected document location, v24.2 reopens the resulting file and validates byte length, SHA-256 and the verified ledger structure. A failed check produces an explicit error instead of a false success message.
2. **Read-only compare backup with current ledger.** The Reports option is named `COMPARE SAVED JSON WITH CURRENT LEDGER`. Selecting any valid JSON shows members, contribution counts, expenditure counts, latest reporting month and closing balance for the selected backup and the in-app ledger. It does **not** import, replace, merge or modify any records.
3. Existing v24.1 Home backup health, backup failure warnings, and auto-snapshots are retained. Existing A4 landscape reporting, Kanishk's noncash February correction, and the unresolved historical ₹1,000 February cash discrepancy remain unchanged.
4. **No accounting schema or calculation changes** were made.

## Installation

- **First:** In your currently installed signed v24/v24.1, export a verified JSON into a trusted location. Keep the previous signed app installed and its data intact.
- Download the v24.2 signed APK from the successful `Build Badminton Premium v24 STABLE` GitHub run.
- Install over signed v24 or v24.1. Android should recognize the same package ID and signing identity. **Do not uninstall the existing app, clear data, or reinstall a debug preview.**
- In v24.2 open Reports, use `COMPARE SAVED JSON WITH CURRENT LEDGER`, and check the stored account summary. Confirm the November 2026 closing balance (historically ₹4,350 credit), Kanishk's ₹8,500 +1 donated pack, and the unresolved February ₹1,000 reconciliation note, subject to any later genuine ledger changes.
- Create and verify another external backup after the update.

## Boundaries

An exported JSON file is unencrypted and contains member financial information; keep it private. A selected cloud-synced folder can transmit files under that provider's settings. The app itself does not automatically upload them.

Read-only counts and matching balances are screening checks, **not proof that every member transaction matches**; full verified import remains blocked for a nonempty target database to prevent destructive replacement.

GitHub build, accounting regression tests and signature checks are not substitutes for testing a real Android update and provider-specific Storage Access Framework writes on your phone. The stable signed artifact is not claimed ready until the workflow run succeeds.

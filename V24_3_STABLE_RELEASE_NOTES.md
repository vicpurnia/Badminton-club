# Badminton Premium v24.3 — consistent backup recovery limits

The v24.3 Android application is a **stable in-place upgrade** from the previously signed v24, v24.1 and v24.2 builds. The release workflow continues to use the **same package ID** (`premiumv24`), the owner's persistent signing key and the pinned SHA-256 certificate. It uses a higher Android version code from the stable workflow run number.

## Changes

- The verified automatic snapshot, manual export, recovery JSON picker and verified import UI now use the same **12,000,000-byte maximum**, rather than automatic backups permitting 12 MB but the picker rejecting anything above 6 MB.
- Both import and comparison file readers enforce the size cap **before** adding new bytes to memory. JSON verification remains required. Any additional restrictions in the existing migration codec still apply.
- Manual Verified Account Export checks the 12 MB cap before displaying the Android save picker.
- Home and Reports include a **Recovery readiness** explanation based on the date of the latest private and external verified backups. The app distinguishes private-only snapshots, unverified external folders, and external copies older than the latest private snapshot.
- The top app header identifies **STABLE v24.3** to avoid confusing the signed release with earlier preview installs.
- Existing backups are not deleted during the upgrade; the previously implemented 7-copy rotation of app-private snapshots remains unchanged.

## Not changed

No financial calculations, member payment records, donation entries, Feb 2026 ₹1,000 discrepancy, classic A4 landscape PDF generator, Room database schema or signing identity were modified by this upgrade.

## Safe installation

1. On the currently installed **signed stable** Badminton app, select Reports → Verified account export and save the JSON to a private location outside the app. Check that the file reopens.
2. Install the signed v24.3 APK **over** the already installed signed stable app. **Do not uninstall or clear its data**.
3. If this is the first stable installation on the phone, import a verified JSON once into the empty stable database. Existing nonempty databases remain protected from destructive reimport.
4. Confirm the current account balances and the February donated pack using the actual ledger (historical November 2026 reference: ₹4,350 credit). Later legitimate transactions may change the current balance.
5. Review Home → Backup health and Reports → Recovery readiness. For uninstall-proof copies, select an external folder, use Back Up Now and Verify Last Private Backup; external providers may remove files later, so retain separate verified copies.

## Verification

GitHub CI performs Java accounting regression tests, Android compilation and disposable-key signature checks. The stable workflow signs using the owner's protected key and verifies the certificate SHA-256 against the pinned public fingerprint. These tests **do not prove a device-level data-preserving update**; that must be checked on the Android phone.

The unencrypted JSON contains club account data; store it privately. Neither member financial files nor private signing keys are added to the repository.

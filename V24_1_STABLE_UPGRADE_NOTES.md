# Badminton Premium v24.1 — signed in-place upgrade

Builds use the **same Android application ID** (`premiumv24` in the Android Gradle source) and **same permanently pinned release certificate** as signed v24. No new signing key is generated.

## Changes

- Corrected the opening screen: it no longer calls the signed stable release an unsigned preview.
- Home now has a **Backup health** section with the most recent private/external backup status.
- Home now offers **Back up now** and **Verify backup** shortcuts.
- If automatic backup fails after member, contribution, expense, import, or verified noncash donation changes, the user receives a visible warning. The saved ledger transaction is not undone or silently altered.
- The Reports screen explicitly says to review any backup-folder warnings.
- No edits to the contribution ledger, historical February discrepancy, donated-pack correction, A4 landscape report calculations, or Room database schema.

## Upgrade and data preservation

The Android release pipeline assigns `versionCode = 24000 + stable-workflow run number`. Stable v24 was built in workflow runs #1 and #2; this v24.1 upgrade uses #3 or later, so Android treats it as a higher version. The certificate fingerprint must match `BADMINTON_EXPECTED_CERT_SHA256`; an incorrect certificate blocks artifact upload.

If **signed v24 is already installed**, install v24.1 over it without uninstalling. Your existing Room database and external backups are intended to remain in place. If **v24 was not installed**, v24.1 is a first-time stable installation and will need one verified JSON import from the previous badminton app.

Before installing any update, save a verified JSON backup in an external location and verify that the previous app still shows November 2026 closing credit ₹4,350 and Kanishk's February +1 donated pack. After updating, verify both again and confirm the unresolved historical February ₹1,000 discrepancy remains visible.

Do not uninstall the previous app merely to resolve a signing conflict. Never share the .p12 signing keystore or its password in chat.

## Validation

- GitHub Java accounting regression suite.
- Android release compilation.
- Pinned APK certificate verification using the owner's persistent signing key.
- Separate disposable-key signing smoke test.
- Device installation and real backup read/write still require verification on an Android phone.

No member financial records, private keystore, or signing password are stored in this repository.

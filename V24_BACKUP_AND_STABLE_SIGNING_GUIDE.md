# Badminton Premium v24: Backups, safe migration, and permanent Android signing

## Current status

The v23.2 app and its **November 2026 landscape ledger** are the verified baseline. The v24 **Preview** adds automatic verified JSON backups but is a separately installed test app. It does not overwrite v23.2.

The **Stable** v24 release is prepared in `.github/workflows/build-native-v24-stable.yml`. It will produce a properly signed APK **only after the repository owner provides a permanent private signing key through GitHub Actions secrets**. Never commit or paste passwords or keystores into this public repository, an issue, or a chat.

### Before installation (required)

1. From the currently working v23.2 app, choose **Reports → Verified account export** and save the JSON backup in a safe Documents folder.
2. Keep v23.2 installed until you have successfully imported and verified the future v24 Stable app.
3. Preserve the unresolved February 2026 **₹1,000 cash discrepancy**, and check that Kanishk shows **₹8,500 +1pk**.
4. Check that November 2026 still closes at **₹4,350 credit**.

## Backups in v24 Preview and v24 Stable

- A verified JSON snapshot is attempted after successful import, member addition, cash contribution, shuttle expenditure, and the one-time February donation correction.
- A daily startup snapshot is attempted if no verified snapshot was recorded within the previous 24 hours.
- The app retains up to **seven** dated verified JSON snapshots in its own app-private files. **These private copies are lost if the app is uninstalled or its storage is cleared**.
- For persistent copies, in **Reports → CHOOSE EXTERNAL BACKUP FOLDER**, choose a dedicated writable subfolder under Documents using Android's folder picker. The app stores persisted Android folder permission and writes new dated JSON copies there after ledger changes.
- External backups are *opt-in and append-only*; old external copies are not automatically deleted. It is the user's responsibility to manage their storage space.
- In **Reports → BACK UP NOW**, you can force a new snapshot and read the backup status, including errors and the last external copy.
- Under **Reports → VERIFY LAST PRIVATE BACKUP**, the app checks the most recent private JSON snapshot's filename checksum and ledger envelope without changing accounts.
- Under **Reports → VERIFY ANY JSON BACKUP**, choose a previously exported or external copy; the app displays the account counts and balances after read-only JSON verification. This does **not** overwrite or restore any records.
- Each new external copy is read back and checked against its SHA-256 digest and JSON structure before being marked successful. A failed write is reported, and any newly created partial external file is deleted where the provider allows it. Older backups remain untouched.
- **Restore / import verified JSON** is available only when the destination app database is empty. Import is blocked for a nonempty ledger; no automatic destructive replacement takes place.
- The JSON files contain member financial records in **unencrypted plain text**. Store them privately. The app does not upload them on its own; a selected cloud-synced Documents provider may sync copies according to that provider's settings.

## One-time stable signing key setup (repo owner)

### Recommended: guided Windows setup

A Windows one-time setup script is included at
[tools/SETUP_BADMINTON_STABLE_SIGNING.ps1](tools/SETUP_BADMINTON_STABLE_SIGNING.ps1).

On a **trusted Windows computer**, install [GitHub CLI](https://cli.github.com/) and a JDK or Android Studio, then download the script from GitHub. Open PowerShell in the folder holding that downloaded script and run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\SETUP_BADMINTON_STABLE_SIGNING.ps1
```

The script will sign you into your own GitHub account, create or reuse **one local permanent Android signing keystore**, ask for a password, show the certificate SHA-256 fingerprint, require a confirmed **offline backup**, store the four GitHub Actions secrets via standard input, and pin the release certificate fingerprint in a GitHub Actions repository variable. It optionally triggers the signed release workflow once you type BUILD.

The script does **not** send the keystore or passwords to ChatGPT or commit them into the repository. It protects against accidental key replacement: if a certificate is already pinned, it refuses to create an unrelated signing key. **A brand-new stable app requires one import; later stable APKs use the same key and app identity and can update in place.**

The key is saved locally under `%LOCALAPPDATA%\Badminton_Stable_Signing_Keep_Private\badminton-stable.p12` unless a different path is explicitly selected. **Copy the .p12 file to an offline drive and store the password in a secure password manager before completing setup.** Loss of both the key and its backup can prevent future in-place updates.

### Manual alternative (Windows, macOS or Linux)

Because Android checks both **application ID** and **signing certificate** during an upgrade, a v23.2 debug APK and a future stable APK cannot safely be treated as interchangeable. v24 Stable has its own app ID, with the explicit goal of **one import, then in-place upgrades for all subsequent stable versions**.

1. On a trusted PC with JDK installed, create an Android signing key. Example command:

   `keytool -genkeypair -v -keystore Badminton-stable.p12 -storetype PKCS12 -alias badminton-stable -keyalg RSA -keysize 3072 -validity 10000`

   Choose a strong, unique keystore password. For PKCS12, the key password can be the same as the keystore password.

2. Back up **Badminton-stable.p12** and its alias/passwords securely **outside GitHub**. Losing the key prevents future upgrades signed with the same certificate.
3. Encode the binary keystore to one continuous Base64 string.

   Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\path\\Badminton-stable.p12"))`

   macOS: `base64 -i Badminton-stable.p12 | tr -d '\n'`

   Linux: `base64 -w 0 Badminton-stable.p12`

4. In your GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**. Create **four** repository secrets:

   - `BADMINTON_SIGNING_KEYSTORE_B64`: complete Base64 keystore string
   - `BADMINTON_KEYSTORE_PASSWORD`: keystore password
   - `BADMINTON_KEY_ALIAS`: for example `badminton-stable`
   - `BADMINTON_KEY_PASSWORD`: private-key password

5. Open **Actions → Build Badminton Premium v24 STABLE (private signing key) → Run workflow**. Successful runs produce an artifact named `Badminton-Premium-v24-STABLE-signed`.
6. The release workflow verifies the APK signature and checks its SHA-256 certificate fingerprint against the pinned repository variable `BADMINTON_EXPECTED_CERT_SHA256`. Future versions must reuse the same package ID and signing key. The Android version code is assigned from the stable workflow run number.
7. For manual setup, create the repository variable `BADMINTON_EXPECTED_CERT_SHA256` with the lowercase, 64-character certificate SHA-256 fingerprint (no colons). The Windows script handles this automatically. A mismatched or missing fingerprint **blocks APK upload**.

**Security:** The workflow fails when required secrets are absent; it never checks a signing key or password into the public repository. The separate `test-v24-release-signing.yml` uses a throwaway key **only to validate the build configuration** and never distributes that APK.

## Current source hardening
- The 13 Nov 2026 v24 Preview source adds checksum/readback verification and a read-only recovery rehearsal. Existing confirmed November financial balances remain untouched.
- Do not confuse **verification** with **restoration**. Importing into a populated ledger stays blocked by design.
- The first v24 Stable installation remains a separate application from v23.2 and must import a verified JSON backup. To upgrade subsequent stable versions without reimporting, keep the same release application ID and signing certificate.
- GitHub's normal debug APK cannot be upgraded in place with a new debug signing certificate; do not uninstall an app until an externally saved verified backup has been checked.
- Folder providers such as cloud-synced Documents locations may upload the JSON according to their own settings. The JSON is not encrypted; select a trusted private folder.

## Limitations / QA

- GitHub compilation and accounting regression tests do **not** replace installation and restore testing on an Android phone.
- The stable signed APK has **not** been produced until the four GitHub secrets and the expected certificate fingerprint are configured and the stable workflow runs successfully.
- The first v24 Stable installation needs **one verified JSON import** from v23.2. Subsequent v24 Stable releases should install over it with matching signing key and increasing versionCode.
- The backup system makes best-effort snapshots and reports errors; it does not roll back successful payments if a backup folder is unavailable. Check the backup status periodically.
- Do not uninstall the old version until a verified external backup and the target app's closing balances have been checked.

This repository contains source code only, not actual account exports, signing keys, or personal member payments.

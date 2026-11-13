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
- **Restore / import verified JSON** is available only when the destination app database is empty. Import is blocked for a nonempty ledger; no automatic destructive replacement takes place.
- The JSON files contain member financial records in **unencrypted plain text**. Store them privately. The app does not upload them on its own; a selected cloud-synced Documents provider may sync copies according to that provider's settings.

## One-time stable signing key setup (repo owner)

Because Android checks both **application ID** and **signing certificate** during an upgrade, a v23.2 debug APK and a future stable APK cannot safely be treated as interchangeable. v24 Stable has its own app ID, with the explicit goal of **one import, then in-place upgrades for all subsequent stable versions**.

1. On a trusted PC with JDK installed, create an Android signing key. Example command:

   `keytool -genkeypair -v -keystore Badminton-stable.p12 -storetype PKCS12 -alias badminton-stable -keyalg RSA -keysize 3072 -validity 10000`

   Choose a strong, unique keystore password. For PKCS12, the key password can be the same as the keystore password.

2. Back up **Badminton-stable.p12** and its alias/passwords securely **outside GitHub**. Losing the key prevents future upgrades signed with the same certificate.
3. Encode the binary keystore to one continuous Base64 string.

   Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\path\\Badminton-stable.p12"))`

   macOS: `base64 -i Badminton-stable.p12 | tr -d '\\n'`

   Linux: `base64 -w 0 Badminton-stable.p12`

4. In your GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**. Create **four** repository secrets:

   - `BADMINTON_SIGNING_KEYSTORE_B64`: complete Base64 keystore string
   - `BADMINTON_KEYSTORE_PASSWORD`: keystore password
   - `BADMINTON_KEY_ALIAS`: for example `badminton-stable`
   - `BADMINTON_KEY_PASSWORD`: private-key password

5. Open **Actions → Build Badminton Premium v24 STABLE (private signing key) → Run workflow**. Successful runs produce an artifact named `Badminton-Premium-v24-STABLE-signed`.
6. The release workflow verifies the APK signature, and assigns an increasing Android `versionCode`. Future stable versions must reuse the same package ID and signing key.

**Security:** The workflow fails when required secrets are absent; it never checks a signing key or password into the public repository. The separate `test-v24-release-signing.yml` uses a throwaway key **only to validate the build configuration** and never distributes that APK.

## Limitations / QA

- GitHub compilation and accounting regression tests do **not** replace installation and restore testing on an Android phone.
- The stable signed APK has **not** been produced until the four GitHub secrets are configured and the stable workflow runs successfully.
- The first v24 Stable installation needs **one verified JSON import** from v23.2. Subsequent v24 Stable releases should install over it with matching signing key and increasing versionCode.
- The backup system makes best-effort snapshots and reports errors; it does not roll back successful payments if a backup folder is unavailable. Check the backup status periodically.
- Do not uninstall the old version until a verified external backup and the target app's closing balances have been checked.

This repository contains source code only, not actual account exports, signing keys, or personal member payments.

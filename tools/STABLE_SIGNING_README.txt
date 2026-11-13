BADMINTON PREMIUM v24 — ONE-TIME STABLE APK SIGNING
Windows setup pack (source code only; NO private signing key)

WHY THIS IS NECESSARY
Future Android APK updates must have the same app ID and signing certificate.
The new v24 STABLE app is separate from the current working v23.2 app.
After ONE successful verified-JSON import into the FIRST stable install, future
v24 stable builds can update that same app without repeating the import.

BEFORE STARTING
- Use a trusted Windows computer with internet access.
- Install GitHub CLI: https://cli.github.com/
- Install Android Studio or a Java 17+ JDK (for keytool).
- Open your own repository: https://github.com/vicpurnia/Badminton-club
- Keep v23.2 installed on your Android phone.
- Export v23.2 -> Reports -> Verified account export JSON.

ONE-TIME SETUP
1. Extract this ZIP to an ordinary folder on your Windows computer.
2. Double-click START_HERE_STABLE_SIGNING.bat.
3. Sign into the GitHub account that controls vicpurnia/Badminton-club.
4. Type CREATE only if you are setting up a NEW signing certificate.
5. Enter and confirm a strong permanent password (at least 14 characters).
6. The script creates a private Android signing key LOCALLY, prints its public
   SHA-256 certificate fingerprint, and waits.
7. BACK UP the .p12 file to an offline drive and save its password in a
   password manager. Do not email them or paste them into a chat.
8. Type BACKED UP. The script securely configures four GitHub Actions
   repository secrets and pins the certificate fingerprint.
9. Type BUILD if you want to trigger the stable APK workflow immediately.
10. On GitHub -> Actions -> Build Badminton Premium v24 STABLE, open the
    successful run and download the signed APK artifact.

PRIVATE KEY LOCATION
%LOCALAPPDATA%\Badminton_Stable_Signing_Keep_Private\badminton-stable.p12
Store an OFFLINE copy securely. The signing password is NOT saved by the script.
If the key is lost, DO NOT create a new one and overwrite the original GitHub
secrets: future Android in-place upgrades could become impossible.

INSTALLING THE FIRST STABLE VERSION
- Do not uninstall v23.2.
- Install the stable APK as a separate app.
- Import the verified JSON from the working app.
- Check Kanishk's February +1 donated pack, the historical Rs 1,000
  reconciliation flag and the Nov 2026 Rs 4,350 credit.
- Verify that backups work before retiring the older app.

NEXT STABLE UPGRADES
- Run the SAME stable GitHub workflow with the SAME pinned certificate.
- Do not generate a second signing key.
- Android can upgrade the app in-place when app ID, signing certificate,
  and version code requirements are satisfied.

SECURITY
This ZIP contains ONLY a batch launcher, PowerShell source and instructions.
It does not contain any keystore, financial data or GitHub tokens.
Never upload private signing material into the public repository.

Detailed guide:
https://github.com/vicpurnia/Badminton-club/blob/main/V24_BACKUP_AND_STABLE_SIGNING_GUIDE.md

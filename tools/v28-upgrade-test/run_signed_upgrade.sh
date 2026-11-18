#!/usr/bin/env bash
# Ephemeral GitHub Actions Android emulator ONLY. No actual member data.
# Uses owner-signed v25.9.1 and owner-signed v28.1, never uninstalls or clears storage.
set -euo pipefail
PKG=com.vinayak.badminton.premiumv24
TEST_PKG="${PKG}.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
CLASS="com.vinayak.badminton.nativepreview.V26SignedUpgradePersistenceTest"
OLD="$RUNNER_TEMP/v25-signed/app-release.apk"
NEW="app/build/outputs/apk/release/app-release.apk"
TEST="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
EVIDENCE="$RUNNER_TEMP/v28-signed-upgrade-evidence"
mkdir -p "$EVIDENCE"
test -s "$OLD" && test -s "$NEW" && test -s "$TEST"
AAPT="$ANDROID_HOME/build-tools/36.0.0/aapt"
export OLD NEW AAPT
python3 - <<'PY'
import os,re,subprocess
def info(file):
    s=subprocess.check_output([os.environ['AAPT'],'dump','badging',file],text=True)
    m=re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",s)
    assert m, "Cannot parse Android package"
    assert m.group(1)=="com.vinayak.badminton.premiumv24", "Package ID changed"
    return int(m.group(2)),m.group(3)
old=info(os.environ['OLD'])
new=info(os.environ['NEW'])
assert old[0]<new[0], f"VersionCode did not increase: {old} -> {new}"
assert old[1].startswith("25.9.1."), "Selected baseline is not the signed v25.9.1 stable APK"
assert new[1].startswith("28.1-rc-test."), "Selected APK is not v28.1 TEST candidate"
print("Verified SAME package; ascending owner-signed version: "+str(old)+" -> "+str(new))
PY

adb wait-for-device
adb logcat -c || true
echo "1. Install authentic owner-signed v25.9.1 stable APK in a clean emulator"
adb install "$OLD"
adb install "$TEST"

instrument(){
 local method="$1" evidence="$2"
 adb shell am instrument -w -r -e class "$CLASS#$method" "$TEST_PKG/$RUNNER" \
  | tee "$EVIDENCE/$evidence"
 grep -q "INSTRUMENTATION_CODE: -1" "$EVIDENCE/$evidence" || {
  echo "Instrumentation did not report a passing test method";exit 1;
 }
 if grep -Eq "FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed" "$EVIDENCE/$evidence";then
  echo "Synthetic test reported failure";exit 1;
 fi
}

echo "2. Seed one synthetic member, actual-fee cash payment, shuttle purchase, shuttle usage"
instrument seedOnSignedV25 seed-v25.txt
adb shell am force-stop "$PKG" || true

echo "3. Upgrade SAME package in place. Do not uninstall or clear data."
adb install -r "$NEW"
adb shell pm path "$PKG" | tee "$EVIDENCE/package-after-update.txt"

echo "4. Open Room v1 after upgraded app, verify exact original synthetic records and counts"
instrument verifyAfterSignedV26Upgrade verify-v26.txt
echo "4b. Android 35: verify real PDF rendering, FileProvider, receipt ZIP, AES staging and unchanged ledger"
EXTRA_CLASS="com.vinayak.badminton.nativepreview.V27ArtifactsAfterUpgradeTest#verifyReadOnlyArtifactsAfterSignedUpgrade"
adb shell am instrument -w -r -e class "$EXTRA_CLASS" "$TEST_PKG/$RUNNER" \
 | tee "$EVIDENCE/v27-artifacts-after-upgrade.txt"
grep -q "INSTRUMENTATION_CODE: -1" "$EVIDENCE/v27-artifacts-after-upgrade.txt" || {
 echo "v27 Android artifact/recovery test did not pass";exit 1;
}
# AndroidJUnitRunner captures System.out in Android logcat, not in the
# 'am instrument -w' result stream. Require the actual JUnit test name,
# completed status and one passing test, rather than non-streamed print markers.
grep -q 'INSTRUMENTATION_STATUS: test=verifyReadOnlyArtifactsAfterSignedUpgrade' \
 "$EVIDENCE/v27-artifacts-after-upgrade.txt"
grep -q 'INSTRUMENTATION_STATUS_CODE: 0' \
 "$EVIDENCE/v27-artifacts-after-upgrade.txt"
grep -q 'OK (1 test)' "$EVIDENCE/v27-artifacts-after-upgrade.txt"
if grep -Eq "FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed" \
 "$EVIDENCE/v27-artifacts-after-upgrade.txt"; then
 echo "v27 Android output test failed";exit 1;
fi
echo "4c. Android 35: v28 photos, per-ID local portrait and in-memory recovery audit"
PHOTO_CLASS="com.vinayak.badminton.nativepreview.V28PhotosAndReadinessTest#verifyPhotoAndRecoveryAuditAfterSignedUpgrade"
adb shell am instrument -w -r -e class "$PHOTO_CLASS" "$TEST_PKG/$RUNNER" \
 | tee "$EVIDENCE/v28-member-photo-release-audit.txt"
grep -q "INSTRUMENTATION_CODE: -1" "$EVIDENCE/v28-member-photo-release-audit.txt"
grep -q 'INSTRUMENTATION_STATUS: test=verifyPhotoAndRecoveryAuditAfterSignedUpgrade' \
 "$EVIDENCE/v28-member-photo-release-audit.txt"
grep -q 'INSTRUMENTATION_STATUS_CODE: 0' "$EVIDENCE/v28-member-photo-release-audit.txt"
grep -q 'OK (1 test)' "$EVIDENCE/v28-member-photo-release-audit.txt"
if grep -Eq "FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed" \
 "$EVIDENCE/v28-member-photo-release-audit.txt"; then
 echo "v28 Android portrait/release-audit test failed"; exit 1
fi
adb shell am force-stop "$PKG" || true
# Launch the target activity directly. Monkey/launcher launch can provoke
# a Pixel Launcher ANR in overloaded headless CI even when Room upgrade passes.
adb shell am start -W -n "$PKG/com.vinayak.badminton.nativepreview.MainActivity"
sleep 3
adb shell pidof "$PKG" >/dev/null
echo "5. Verify real Android navigation across populated synthetic ledger after upgrade"
python3 tools/v27-upgrade-test/check_populated_navigation.py
# All four instrumentation invocations above are mandatory pass/fail. The emulator
# can transiently go offline when taking screenshots at shutdown; diagnostics are
# best-effort and must not erase two independently passing persistence checks.
if adb logcat -d -v brief > "$EVIDENCE/post-upgrade-logcat.txt" 2>/dev/null;then
 if grep -A 8 'FATAL EXCEPTION' "$EVIDENCE/post-upgrade-logcat.txt" | grep -q "$PKG";then
  echo "Android crash after in-place signed upgrade";exit 1;
 fi
else
 echo "::warning::Emulator logcat unavailable after all persistence checks passed."
 rm -f "$EVIDENCE/post-upgrade-logcat.txt"
fi
if ! adb exec-out screencap -p > "$EVIDENCE/v28-synthetic-post-upgrade.png" 2>/dev/null;then
 rm -f "$EVIDENCE/v28-synthetic-post-upgrade.png"
 echo "::warning::Emulator screenshot unavailable; data-preservation checks passed."
fi
echo "SIGNED v25.9.1 -> v28.1, same-package in-place synthetic Room persistence PASSED."
echo "Not a production ledger migration, actual real-phone testing, or restoration acceptance."

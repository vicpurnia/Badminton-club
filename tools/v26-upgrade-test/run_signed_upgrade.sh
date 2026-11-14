#!/usr/bin/env bash
# Ephemeral GitHub Actions Android emulator ONLY. No actual member data.
# Uses owner-signed v25.9 and owner-signed v26.9, never uninstalls or clears storage.
set -euo pipefail
PKG=com.vinayak.badminton.premiumv24
TEST_PKG="${PKG}.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
CLASS="com.vinayak.badminton.nativepreview.V26SignedUpgradePersistenceTest"
OLD="$RUNNER_TEMP/v25-signed/app-release.apk"
NEW="app/build/outputs/apk/release/app-release.apk"
TEST="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
EVIDENCE="$RUNNER_TEMP/v26-signed-upgrade-evidence"
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
assert old[1].startswith("25.9."), "Selected historical APK is not v25.9 stable"
assert new[1].startswith("26.9-rc-test."), "Selected APK is not v26.9 TEST candidate"
print("Verified SAME package; ascending owner-signed version: "+str(old)+" -> "+str(new))
PY

adb wait-for-device
adb logcat -c || true
echo "1. Install authentic owner-signed v25.9 stable APK in a clean emulator"
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
adb shell am force-stop "$PKG" || true
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
sleep 3
adb shell pidof "$PKG" >/dev/null
if adb logcat -d -v brief | grep -A 8 'FATAL EXCEPTION' | grep -q "$PKG";then
 echo "Android crash after in-place signed upgrade";exit 1;
fi
adb exec-out screencap -p > "$EVIDENCE/v26-synthetic-post-upgrade.png"
echo "SIGNED v25.9 -> v26.9, same-package in-place synthetic Room persistence PASSED."
echo "Not a production ledger migration, actual real-phone testing, or restoration acceptance."

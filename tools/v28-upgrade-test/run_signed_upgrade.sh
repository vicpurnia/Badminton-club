#!/usr/bin/env bash
# Ephemeral GitHub Actions Android emulator ONLY. No actual member data.
# Uses owner-signed v25.9.1 and owner-signed v28.3, never uninstalls or clears storage.
set -euo pipefail
PKG=com.vinayak.badminton.premiumv24
TEST_PKG="${PKG}.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
CLASS="com.vinayak.badminton.nativepreview.V26SignedUpgradePersistenceTest"
OLD="$RUNNER_TEMP/v25-signed/app-release.apk"
NEW="app/build/outputs/apk/release/app-release.apk"
TEST="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
EVIDENCE="$RUNNER_TEMP/v28-signed-upgrade-evidence"
DISPLAY_OVERRIDDEN=0
ORIGINAL_SIZE_OVERRIDE=""
ORIGINAL_DENSITY_OVERRIDE=""
ORIGINAL_FONT_SCALE="null"
ORIGINAL_HARD_KEYBOARD_IME="null"
mkdir -p "$EVIDENCE"
restore_test_display(){
 if [ "$DISPLAY_OVERRIDDEN" -eq 0 ]; then return; fi
 timeout 10s adb shell wm size "${ORIGINAL_SIZE_OVERRIDE:-reset}" || return
 timeout 10s adb shell wm density "${ORIGINAL_DENSITY_OVERRIDE:-reset}" || return
 if [ "$ORIGINAL_FONT_SCALE" = "null" ]; then
  timeout 10s adb shell settings delete system font_scale || return
 else
  timeout 10s adb shell settings put system font_scale "$ORIGINAL_FONT_SCALE" || return
 fi
 if [ "$ORIGINAL_HARD_KEYBOARD_IME" = "null" ]; then
  timeout 10s adb shell settings delete secure show_ime_with_hard_keyboard || return
 else
  timeout 10s adb shell settings put secure show_ime_with_hard_keyboard "$ORIGINAL_HARD_KEYBOARD_IME" || return
 fi
 DISPLAY_OVERRIDDEN=0
}
collect_ui_evidence(){
 adb pull "/sdcard/Android/data/$PKG/files/photo-ui-evidence/." \
  "$EVIDENCE/" >/dev/null 2>&1 || true
 mkdir -p "$EVIDENCE/premium-ui"
 adb pull "/sdcard/Android/data/$PKG/files/premium-ui-evidence/." \
  "$EVIDENCE/premium-ui/" >/dev/null 2>&1 || true
 # Preserve Android lifecycle/error diagnostics even when instrumentation fails
 # before the normal successful-end log capture. This never changes its verdict.
 if [ ! -s "$EVIDENCE/post-upgrade-logcat.txt" ]; then
  if ! timeout 15s adb logcat -d -v brief > "$EVIDENCE/post-upgrade-logcat.txt" 2>/dev/null; then
   rm -f "$EVIDENCE/post-upgrade-logcat.txt"
  fi
 fi
}
finish_test_run(){
 local result=$?
 collect_ui_evidence || true
 restore_test_display || echo "::warning::Could not restore the ephemeral emulator display settings."
 exit "$result"
}
trap finish_test_run EXIT
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
assert new[1].startswith("28.3-rc-test."), "Selected APK is not v28.3 TEST candidate"
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
echo "4d. Android 35: direct member-list photo action, picker cancellation, replacement and removal"
PHOTO_UI_CLASS="com.vinayak.badminton.nativepreview.V28MemberListPhotoUiTest#memberListPhotoActionsPreserveIdentityAndLedger"
adb shell am instrument -w -r -e class "$PHOTO_UI_CLASS" "$TEST_PKG/$RUNNER" \
 | tee "$EVIDENCE/v28-member-list-photo-ui.txt"
grep -q "INSTRUMENTATION_CODE: -1" "$EVIDENCE/v28-member-list-photo-ui.txt"
grep -q 'INSTRUMENTATION_STATUS: test=memberListPhotoActionsPreserveIdentityAndLedger' \
 "$EVIDENCE/v28-member-list-photo-ui.txt"
grep -q 'INSTRUMENTATION_STATUS_CODE: 0' "$EVIDENCE/v28-member-list-photo-ui.txt"
grep -q 'OK (1 test)' "$EVIDENCE/v28-member-list-photo-ui.txt"
if grep -Eq "FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed" \
 "$EVIDENCE/v28-member-list-photo-ui.txt"; then
 echo "v28 direct member-list photo UI test failed"; exit 1
fi
echo "4e. Android 35: premium screens, visible actions and cancelled forms preserve financial records"
PREMIUM_UI_CLASS="com.vinayak.badminton.nativepreview.V283WorkflowUiTest#premiumScreensAndActionsRemainUsable"
adb shell am instrument -w -r -e class "$PREMIUM_UI_CLASS" "$TEST_PKG/$RUNNER" \
 | tee "$EVIDENCE/v283-premium-workflow-ui.txt"
grep -q "INSTRUMENTATION_CODE: -1" "$EVIDENCE/v283-premium-workflow-ui.txt"
grep -q 'INSTRUMENTATION_STATUS: test=premiumScreensAndActionsRemainUsable' \
 "$EVIDENCE/v283-premium-workflow-ui.txt"
grep -q 'INSTRUMENTATION_STATUS_CODE: 0' "$EVIDENCE/v283-premium-workflow-ui.txt"
grep -q 'OK (1 test)' "$EVIDENCE/v283-premium-workflow-ui.txt"
if grep -Eq "FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed" \
 "$EVIDENCE/v283-premium-workflow-ui.txt"; then
 echo "v28.3 premium workflow UI test failed"; exit 1
fi
adb shell am force-stop "$PKG" || true
# Launch the target activity directly. Monkey/launcher launch can provoke
# a Pixel Launcher ANR in overloaded headless CI even when Room upgrade passes.
adb shell am start -W -n "$PKG/com.vinayak.badminton.nativepreview.MainActivity"
sleep 3
adb shell pidof "$PKG" >/dev/null
echo "5. Verify real Android navigation across populated synthetic ledger after upgrade"
python3 tools/v27-upgrade-test/check_populated_navigation.py
instrument_premium_case(){
 local target_class="$1" method="$2" evidence="$3"
 adb shell am instrument -w -r -e class "$target_class#$method" "$TEST_PKG/$RUNNER" \
  | tee "$EVIDENCE/$evidence"
 grep -q 'INSTRUMENTATION_CODE: -1' "$EVIDENCE/$evidence"
 grep -q "INSTRUMENTATION_STATUS: test=$method" "$EVIDENCE/$evidence"
 grep -q 'INSTRUMENTATION_STATUS_CODE: 0' "$EVIDENCE/$evidence"
 grep -q 'OK (1 test)' "$EVIDENCE/$evidence"
 if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=|Process crashed' "$EVIDENCE/$evidence"; then
  echo "Focused premium UI test failed: $method"; exit 1
 fi
}
echo "6. Verify accessible month actions and saved navigation/search across activity recreation"
ACCESSIBILITY_CLASS="com.vinayak.badminton.nativepreview.V283AccessibilityStateTest"
instrument_premium_case "$ACCESSIBILITY_CLASS" accessibilityAndScreenStateSurviveRecreation \
 v283-accessibility-screen-state.txt
echo "7. Verify queued photo save and failure across activity recreation"
instrument_premium_case "com.vinayak.badminton.nativepreview.V283PhotoLifecycleTest" \
 queuedPhotoSaveAndFailureSurviveRecreation v283-photo-lifecycle.txt
echo "8. Verify 320dp width and 200% fonts, including profile fields above the software keyboard"
ORIGINAL_SIZE_OVERRIDE=$(adb shell wm size | sed -n 's/^Override size: //p' | tr -d '\r')
ORIGINAL_DENSITY_OVERRIDE=$(adb shell wm density | sed -n 's/^Override density: //p' | tr -d '\r')
ORIGINAL_FONT_SCALE=$(adb shell settings get system font_scale | tr -d '\r')
ORIGINAL_HARD_KEYBOARD_IME=$(adb shell settings get secure show_ime_with_hard_keyboard | tr -d '\r')
DISPLAY_OVERRIDDEN=1
adb shell am force-stop "$PKG"
adb shell wm size 640x1280
adb shell wm density 320
adb shell settings put system font_scale 2.0
adb shell settings put secure show_ime_with_hard_keyboard 1
adb shell am force-stop "$PKG"
instrument_premium_case "$ACCESSIBILITY_CLASS" largeTextOnNarrowScreenKeepsActionsReachable \
 v283-narrow-large-text.txt
restore_test_display
adb shell am force-stop "$PKG"
echo "9. Verify retained form errors, Cancel/Back, and exactly-once synthetic metadata retries"
# This case intentionally commits two non-financial metadata events. Keep it
# after every original-fixture assertion and after restoring the display.
instrument_premium_case "com.vinayak.badminton.nativepreview.V283FormWorkflowTest" \
 rejectedFormsRetainValuesAndSafeRetriesCommitOnce v283-retained-form-workflows.txt
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.vinayak.badminton.nativepreview.MainActivity"
# All instrumentation invocations above are mandatory pass/fail. The emulator
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
echo "SIGNED v25.9.1 -> v28.3, same-package in-place synthetic Room persistence PASSED."
echo "Not a production ledger migration, actual real-phone testing, or restoration acceptance."

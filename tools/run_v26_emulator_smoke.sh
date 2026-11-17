#!/usr/bin/env bash
# Native v26 empty-ledger Android navigation smoke; NO production ledger imported.
set -euo pipefail
PKG="com.vinayak.badminton.premiumv24"
APK="app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
adb wait-for-device
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell input keyevent KEYCODE_HOME || true
adb logcat -c || true
adb install -r "$APK"
adb shell am force-stop "$PKG" || true
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
sleep 5
adb shell pidof "$PKG" >/dev/null

# UiAutomator drives the actual app's bottom navigation, not image coordinates
# guessed for a particular emulator screen size.
python3 - <<'PY'
import re, subprocess, time, xml.etree.ElementTree as ET

def run(*args):
    return subprocess.check_output(["adb", *args], text=True, stderr=subprocess.STDOUT)

def screen():
    for attempt in range(3):
        try:
            run("shell","uiautomator","dump","/sdcard/v26-ui.xml")
            run("pull","/sdcard/v26-ui.xml","/tmp/v26-ui.xml")
            return ET.parse("/tmp/v26-ui.xml").getroot()
        except Exception:
            time.sleep(2)
    raise RuntimeError("Android UiAutomator could not inspect the v26 screen")

def labels(tree):
    return {n.attrib.get("text","") for n in tree.iter()}

def press(label, expected):
    tree=screen()
    choices=[n for n in tree.iter() if n.attrib.get("text")==label]
    if not choices:
        raise AssertionError(f"Navigation {label!r} missing in device UI; visible labels={sorted(labels(tree))[:30]}")
    bounds=choices[-1].attrib.get("bounds","")
    found=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",bounds)
    if not found:
        raise AssertionError(f"Unusable Android navigation bounds for {label}: {bounds!r}")
    x1,y1,x2,y2=map(int,found.groups())
    run("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
    time.sleep(1.8)
    after=screen()
    if expected not in labels(after):
        raise AssertionError(f"After {label}, screen lacks expected heading {expected!r}")
    print(f"Android emulator navigation: {label} -> {expected} PASS")

initial=screen()
if "Overview" not in labels(initial):
    raise AssertionError("Home/Overview not visible after cold launch")
for tab, expected in [
    ("Members","Actual payments, assessed dues and immutable member histories"),
    ("Shuttles","Shuttle desk"),
    ("Collect","Collect fees"),
    ("Reports","Your club finances, beautifully presented"),
    ("Home","Overview"),
]:
    press(tab,expected)
print("v26.9 native empty-ledger launch, five-tab navigation and return PASS")
PY

adb shell pidof "$PKG" >/dev/null
if adb logcat -d -v brief | grep -A 8 "FATAL EXCEPTION" | grep -q "$PKG"; then
  echo "Android FATAL EXCEPTION for $PKG"
  exit 1
fi
mkdir -p "$RUNNER_TEMP/v26-emulator-evidence"
adb exec-out screencap -p > "$RUNNER_TEMP/v26-emulator-evidence/final-home.png"
adb logcat -d -v brief | tail -n 500 > "$RUNNER_TEMP/v26-emulator-evidence/logcat-tail.txt"
echo "Emulator only: no real phone, signed upgrade, receipt sharing or ledger restoration tested."

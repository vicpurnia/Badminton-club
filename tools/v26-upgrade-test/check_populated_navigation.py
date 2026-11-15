#!/usr/bin/env python3
"""UIAutomator validation of the *upgraded, populated synthetic ledger* app.

The surrounding signed-upgrade workflow installs the genuine owner-signed APK.
This script never installs, uninstalls, clears data or imports a real ledger.
"""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG="com.vinayak.badminton.premiumv24"
EVIDENCE=Path(os.environ.get("RUNNER_TEMP","/tmp"))/"v26-signed-upgrade-evidence"
EVIDENCE.mkdir(parents=True,exist_ok=True)

def adb(*args):
    return subprocess.check_output(["adb",*args],text=True,stderr=subprocess.STDOUT)

def dump():
    for attempt in range(4):
        try:
            adb("shell","uiautomator","dump","/sdcard/v26-upgrade-ui.xml")
            adb("pull","/sdcard/v26-upgrade-ui.xml",str(EVIDENCE/"ui.xml"))
            return ET.parse(EVIDENCE/"ui.xml").getroot()
        except Exception:
            time.sleep(2)
    raise AssertionError("Upgraded Android app UI could not be inspected")

def labels(tree):
    return {e.attrib.get("text","") for e in tree.iter() if e.attrib.get("text","")}

def tap(tab,expected):
    tree=dump()
    found=[e for e in tree.iter() if e.attrib.get("text","")==tab]
    assert found, f"Missing upgraded tab: {tab}; visible labels: {sorted(labels(tree))[:30]}"
    match=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",found[-1].attrib.get("bounds",""))
    assert match, f"Cannot use Android bounds for {tab}"
    x1,y1,x2,y2=map(int,match.groups())
    adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
    time.sleep(1.8)
    after=labels(dump())
    assert expected in after, f"After {tab}, missing heading {expected!r}; visible: {sorted(after)[:35]}"
    assert adb("shell","pidof",PKG).strip(),f"App process exited after {tab}"
    print(f"POPULATED-SYNTHETIC-NAV PASS: {tab} -> {expected}")

home=set()
for attempt in range(7):
    home=labels(dump())
    if "Overview" in home:
        break
    # A signed APK update may briefly leave Android's launcher or instrumentation
    # on top of the activity. Explicit launch is harmless to the persisted ledger.
    if attempt==1:
        adb("shell","am","start","-W","-n",
            PKG+"/com.vinayak.badminton.nativepreview.MainActivity")
    time.sleep(2)
else:
    raise AssertionError(
        "Signed upgraded Home screen unavailable after retries; UI labels="+
        repr(sorted(home)[:60]))
print("SIGNED-UPGRADE POPULATED HOME visible after launch")
for tab,expected in [
    ("Members","Actual payments, assessed dues and immutable member histories"),
    ("Shuttles","Shuttle desk"),
    ("Collect","Collect fees"),
    ("Reports","Your club finances, beautifully presented"),
    ("Home","Overview"),
]:
    tap(tab,expected)
print("SIGNED-v25-TO-v26 populated synthetic ledger: five-tab UI navigation PASSED")

# Real Android accessibility-level verification of the v25.9.1 navigation hotfix.
# This runs against the signed, in-place-upgraded SYNTHETIC ledger, not real accounts.
def tap_home_card(prefix,expected):
    size=adb("shell","wm","size")
    match=re.search(r"(\\d+)x(\\d+)",size)
    assert match,f"Cannot read emulator dimensions: {size!r}"
    width,height=map(int,match.groups())
    target=None
    for attempt in range(6):
        tree=dump()
        candidates=[node for node in tree.iter()
            if node.attrib.get("content-desc","").startswith(prefix+": ")]
        if candidates:
            target=candidates[-1]
            break
        adb("shell","input","swipe",str(width//2),str(int(height*.78)),
            str(width//2),str(int(height*.32)),"300")
        time.sleep(.6)
    assert target is not None, f"{prefix} Home shortcut missing or not accessible"
    assert target.attrib.get("clickable")=="true",f"{prefix} card is not clickable"
    assert target.attrib.get("focusable")=="true",f"{prefix} card cannot receive focus"
    bounds=target.attrib.get("bounds","")
    found=re.fullmatch(r"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]",bounds)
    assert found,f"Invalid Home shortcut bounds: {bounds!r}"
    x1,y1,x2,y2=map(int,found.groups())
    adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
    for attempt in range(7):
        time.sleep(.65)
        if expected in labels(dump()):
            print(f"SIGNED-SYNTHETIC SHORTCUT PASS: {prefix} card -> {expected}")
            assert adb("shell","pidof",PKG).strip(),"App exited after Home card tap"
            return
    raise AssertionError(f"{prefix} Home shortcut did not navigate to {expected!r}")

tap_home_card("Players","Actual payments, assessed dues and immutable member histories")
tap("Home","Overview")
tap_home_card("Receipts","Monthly receipts")
print("SIGNED-v25-TO-v26: Home Players and Receipts full-card navigation PASSED")

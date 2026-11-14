#!/usr/bin/env python3
"""Fail-closed Android package identity / upgrade-code check for v26 TEST artifacts.

This is a packaging gate only. It does not certify any on-device upgrade.
"""
import os
import re
import subprocess
import sys
from pathlib import Path


def check(apk: str, version_prefix: str) -> None:
    source = Path(apk)
    if not source.is_file() or source.stat().st_size < 10_000:
        raise SystemExit("v26 APK does not exist or is abnormally small")
    android = Path(os.environ["ANDROID_HOME"])
    binary = android / "build-tools" / "36.0.0" / "aapt"
    if not binary.is_file():
        raise SystemExit("Android aapt build-tools 36.0.0 unavailable")
    output = subprocess.check_output(
        [str(binary), "dump", "badging", str(source)],
        text=True, stderr=subprocess.STDOUT,
    )
    found = re.search(
        r"package:\s+name='([^']+)'\s+versionCode='(\d+)'\s+versionName='([^']+)'",
        output,
    )
    if found is None:
        raise SystemExit("Cannot parse Android package identity from APK")
    app_id, code, name = found.group(1), int(found.group(2)), found.group(3)
    if app_id != "premiumv24":
        raise SystemExit("ABORT: original stable Android package ID changed")
    if not (27_000 <= code < 100_000):
        raise SystemExit("ABORT: candidate versionCode cannot safely supersede v25 stable")
    if not name.startswith(version_prefix):
        raise SystemExit("ABORT: the APK is not labeled as a v26 development/test build")
    print(f"Verified candidate identity: {app_id} / code {code} / {name}")
    print("Source compatibility gate only — no physical-device acceptance has been performed.")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: verify_v26_apk_identity.py <APK> <26.9-dev. or 26.9-rc-test.>")
    check(sys.argv[1], sys.argv[2])

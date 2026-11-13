#!/usr/bin/env bash
# Verify signature AND permanently pinned signer identity before distributing an APK.
# The fingerprint is a public SHA-256 certificate digest (64 hex chars).
set -euo pipefail
if [ "$#" -ne 2 ]; then
  echo "Usage: bash tools/check-stable-signature.sh APK EXPECTED_CERT_SHA256" >&2
  exit 2
fi
apk="$1"
expected="$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]:')"
if [[ ! "$expected" =~ ^[0-9a-f]{64}$ ]]; then
  echo "::error::Expected release certificate SHA-256 is missing or malformed. Do not publish this APK." >&2
  exit 1
fi
apksigner="${ANDROID_HOME:?ANDROID_HOME not set}/build-tools/36.0.0/apksigner"
if [ ! -x "$apksigner" ]; then
  echo "::error::Android apksigner is unavailable at $apksigner" >&2
  exit 1
fi
if [ ! -s "$apk" ]; then
  echo "::error::Release APK missing or empty: $apk" >&2
  exit 1
fi
details="$("$apksigner" verify --verbose --print-certs "$apk")"
actual="$(printf '%s\n' "$details" |
    sed -nE 's/^Signer #1 certificate SHA-256 digest: ([[:xdigit:]]{64}).*$/\1/p' |
    head -n 1 | tr '[:upper:]' '[:lower:]')"
if [ -z "$actual" ] || [ "$actual" != "$expected" ]; then
  echo "::error::Release signature does not match pinned certificate! Do NOT distribute or rotate automatically." >&2
  echo "Expected certificate fingerprint: $expected" >&2
  echo "Actual certificate fingerprint: ${actual:-unknown}" >&2
  exit 1
fi
echo "APK signature verified; certificate matches pinned SHA-256 $actual"

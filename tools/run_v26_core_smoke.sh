#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
WORK="${RUNNER_TEMP:-/tmp}/badminton-v26-smoke"
mkdir -p "$WORK/classes"
JAR="$WORK/json-20240303.jar"
curl --fail --location --silent --show-error --retry 3 \
  "https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar" -o "$JAR"
test "$(stat -c%s "$JAR")" -gt 50000
javac -encoding UTF-8 -cp "$JAR" -d "$WORK/classes" \
  src/premium/v26_9/CashCorrectionsV256.java \
  src/premium/v26_9/PremiumV26State.java \
  src/premium/v26_9/PremiumBackupCrypto.java \
  src/premium/v26_9/PremiumV26StockGuard.java \
  src/premium/v26_9/PremiumCashDateAudit.java \
  src/premium/v26_9/PremiumV26ClosingGuard.java \
  src/premium/v26_9/tests/V26CoreSmoke.java
java -ea -cp "$WORK/classes:$JAR" com.vinayak.badminton.nativepreview.V26CoreSmoke

#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
WORK="${RUNNER_TEMP:-/tmp}/badminton-v26-smoke"
mkdir -p "$WORK/classes"
JAR="$WORK/json-20240303.jar"
# A single Maven CDN can return HTTP 429 during concurrent Android CI.
# Prefer independent HTTPS mirrors but accept only the pinned, known 20240303 JAR.
# Never silently skip the financial/backup test suite if all mirrors fail.
EXPECTED_SHA256=3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed
RELATIVE=org/json/json/20240303/json-20240303.jar
downloaded=0
for base in \
  "https://repo1.maven.org/maven2" \
  "https://maven-central.storage-download.googleapis.com/maven2" \
  "https://repository-cdn.liferay.com/nexus/content/repositories/public" \
  "https://repo.maven.apache.org/maven2"; do
  rm -f "$JAR.partial"
  if curl --fail --location --silent --show-error --retry 2 \
      --retry-all-errors --retry-delay 2 --max-time 40 \
      "$base/$RELATIVE" -o "$JAR.partial"; then
    if test "$(stat -c%s "$JAR.partial")" -eq 78332 &&
       echo "$EXPECTED_SHA256  $JAR.partial" | sha256sum -c -; then
      mv "$JAR.partial" "$JAR"
      downloaded=1
      break
    fi
    echo "::warning::Maven mirror returned unexpected JAR content: $base"
  else
    echo "::warning::Maven mirror temporarily unavailable: $base"
  fi
done
if [ "$downloaded" -ne 1 ]; then
  echo "::error::Verified org.json Java dependency unavailable; accounting tests MUST run."
  exit 1
fi
javac -encoding UTF-8 -cp "$JAR" -d "$WORK/classes" \
  src/premium/v26_9/CashCorrectionsV256.java \
  src/premium/v26_9/PremiumV26State.java \
  src/premium/v26_9/PremiumBackupCrypto.java \
  src/premium/v26_9/PremiumV26StockGuard.java \
  src/premium/v26_9/PremiumCashDateAudit.java \
  src/premium/v26_9/PremiumV26ClosingGuard.java \
  src/premium/v26_9/PremiumV26PaymentOnce.java \
  src/premium/v26_9/PremiumV26BackupCompare.java \
  src/premium/v26_9/tests/V26CoreSmoke.java
java -ea -cp "$WORK/classes:$JAR" com.vinayak.badminton.nativepreview.V26CoreSmoke

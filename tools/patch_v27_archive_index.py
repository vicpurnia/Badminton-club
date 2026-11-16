#!/usr/bin/env python3
"""Overlay central-directory aware receipt ZIP validation after verified v27 base.

No original receipts, payments, database schema or stable source are altered.
"""
from pathlib import Path
app=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
target=app/"PremiumV27ReceiptArchive.java"
source=Path("src/premium/v27_9_1/PremiumV27ReceiptArchive.java").read_text(encoding="utf-8")
existing=target.read_text(encoding="utf-8")
if "ZipInputStream" not in existing or "promote(" not in existing:
 raise AssertionError("Original v27 verified archive integration missing")
for phrase in ("ZipFile index", "indexedCrc", "indexedSizes",
               "Receipt ZIP central directory CRC or entry size"):
 if phrase not in source:raise AssertionError("Hardened ZIP source missing "+phrase)
target.write_text(source,encoding="utf-8")
print("PASS v27 receipt archive central-directory + CRC hardening overlay")

#!/usr/bin/env python3
"""Release-blocking source checks for asynchronous native navigation (no real data)."""
from pathlib import Path
import re

source = Path("src/premium/v26_9/MainActivity.java").read_text(encoding="utf-8")
required = [
    "screenEpoch++;tab=name;",
    'currentScreen(screen,"Home",period)',
    'currentScreen(screen,"Collect",viewedMonth)',
    'currentScreen(screen,"Shuttles",selected)',
    'currentScreen(screen,"Members",viewedMonth)',
    'selectTab("Home");',
    'selectTab("Collect");',
    'selectTab("Shuttles");',
    "memberRenderGeneration",
]
for item in required:
    assert item in source, f"Native-screen stale-result guard missing: {item}"

calls = []
for number, line in enumerate(source.splitlines(), 1):
    if "private void selectTab(" in line:
        continue
    if "private void showHome(" in line or "private void showPayments(" in line or "private void showExpenses(" in line:
        continue
    for name in ["showHome", "showPayments", "showExpenses"]:
        if re.search(r"\b" + name + r"\s*\(\s*\)\s*;", line):
            calls.append(f"line {number}: {name}() invoked directly outside navigation")
if calls:
    raise AssertionError("Post-save navigation bypasses the selected tab epoch: " + "; ".join(calls))

# Background rendering must check that the selected screen/month still matches.
for marker, count in [
    ('currentScreen(screen,"Home",period)', 3),
    ('currentScreen(screen,"Collect",viewedMonth)', 1),
    ('currentScreen(screen,"Shuttles",selected)', 1),
    ('currentScreen(screen,"Members",viewedMonth)', 2),
]:
    observed = source.count(marker)
    assert observed >= count, f"Missing expected {marker}: {observed} < {count}"

# Guard remains a client-side rendering check; financial writes still use Room transactions.
assert source.count("database.runInTransaction(") >= 5
assert "requireOpenMonth(original.feeMonth)" in source
assert "PremiumV26StockGuard.requireEditableStockMonth(" in source
print("v26 screen epoch, post-save routing, month consistency and financial transaction guard: PASS")

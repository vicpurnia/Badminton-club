#!/usr/bin/env python3
"""v27.1 build-only additive member duplicate-name guard."""
from pathlib import Path
root=Path("app/src/main/java/com/vinayak/badminton/nativepreview")
source=root/"MainActivity.java"
s=source.read_text()
changes={
 r'String normal=n.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");':
 r'String normal=PremiumV27MemberNames.key(n);',
 r'effectiveName(other,profiles).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").equals(normal)':
 'PremiumV27MemberNames.key(effectiveName(other,profiles)).equals(normal)',
 r'String normalized=display.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");':
 r'String normalized=PremiumV27MemberNames.key(display);',
 r'String rival=effectiveName(other,profiles).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");':
 r'String rival=PremiumV27MemberNames.key(effectiveName(other,profiles));'
}
for old,new in changes.items():
 n=s.count(old)
 if n!=1: raise AssertionError(f"v27.1 expected exactly one match (got {n}): {old}")
 s=s.replace(old,new,1)
source.write_text(s)
(root/"PremiumV27MemberNames.java").write_text(Path("src/premium/v27_1/PremiumV27MemberNames.java").read_text())
assert (root/"PremiumV27MemberNames.java").is_file()
print("PASS v27.1 non-destructive member uniqueness overlay")

package com.vinayak.badminton.nativepreview;

import java.text.Normalizer;
import java.util.Locale;

/** v27.1: canonical duplicate detection without rewriting historical names. */
final class PremiumV27MemberNames {
 private PremiumV27MemberNames() {}
 static String key(String name) {
  if(name==null)throw new IllegalArgumentException("Member name is required");
  String normalized=Normalizer.normalize(name,Normalizer.Form.NFKC);
  normalized=normalized.replaceAll("[\\p{Cf}\\p{Cc}]", " ");
  normalized=normalized.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
  if(normalized.isEmpty())throw new IllegalArgumentException("Member name is required");
  return normalized;
 }
 static void requireUnique(String proposed, Iterable<String> existing) {
  String expected=key(proposed);
  if(existing==null)throw new IllegalArgumentException("Existing member names unavailable");
  for(String previous:existing)
   if(expected.equals(key(previous)))
    throw new IllegalArgumentException("Possible duplicate member name; original ID protected");
 }
}

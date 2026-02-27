package com.databricks.jsonnetjvm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for JsonnetStrings: codepoint-based string comparison. */
public class JsonnetStringsTest {

  @Test
  void equalStrings() {
    assertEquals(0, JsonnetStrings.compareByCodepoint("abc", "abc"));
    assertEquals(0, JsonnetStrings.compareByCodepoint("", ""));
  }

  @Test
  void emptyVsNonEmpty() {
    assertTrue(JsonnetStrings.compareByCodepoint("", "a") < 0);
    assertTrue(JsonnetStrings.compareByCodepoint("a", "") > 0);
  }

  @Test
  void asciiOrdering() {
    assertTrue(JsonnetStrings.compareByCodepoint("a", "b") < 0);
    assertTrue(JsonnetStrings.compareByCodepoint("b", "a") > 0);
    assertTrue(JsonnetStrings.compareByCodepoint("abc", "abd") < 0);
  }

  @Test
  void lengthTiebreaker() {
    assertTrue(JsonnetStrings.compareByCodepoint("ab", "abc") < 0);
    assertTrue(JsonnetStrings.compareByCodepoint("abc", "ab") > 0);
  }

  @Test
  void surrogateVsBmpOrdering() {
    // U+FFFF (last BMP char) should sort before U+10000 (first supplementary)
    // U+FFFF is \uFFFF in UTF-16
    // U+10000 is \uD800\uDC00 in UTF-16
    String bmpMax = "\uFFFF";
    String suppMin = "\uD800\uDC00";

    assertTrue(
        JsonnetStrings.compareByCodepoint(bmpMax, suppMin) < 0,
        "U+FFFF should sort before U+10000 in codepoint order");
  }

  @Test
  void emojiComparison() {
    // Earth emoji 🌍 = U+1F30D, Rocket 🚀 = U+1F680
    String earth = "\uD83C\uDF0D";
    String rocket = "\uD83D\uDE80";
    assertTrue(JsonnetStrings.compareByCodepoint(earth, rocket) < 0);
  }

  @Test
  void mixedBmpAndSurrogateStrings() {
    // "A🌍" vs "A🚀" — same prefix, different supplementary char
    String a = "A\uD83C\uDF0D";
    String b = "A\uD83D\uDE80";
    assertTrue(JsonnetStrings.compareByCodepoint(a, b) < 0);
  }

  @Test
  void codepointLengthNotUtf16Length() {
    // A string with one emoji (2 UTF-16 code units, 1 codepoint)
    // vs a string with two ASCII chars (2 UTF-16 code units, 2 codepoints)
    String oneEmoji = "🌍"; // 1 codepoint, 2 code units
    String twoAscii = "AB"; // 2 codepoints, 2 code units
    // '🌍' (U+1F30D) > 'A' (U+0041), so oneEmoji > twoAscii by first codepoint
    assertTrue(JsonnetStrings.compareByCodepoint(oneEmoji, twoAscii) > 0);
  }
}

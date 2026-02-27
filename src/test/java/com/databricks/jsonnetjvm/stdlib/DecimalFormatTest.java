package com.databricks.jsonnetjvm.stdlib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the DecimalFormat logic inside JsonnetFormat, ported from sjsonnet's
 * DecimalFormatTests.scala.
 */
public class DecimalFormatTest {

  /**
   * Mirrors sjsonnet's {@code DecimalFormatTests.format(pattern, n)} helper. Parses a pattern like
   * "0.000000E00" into (zeroes, hashes, alternate, expLength) and calls {@link
   * JsonnetFormat#decimalFormat}.
   */
  private static String format(String pattern, double n) {
    String wholePart;
    String fracPart = null;
    String expPart = null;

    // Split on '.' first, then each piece on 'E'
    String[] dotParts = pattern.split("\\.", -1);
    if (dotParts.length == 1) {
      String[] eParts = dotParts[0].split("E", -1);
      wholePart = eParts[0];
      if (eParts.length > 1) expPart = eParts[1];
    } else {
      wholePart = dotParts[0].split("E", -1)[0];
      String[] eParts = dotParts[1].split("E", -1);
      fracPart = eParts[0];
      if (eParts.length > 1) expPart = eParts[1];
    }

    assertEquals(1, wholePart.length(), "whole part must be single char: " + wholePart);

    int fracZeroes = 0;
    int fracHashes = 0;
    if (fracPart != null) {
      for (char c : fracPart.toCharArray()) {
        if (c == '0') fracZeroes++;
        else if (c == '#') fracHashes++;
      }
    }
    boolean alternate = fracPart != null && fracZeroes == 0 && fracHashes == 0;
    int expLength = expPart != null ? expPart.length() : -1;

    return JsonnetFormat.decimalFormat(fracZeroes, fracHashes, alternate, expLength, n);
  }

  @Test
  void exponentialBasic() {
    assertEquals("9.100000E02", format("0.000000E00", 910));
  }

  @Test
  void exponentialNoFrac() {
    assertEquals("9E02", format("0E00", 910));
  }

  @Test
  void exponentialSmallNumber() {
    assertEquals("9E-03", format("0E00", 0.009123));
  }

  @Test
  void exponentialShortExp() {
    assertEquals("9E-3", format("0E0", 0.009123));
  }

  @Test
  void exponentialVerySmall() {
    assertEquals("9E-12", format("0E0", 0.000000000009123));
  }

  @Test
  void exponentialNegative() {
    assertEquals("-9.100000E02", format("0.000000E00", -910));
  }

  @Test
  void exponentialFourFrac() {
    assertEquals("9.1030E02", format("0.0000E00", 910.3));
  }

  @Test
  void exponentialAlternate() {
    assertEquals("9.E02", format("0.E00", 910.3));
  }

  @Test
  void exponentialAlternateNoFrac() {
    assertEquals("9.E02", format("0.E00", 900));
  }

  @Test
  void exponentialLargeNumber() {
    assertEquals("1.000E09", format("0.000E00", 1000000001));
  }

  @Test
  void fixedBasic() {
    assertEquals("910.000000", format("0.000000", 910));
  }

  @Test
  void fixedZero() {
    assertEquals("0.000000", format("0.000000", 0));
  }

  @Test
  void fixedNoFrac() {
    assertEquals("910", format("0", 910));
  }

  @Test
  void fixedNegative() {
    assertEquals("-910.000000", format("0.000000", -910));
  }

  @Test
  void fixedFourZeroes() {
    assertEquals("910.3000", format("0.0000", 910.3));
  }

  @Test
  void fixedAlternate() {
    assertEquals("910.", format("0.", 910.3));
  }

  @Test
  void fixedAlternateNoFrac() {
    assertEquals("910.", format("0.", 910));
  }

  @Test
  void fixedLargeNumber() {
    assertEquals("1000000001.000", format("0.000", 1000000001));
  }

  @Test
  void expTwoDecLargeNumber() {
    assertEquals("1.00E09", format("0.00E00", 1000000001));
  }

  @Test
  void expTwoDecThousand() {
    assertEquals("1.10E03", format("0.00E00", 1100));
  }

  @Test
  void fixedTwoDec() {
    assertEquals("1.10", format("0.00", 1.1));
  }

  @Test
  void expFourDecLargeNumber() {
    assertEquals("1.0000E09", format("0.0000E00", 1000000001));
  }

  @Test
  void fixedFourDec() {
    assertEquals("1.1000", format("0.0000", 1.1));
  }

  @Test
  void expHashesLargeNumber() {
    assertEquals("1E09", format("0.##E00", 1000000001));
  }

  @Test
  void expHashesThousand() {
    assertEquals("1.1E03", format("0.##E00", 1100));
  }

  @Test
  void fixedHashes() {
    assertEquals("1.1", format("0.##", 1.1));
  }

  @Test
  void expFourHashesLargeNumber() {
    assertEquals("1E09", format("0.####E00", 1000000001));
  }

  @Test
  void fixedFourHashes() {
    assertEquals("1.1", format("0.####", 1.1));
  }
}

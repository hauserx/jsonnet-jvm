package com.databricks.jsonnetjvm.runtime;

/**
 * String utilities for Jsonnet. Jsonnet strings are defined over Unicode codepoints, so comparisons
 * and ordering must use codepoint-based logic, not UTF-16 code units.
 */
public final class JsonnetStrings {

  private JsonnetStrings() {}

  /**
   * Compare two strings by Unicode codepoint order (not UTF-16 code unit order). Returns negative
   * if a < b, zero if equal, positive if a > b.
   */
  public static int compareByCodepoint(String a, String b) {
    int i = 0, j = 0;
    while (i < a.length() && j < b.length()) {
      int cpA = a.codePointAt(i);
      int cpB = b.codePointAt(j);
      if (cpA != cpB) return Integer.compare(cpA, cpB);
      i += Character.charCount(cpA);
      j += Character.charCount(cpB);
    }
    return Integer.compare(a.codePointCount(0, a.length()), b.codePointCount(0, b.length()));
  }
}

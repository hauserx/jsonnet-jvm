package com.databricks.jsonnetjvm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Ported from sjsonnet ParserTests.scala. Tests parser behavior including operator precedence,
 * parse errors, duplicate detection, computed imports, identifier rules, and digit separator
 * support.
 */
public class ParserTest {

  private record EvalResult(int exitCode, String stdout, String stderr) {}

  private static EvalResult eval(String code) {
    PrintStream origOut = System.out;
    PrintStream origErr = System.err;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out));
    System.setErr(new PrintStream(err));
    int exitCode;
    try {
      exitCode = new CommandLine(new Main()).execute("-e", code);
    } finally {
      System.setOut(origOut);
      System.setErr(origErr);
    }
    return new EvalResult(exitCode, out.toString().trim(), err.toString().trim());
  }

  private static void assertEval(String code, String expected) {
    EvalResult r = eval(code);
    assertEquals(0, r.exitCode, "Unexpected error: " + r.stderr);
    assertEquals(expected, r.stdout);
  }

  private static void assertParseError(String code) {
    EvalResult r = eval(code);
    assertNotEquals(0, r.exitCode, "Expected parse error for: " + code);
    assertFalse(r.stderr.isEmpty(), "Expected error output for: " + code);
  }

  private static void assertStaticError(String code, String expectedSubstring) {
    EvalResult r = eval(code);
    assertNotEquals(0, r.exitCode, "Expected error for: " + code);
    assertTrue(
        r.stderr.contains(expectedSubstring),
        "Expected stderr to contain '" + expectedSubstring + "' but got: " + r.stderr);
  }

  @Nested
  class OperatorPrecedence {
    @Test
    void literals() {
      assertEval("true", "true");
    }

    @Test
    void additionIsLeftAssociative() {
      assertEval("123 + 456 + 789", "1368");
    }

    @Test
    void multiplicationBeforeAddition() {
      assertEval("1 * 2 + 3", "5");
    }

    @Test
    void additionBeforeMultiplication() {
      assertEval("1 + 2 * 3", "7");
    }
  }

  @Nested
  class DuplicateDetection {
    @Test
    void duplicateStaticFields() {
      assertStaticError("{ a: 1, a: 2 }", "Expected no duplicate field: a");
    }

    @Test
    void duplicateLocalInObject() {
      assertStaticError(
          "{\nlocal x = 1,\nlocal x = x + 1,\na: x,\n}", "Expected no duplicate local: x");
    }

    @Test
    void noDuplicateFieldsSucceeds() {
      assertEval("{ a: 1, b: 2 }", "{\"a\": 1, \"b\": 2}");
    }

    @Test
    void duplicateComputedFieldAtRuntime() {
      assertStaticError("{[\"k\"]: 1, [\"k\"]: 2}", "Duplicate key k");
    }

    @Test
    void duplicateMixedStaticComputed() {
      assertStaticError("{k: 1, [\"k\"]: 2}", "Duplicate key k");
    }

    @Test
    void duplicateFieldInComprehension() {
      assertStaticError(
          "{[x]: x for x in [\"A\", \"A\"]}", "Duplicate key A in evaluated object comprehension");
    }
  }

  @Nested
  class LocalInObject {
    @Test
    void localBindingInObject() {
      assertEval("{\nlocal x = 1,\na: x,\n}", "{\"a\": 1}");
    }
  }

  @Nested
  class ComputedImports {
    @Test
    void computedImportParenRejected() {
      assertParseError("local foo = import (\"foo\" + bar); 0");
    }

    @Test
    void importExpressionIsValue() {
      // import "foo" + bar parses as (import "foo") + bar — runtime error on bar
      assertStaticError("local foo = import \"foo\" + bar; 0", "Unknown variable: bar");
    }
  }

  @Nested
  class IdentifierRules {
    @Test
    void idStartingWithNumberIsError() {
      assertParseError("{1_n: \"foo\",}");
    }

    @Test
    void underscoreIdentifier() {
      // _123 should parse as identifier, not number
      assertStaticError("_123", "Unknown variable: _123");
    }
  }

  @Nested
  class DigitSeparators {
    @Test
    void basicIntegerSeparator() {
      assertEval("123_456", "123456");
    }

    @Test
    void millionsSeparator() {
      assertEval("1_750_000", "1750000");
    }

    @Test
    void singleDigitGroups() {
      assertEval("1_2_3", "123");
    }

    @Test
    void decimalSeparator() {
      assertEval("3.141_592", "3.141592");
    }

    @Test
    void integerWithDecimal() {
      assertEval("1_200.0", "1200");
    }

    @Test
    void exponentSeparator() {
      assertEval("0e1_01", "0");
    }

    @Test
    void integerAndExponentSeparator() {
      assertEval("10_10e3", "1010000");
    }

    @Test
    void bothSeparators() {
      assertEval("2_3e1_2", "23000000000000");
    }

    @Test
    void decimalAndExponentSeparator() {
      assertEval("std.assertEqual(1.1_2e100, 1.12e100)", "true");
    }

    @Test
    void negativeExponentSeparator() {
      assertEval("std.assertEqual(1.1e-10_1, 1.1e-101)", "true");
    }

    @Test
    void scientificNotation() {
      assertEval("std.assertEqual(9.109_383_56e-31, 9.10938356e-31)", "true");
    }

    @Test
    void leadingZeroWithSeparatorIsError() {
      // 0_5 -> 0 followed by _5 (identifier) -> two adjacent values
      assertParseError("0_5");
    }

    @Test
    void trailingUnderscoreIsError() {
      assertParseError("123456_");
    }

    @Test
    void doubleUnderscoreIsError() {
      assertParseError("123__456");
    }

    @Test
    void underscoreBeforeExponentIsError() {
      // 1_200 followed by _e2 (identifier)
      assertParseError("1_200_e2");
    }

    @Test
    void underscoreAfterExponentLetterIsError() {
      // 1_200 followed by e_2 (identifier)
      assertParseError("1_200e_2");
    }

    @Test
    void underscoreAfterExponentSignIsError() {
      // 200e-_2 -> parse error
      assertParseError("200e-_2");
    }

    @Test
    void underscoreAfterExponentPlusIsError() {
      assertParseError("200e+_2");
    }
  }
}

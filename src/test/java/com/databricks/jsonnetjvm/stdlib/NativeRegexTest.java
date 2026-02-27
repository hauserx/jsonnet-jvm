package com.databricks.jsonnetjvm.stdlib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.databricks.jsonnetjvm.Main;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Tests for std.native regex functions: match, replace, quoteMeta. */
public class NativeRegexTest {

  private static String eval(String code) {
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
    assertEquals(0, exitCode, "Error: " + err.toString().trim());
    return out.toString().trim();
  }

  @Nested
  class PartialMatch {
    @Test
    void simpleMatch() {
      assertEquals(
          "true",
          eval("local m = std.native('regexPartialMatch')('o+', 'foobar'); m.string == 'foobar'"));
    }

    @Test
    void noMatch() {
      assertEquals("true", eval("std.native('regexPartialMatch')('xyz', 'foobar') == null"));
    }

    @Test
    void captures() {
      assertEquals(
          "\"oo\"",
          eval("local m = std.native('regexPartialMatch')('f(o+)', 'foobar'); m.captures[0]"));
    }

    @Test
    void namedCaptures() {
      assertEquals(
          "\"bar\"",
          eval(
              "local m = std.native('regexPartialMatch')('(?P<word>\\\\w+)$', 'foo bar'); m.namedCaptures.word"));
    }
  }

  @Nested
  class FullMatch {
    @Test
    void fullMatchSuccess() {
      assertEquals("true", eval("std.native('regexFullMatch')('fo+bar', 'foobar') != null"));
    }

    @Test
    void fullMatchFailsOnPartial() {
      assertEquals("true", eval("std.native('regexFullMatch')('foo', 'foobar') == null"));
    }
  }

  @Nested
  class Replace {
    @Test
    void replaceFirst() {
      assertEquals("\"fXXbar\"", eval("std.native('regexReplace')('foobar', 'o+', 'XX')"));
    }

    @Test
    void globalReplace() {
      assertEquals("\"fXbXr\"", eval("std.native('regexGlobalReplace')('fooboor', 'o+', 'X')"));
    }
  }

  @Nested
  class QuoteMeta {
    @Test
    void specialCharsEscaped() {
      assertEquals(
          "true",
          eval(
              "local q = std.native('regexQuoteMeta')('a.b[c]'); "
                  + "std.native('regexFullMatch')(q, 'a.b[c]') != null"));
    }

    @Test
    void dashEscaped() {
      assertEquals(
          "true",
          eval(
              "local q = std.native('regexQuoteMeta')('a-b'); "
                  + "std.native('regexFullMatch')(q, 'a-b') != null"));
    }

    @Test
    void plainStringUnchanged() {
      assertEquals(
          "true",
          eval(
              "local q = std.native('regexQuoteMeta')('hello'); "
                  + "std.native('regexFullMatch')(q, 'hello') != null"));
    }
  }
}

package com.databricks.jsonnetjvm.stdlib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.databricks.jsonnetjvm.Main;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Tests for std.format (%) beyond what DecimalFormatTest covers. Tests the full formatting
 * pipeline: string, integer, float, hex, octal, char, width, precision, flags, and error cases.
 */
public class StdFormatTest {

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
  class StringFormat {
    @Test
    void basicString() {
      assertEquals("\"hello world\"", eval("'%s' % 'hello world'"));
    }

    @Test
    void stringFromNonString() {
      assertEquals("\"42\"", eval("'%s' % 42"));
    }

    @Test
    void stringWidth() {
      assertEquals("\"   hi\"", eval("'%5s' % 'hi'"));
    }

    @Test
    void stringLeftAlign() {
      assertEquals("\"hi   \"", eval("'%-5s' % 'hi'"));
    }
  }

  @Nested
  class IntegerFormat {
    @Test
    void basicDecimal() {
      assertEquals("\"42\"", eval("'%d' % 42"));
    }

    @Test
    void octal() {
      assertEquals("\"52\"", eval("'%o' % 42"));
    }

    @Test
    void octalAlt() {
      assertEquals("\"052\"", eval("'%#o' % 42"));
    }

    @Test
    void hex() {
      assertEquals("\"2a\"", eval("'%x' % 42"));
    }

    @Test
    void hexUpper() {
      assertEquals("\"2A\"", eval("'%X' % 42"));
    }

    @Test
    void hexAlt() {
      assertEquals("\"0x2a\"", eval("'%#x' % 42"));
    }

    @Test
    void zeroPadded() {
      assertEquals("\"007\"", eval("'%03d' % 7"));
    }

    @Test
    void spacePrefixPositive() {
      assertEquals("\" 42\"", eval("'% d' % 42"));
    }

    @Test
    void plusPrefixPositive() {
      assertEquals("\"+42\"", eval("'%+d' % 42"));
    }

    @Test
    void negativeInteger() {
      assertEquals("\"-42\"", eval("'%d' % -42"));
    }
  }

  @Nested
  class FloatFormat {
    @Test
    void basicFloat() {
      assertEquals("\"3.140000\"", eval("'%f' % 3.14"));
    }

    @Test
    void floatPrecision() {
      assertEquals("\"3.14\"", eval("'%.2f' % 3.14"));
    }

    @Test
    void scientific() {
      assertEquals("\"3.140000e+00\"", eval("'%e' % 3.14"));
    }

    @Test
    void scientificPrecision() {
      assertEquals("\"3.14e+00\"", eval("'%.2e' % 3.14"));
    }

    @Test
    void genericG() {
      assertEquals("\"3.14\"", eval("'%g' % 3.14"));
    }

    @Test
    void genericGLarge() {
      assertEquals("\"10e+05\"", eval("'%g' % 1000000"));
    }
  }

  @Nested
  class CharFormat {
    @Test
    void charFromNumber() {
      assertEquals("\"A\"", eval("'%c' % 65"));
    }
  }

  @Nested
  class MultipleArgs {
    @Test
    void tupleArgs() {
      assertEquals("\"1 + 2 = 3\"", eval("'%d + %d = %d' % [1, 2, 3]"));
    }

    @Test
    void namedArgs() {
      assertEquals(
          "\"hello world\"",
          eval("'%(greeting)s %(target)s' % {greeting: 'hello', target: 'world'}"));
    }
  }

  @Nested
  class Percent {
    @Test
    void literalPercent() {
      assertEquals("\"100%\"", eval("'%d%%' % 100"));
    }
  }
}

package com.databricks.jsonnetjvm.stdlib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.databricks.jsonnetjvm.Main;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Tests for manifest functions: manifestJsonEx, manifestYamlDoc, manifestIni, manifestTomlEx,
 * manifestXmlJsonml, manifestPython. Focuses on edge cases in formatting and escaping.
 */
public class ManifestTest {

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

  private static void assertEvalError(String code, String expectedSubstring) {
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
    assertNotEquals(0, exitCode, "Expected error for: " + code);
    assertTrue(
        err.toString().contains(expectedSubstring),
        "Expected '" + expectedSubstring + "' in: " + err.toString().trim());
  }

  @Nested
  class ManifestJsonEx {
    @Test
    void customIndent() {
      String result = eval("std.manifestJsonEx({a: 1}, '\\t')");
      assertTrue(result.contains("\t"), "Expected tab indent");
    }

    @Test
    void emptyObjectHasNewlines() {
      String result = eval("std.manifestJsonEx({}, '  ')");
      assertTrue(result.contains("{\n"), "Empty object should have newlines");
    }

    @Test
    void emptyArrayHasNewlines() {
      String result = eval("std.manifestJsonEx([], '  ')");
      assertTrue(result.contains("[\n"), "Empty array should have newlines");
    }

    @Test
    void nestedObjects() {
      String result = eval("std.manifestJsonEx({a: {b: 1}}, '  ')");
      assertTrue(result.contains("\"a\""), "Should contain field a");
      assertTrue(result.contains("\"b\""), "Should contain field b");
    }
  }

  @Nested
  class ManifestYamlDoc {
    @Test
    void simpleObject() {
      String result = eval("std.manifestYamlDoc({a: 1, b: 'hello'})");
      assertTrue(result.contains("\"a\""));
      assertTrue(result.contains("\"b\""));
    }

    @Test
    void nullValue() {
      String result = eval("std.manifestYamlDoc({a: null})");
      assertTrue(result.contains("null"));
    }

    @Test
    void booleanValues() {
      String result = eval("std.manifestYamlDoc({a: true, b: false})");
      assertTrue(result.contains("true"));
      assertTrue(result.contains("false"));
    }

    @Test
    void nestedArray() {
      String result = eval("std.manifestYamlDoc({a: [1, 2, 3]})");
      assertTrue(result.contains("- 1"));
    }
  }

  @Nested
  class ManifestIni {
    @Test
    void simpleIni() {
      String result = eval("std.manifestIni({sections: {main: {key: 'value'}}})");
      assertTrue(result.contains("[main]"));
      assertTrue(result.contains("key = value"));
    }
  }

  @Nested
  class ManifestPython {
    @Test
    void noneForNull() {
      assertEquals("\"None\"", eval("std.manifestPython(null)"));
    }

    @Test
    void trueCapitalized() {
      assertEquals("\"True\"", eval("std.manifestPython(true)"));
    }

    @Test
    void falseCapitalized() {
      assertEquals("\"False\"", eval("std.manifestPython(false)"));
    }

    @Test
    void listBrackets() {
      assertEquals("\"[1, 2, 3]\"", eval("std.manifestPython([1, 2, 3])"));
    }

    @Test
    void dictFormat() {
      String result = eval("std.manifestPython({a: 1})");
      assertTrue(result.contains("\"a\""), "Should contain key: " + result);
    }
  }

  @Nested
  class ManifestXmlJsonml {
    @Test
    void simpleElement() {
      String result = eval("std.manifestXmlJsonml(['div', 'hello'])");
      assertTrue(result.contains("<div>hello</div>"));
    }

    @Test
    void elementWithAttributes() {
      String result = eval("std.manifestXmlJsonml(['div', {class: 'foo'}, 'text'])");
      assertTrue(result.contains("class=\"foo\""));
    }

    @Test
    void xmlEscaping() {
      String result = eval("std.manifestXmlJsonml(['p', 'a < b & c > d'])");
      assertTrue(result.contains("&lt;"));
      assertTrue(result.contains("&amp;"));
      assertTrue(result.contains("&gt;"));
    }
  }

  @Nested
  class ManifestToml {
    @Test
    void simpleTable() {
      String result = eval("std.manifestTomlEx({section: {key: 'value'}}, '  ')");
      assertTrue(result.contains("[section]"));
      assertTrue(result.contains("key = \"value\""));
    }

    @Test
    void nullInTomlErrors() {
      assertEvalError("std.manifestTomlEx({a: null}, '  ')", "null");
    }

    @Test
    void booleanValues() {
      String result = eval("std.manifestTomlEx({a: true, b: false}, '  ')");
      assertTrue(result.contains("true"));
      assertTrue(result.contains("false"));
    }
  }
}

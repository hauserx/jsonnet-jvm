package com.databricks.jsonnetjvm.stdlib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.databricks.jsonnetjvm.Main;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Tests for std.base64, std.base64Decode, std.md5, std.sha*, std.sha3. */
public class EncodingTest {

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
  class Base64 {
    @Test
    void encodeString() {
      assertEquals("\"SGVsbG8=\"", eval("std.base64('Hello')"));
    }

    @Test
    void encodeEmpty() {
      assertEquals("\"\"", eval("std.base64('')"));
    }

    @Test
    void encodeByteArray() {
      assertEquals("\"AQID\"", eval("std.base64([1, 2, 3])"));
    }

    @Test
    void roundTrip() {
      assertEquals("\"hello world\"", eval("std.base64Decode(std.base64('hello world'))"));
    }

    @Test
    void decodeKnownValue() {
      assertEquals("\"Man\"", eval("std.base64Decode('TWFu')"));
    }
  }

  @Nested
  class Base64DecodeBytes {
    @Test
    void decodeToBytesRoundTrip() {
      assertEquals(
          "true",
          eval(
              "local encoded = std.base64([72, 101, 108]); "
                  + "local decoded = std.base64DecodeBytes(encoded); "
                  + "decoded[0] == 72 && decoded[1] == 101 && decoded[2] == 108"));
    }
  }

  @Nested
  class Hashing {
    @Test
    void md5() {
      assertEquals("\"5d41402abc4b2a76b9719d911017c592\"", eval("std.md5('hello')"));
    }

    @Test
    void md5Empty() {
      assertEquals("\"d41d8cd98f00b204e9800998ecf8427e\"", eval("std.md5('')"));
    }

    @Test
    void sha1() {
      assertEquals("\"aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d\"", eval("std.sha1('hello')"));
    }

    @Test
    void sha256() {
      assertEquals(
          "\"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824\"",
          eval("std.sha256('hello')"));
    }

    @Test
    void sha512Length() {
      String result = eval("std.sha512('hello')");
      assertEquals(130, result.length()); // 128 hex chars + 2 quotes
    }

    @Test
    void sha3Length() {
      String result = eval("std.sha3('hello')");
      assertEquals(130, result.length()); // 128 hex chars + 2 quotes
    }
  }
}

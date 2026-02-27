package com.databricks.jsonnetjvm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

/**
 * Parameterized integration tests. Each *.jsonnet file in src/test/resources/cpp_test_suite/
 * becomes a test case automatically. A corresponding *.jsonnet.golden file holds the expected
 * output; if absent it is created from the first run (bootstrapping).
 *
 * <p>Error tests (name starts with "error.") are expected to fail with exit code 1. Their golden
 * files contain the expected error message (first line of stderr).
 */
public class IntegrationTest {

  // Tests skipped because they require major unimplemented language features.
  // Tracked in TODO.md — re-enable as features are implemented.
  static final Set<String> SKIP = Set.of();

  static Stream<String> testCases() throws Exception {
    Path resourceDir = Path.of("src/test/resources/cpp_test_suite");
    return Files.list(resourceDir)
        .filter(p -> p.toString().endsWith(".jsonnet") && !p.toString().endsWith(".golden"))
        .map(p -> p.getFileName().toString().replace(".jsonnet", ""))
        .sorted();
  }

  static Stream<String> newTestCases() throws Exception {
    Path resourceDir = Path.of("src/test/resources/new_test_suite");
    if (!Files.exists(resourceDir)) return Stream.empty();
    return Files.list(resourceDir)
        .filter(p -> p.toString().endsWith(".jsonnet") && !p.toString().endsWith(".golden"))
        .map(p -> p.getFileName().toString().replace(".jsonnet", ""))
        .sorted();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testCases")
  void runJsonnetTest(String testName) throws Exception {
    assumeFalse(SKIP.contains(testName), testName + " skipped — requires unimplemented features");
    Path resourceDir = Path.of("src/test/resources/cpp_test_suite");
    Path jsonnetFile = resourceDir.resolve(testName + ".jsonnet");
    Path goldenFile = resourceDir.resolve(testName + ".jsonnet.golden");

    boolean isErrorTest = testName.startsWith("error.");

    // Capture stdout and stderr
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStream));
    System.setErr(new PrintStream(errStream));

    List<String> args = new ArrayList<>();
    if ("stdlib".equals(testName)) {
      args.add("--ext-str");
      args.add("var1=test");
      args.add("--ext-code");
      args.add("var2={x:1, y:2}");
    }
    args.add(jsonnetFile.toString());

    int exitCode;
    try {
      exitCode = new CommandLine(new Main()).execute(args.toArray(new String[0]));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    String actualOutput = outStream.toString().trim();
    String actualError = errStream.toString().trim();

    if (!Files.exists(goldenFile)) {
      // Bootstrap: write golden file on first run
      if (isErrorTest) {
        String errorLine = actualError.isEmpty() ? "ERROR: (unknown)" : actualError;
        System.err.println("[bootstrap] error test " + testName + ": " + errorLine);
        Files.writeString(goldenFile, errorLine + "\n");
      } else {
        System.err.println(
            "[bootstrap] no golden for " + testName + ", writing from actual:\n" + actualOutput);
        Files.writeString(goldenFile, actualOutput + "\n");
      }
    }

    String expected = Files.readString(goldenFile).trim();

    if (isErrorTest) {
      // Error test: must exit with non-zero code
      assertNotEquals(0, exitCode, "Error test should have failed: " + testName);
      // Compare error message
      assertEquals(expected, actualError, "Error message mismatch for " + testName);
    } else if (expected.startsWith("ERROR:")) {
      // Legacy: golden file starts with ERROR:
      assertEquals(1, exitCode, "Expected error exit for " + testName);
    } else {
      assertEquals(0, exitCode, "Unexpected error for " + testName + " stderr: " + actualError);
      assertEquals(normalize(expected), normalize(actualOutput));
    }
  }

  @ParameterizedTest(name = "new/{0}")
  @MethodSource("newTestCases")
  void runNewJsonnetTest(String testName) throws Exception {
    Path resourceDir = Path.of("src/test/resources/new_test_suite");
    Path jsonnetFile = resourceDir.resolve(testName + ".jsonnet");
    Path goldenFile = resourceDir.resolve(testName + ".jsonnet.golden");
    Path argsFile = resourceDir.resolve(testName + ".jsonnet.args");

    boolean isErrorTest = testName.startsWith("error.");

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStream));
    System.setErr(new PrintStream(errStream));

    List<String> args = new ArrayList<>();
    if (Files.exists(argsFile)) {
      for (String line : Files.readAllLines(argsFile)) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
          args.add(trimmed);
        }
      }
    }
    args.add(jsonnetFile.toString());

    int exitCode;
    try {
      exitCode = new CommandLine(new Main()).execute(args.toArray(new String[0]));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    String actualOutput = outStream.toString().trim();
    String actualError = errStream.toString().trim();

    if (!Files.exists(goldenFile)) {
      if (isErrorTest) {
        String errorLine = actualError.isEmpty() ? "ERROR: (unknown)" : actualError;
        Files.writeString(goldenFile, errorLine + "\n");
      } else {
        Files.writeString(goldenFile, actualOutput + "\n");
      }
    }

    String expected = Files.readString(goldenFile).trim();

    if (isErrorTest) {
      assertNotEquals(0, exitCode, "Error test should have failed: " + testName);
      assertEquals(expected, actualError, "Error message mismatch for " + testName);
    } else {
      assertEquals(0, exitCode, "Unexpected error for " + testName + " stderr: " + actualError);
      assertEquals(normalize(expected), normalize(actualOutput));
    }
  }

  private String normalize(String json) {
    return json.replaceAll("\\s+", "");
  }
}

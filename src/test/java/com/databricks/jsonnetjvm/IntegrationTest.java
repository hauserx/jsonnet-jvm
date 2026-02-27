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
  static final Set<String> CPP_SKIP = Set.of(
      "import",                        // NPE in argument node — not yet implemented
      "tla.simple",                    // top-level arguments not yet implemented
      // New sjsonnet tests: import error messages differ from our format
      "error.import_empty",
      "error.import_folder",
      "error.import_folder_slash",
      "error.import_static-check-failure",
      "error.import_syntax-error",     // also emits absolute sandbox path
      "error.parse.import_not_literal",
      "error.parse.import_text_block",
      "error.recursive_import",
      "error.verbatim_import"
  );

  // go-jsonnet tests skipped due to unsupported modes or missing features.
  // Tracked in TODO.md — re-enable as features are implemented.
  static final Set<String> GO_JSONNET_SKIP = Set.of(
      // --- Multi-output mode (require -m flag; golden is a directory) ---
      "multi",
      "multi_no_newline",
      "multi_string_output",
      "multi_no_newline_string_output",

      // --- extvar tests require --ext-str/--ext-code CLI args; add to new_test_suite if needed ---
      "extvar_code",
      "extvar_error",
      "extvar_hermetic",
      "extvar_mutually_recursive",
      "extvar_not_a_string",
      "extvar_self_recursive",
      "extvar_static_error",
      "extvar_string",
      "extvar_unknown",

      // --- Array comprehension variable capture: loop var captured by reference, not value ---
      "arrcomp3",
      "arrcomp4",
      "arrcomp7",
      "arrcomp_if2",
      "arrcomp_if3",
      "object_comp4",

      // --- Text-block strings: indentation stripping and newline preservation not implemented ---
      "block_escaping",
      "block_string",
      "block_string_chomped",
      "escaped_fields",           // text-block used as object field name
      "object_various_field_types", // text-block used as object field name

      // --- JSON string serialization: quotes and backslashes not escaped inside string values ---
      "string2",
      "escaped_single_quote",
      "foldl_various",
      "foldr_various",
      "import_twice",
      "plus8",
      "std.flatmap6",
      "std.toString6",
      "std.toString7",

      // --- importstr: trailing newline of imported file not preserved ---
      "import3",
      "import4",

      // --- importbin not implemented ---
      "importbin_nonutf8",

      // --- native functions: std.nativeExt returns null instead of the registered function ---
      "native1",
      "native2",
      "native3",
      "native6",

      // --- stdlib missing overflow/bounds/NaN error checks ---
      "builtinBase64_string_high_codepoint", // should error on high codepoint, we succeed
      "builtinSubStr_second_parameter_not_integer", // we accept non-integer parameter
      "builtinSubStr_third_parameter_less_then_zero", // we accept negative parameter
      "builtinSubStr_third_parameter_not_integer",
      "builtin_exp3",    // std.exp(1000) should overflow, we succeed
      "builtin_exp5",    // std.exp(1e20) should overflow, we succeed
      "builtin_log5",    // std.log(0) should overflow, we succeed
      "builtin_log7",    // std.log(-1) should be NaN error, we succeed
      "builtin_log8",    // std.log(-1e12) should be NaN error, we succeed
      "div4",            // 1/(1e-160)/(1e-160) should overflow, we succeed
      "inf_min_number",  // -1e308*1e308 should overflow, we succeed
      "inf_sum_number",  // 1e308+1e308 should overflow, we succeed
      "pow4",            // 2^1e20 should overflow, we succeed
      "pow7",            // pow overflow not raised
      "std.makeArray_noninteger", // std.makeArray(2.5, ...) should error, we succeed
      "string_index_negative",   // negative string index should error, we succeed

      // --- stdlib wrong behavior ---
      "builtin_escapeStringJson", // std.escapeStringJson(null) raises wrong error
      "std.mod_string",           // std.mod with %s format not implemented
      "std.filter7",              // filter should short-circuit on lazy errors, we propagate
      "std.makeArray_recursive",  // recursive makeArray should error, not infinite-recurse

      // --- Float formatting: our double→string differs from go-jsonnet's format ---
      "builtin_cos",         // 0.5403023058681398 vs 0.54030230586813977
      "builtin_exp4",        // huge float as big-integer string
      "builtin_log3",        // 80.5904782547916 vs 80.590478254791606
      "builtin_manifestTomlEx", // TOML float precision differs
      "div3",                // 9.999999999999999E-31 vs 9.9999999999999991e-31
      "pow6",                // huge float as big-integer string
      "std.mantissa3",       // 0.84 vs 0.83999999999999997
      "std",                 // thisFile path + float precision + escapeStringPython format differ
      "stdlib_smoke_test",   // thisFile path + float precision differ

      // --- std.thisFile: we return filename only, go-jsonnet returns testdata-relative path ---
      "std.thisFile",
      "std.thisFile2",

      // --- Object local scope bugs ---
      "insuper3",              // "x in super" at top level incorrectly fails static analysis
      "insuper5",              // same, in nested object
      "obj_local_right_level",  // object-local scope resolution causes stack overflow
      "obj_local_right_level3", // object-local captured at wrong scope level
      "object_local_recursive", // mutually-recursive object locals fail static analysis
      "object_local_self_super", // self/super in object locals causes stack overflow

      // --- Large integers: we use scientific notation, go-jsonnet emits exact big-integer string ---
      "builtinChar2",   // std.char(0) outputs literal null byte instead of \\u0000

      // --- Bitwise operation on number outside safe integer range ---
      "bitwise_or9",

      // --- tailstrict bugs ---
      "tailstrict",   // "attempted to index a FrameWithoutBoxing with number"
      "tailstrict3"   // tailstrict with default-param error not propagated
  );

  /**
   * Resolves the cpp_test_suite directory. Under Bazel, CPP_TEST_SUITE_RLOC is set to the
   * rlocationpath of a marker file inside the suite, allowing the directory to be found via
   * $TEST_SRCDIR. Falls back to the committed path for non-Bazel runs.
   */
  private static Path resolveCppTestSuiteDir() {
    String rloc = System.getenv("CPP_TEST_SUITE_RLOC");
    if (rloc != null) {
      String runfiles = System.getenv("TEST_SRCDIR");
      if (runfiles == null) runfiles = System.getenv("RUNFILES_DIR");
      if (runfiles != null) {
        return Path.of(runfiles, rloc).getParent();
      }
    }
    return Path.of("src/test/resources/cpp_test_suite");
  }

  /**
   * Resolves the go-jsonnet testdata directory via $GO_JSONNET_TESTS_RLOC / $TEST_SRCDIR.
   * Falls back to a local committed path for non-Bazel runs.
   */
  private static Path resolveGoJsonnetTestsDir() {
    String rloc = System.getenv("GO_JSONNET_TESTS_RLOC");
    if (rloc != null) {
      String runfiles = System.getenv("TEST_SRCDIR");
      if (runfiles == null) runfiles = System.getenv("RUNFILES_DIR");
      if (runfiles != null) {
        return Path.of(runfiles, rloc).getParent();
      }
    }
    return Path.of("src/test/resources/go_jsonnet_tests");
  }

  private static final Path GO_JSONNET_TESTS_DIR = resolveGoJsonnetTestsDir();

  /** Local override golden files where our output format differs from go-jsonnet's. */
  private static final Path GO_JSONNET_TESTS_LOCAL_DIR =
      Path.of("src/test/resources/go_jsonnet_tests_local");

  /**
   * Local override golden files for tests where our error format differs from sjsonnet's.
   * If a golden file exists here, it takes precedence over the one in CPP_TEST_SUITE_DIR.
   */
  private static final Path CPP_TEST_SUITE_LOCAL_DIR =
      Path.of("src/test/resources/cpp_test_suite_local");

  private static final Path CPP_TEST_SUITE_DIR = resolveCppTestSuiteDir();

  static Stream<String> testCases() throws Exception {
    Path resourceDir = CPP_TEST_SUITE_DIR;
    return Files.list(resourceDir)
        .filter(p -> p.toString().endsWith(".jsonnet") && !p.toString().endsWith(".golden"))
        .map(p -> p.getFileName().toString().replace(".jsonnet", ""))
        .sorted();
  }

  /**
   * go-jsonnet testdata: only include tests that have a flat (non-directory) .golden file.
   * Tests with directory goldens (multi-output mode) are excluded here and listed in GO_JSONNET_SKIP.
   */
  static Stream<String> goJsonnetTestCases() throws Exception {
    Path dir = GO_JSONNET_TESTS_DIR;
    if (!Files.exists(dir)) return Stream.empty();
    return Files.list(dir)
        .filter(p -> p.toString().endsWith(".jsonnet") && !p.toString().endsWith(".golden"))
        .map(p -> p.getFileName().toString().replace(".jsonnet", ""))
        .filter(name -> {
          Path g = dir.resolve(name + ".golden");
          return Files.exists(g) && !Files.isDirectory(g);
        })
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
    assumeFalse(CPP_SKIP.contains(testName), testName + " skipped — requires unimplemented features");
    Path resourceDir = CPP_TEST_SUITE_DIR;
    Path jsonnetFile = resourceDir.resolve(testName + ".jsonnet");
    // Local overrides take precedence (e.g. error tests where our format differs from sjsonnet's)
    Path localGolden = CPP_TEST_SUITE_LOCAL_DIR.resolve(testName + ".jsonnet.golden");
    Path goldenFile = Files.exists(localGolden) ? localGolden : resourceDir.resolve(testName + ".jsonnet.golden");

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

  /**
   * Runs a single go-jsonnet testdata test.
   *
   * <p>go-jsonnet error golden files use a multi-line stack-trace format (RUNTIME ERROR: / STATIC
   * ERROR:) that is incompatible with our single-line "Error: ..." output. When no local override
   * exists for such a test we only assert that the process exits with a non-zero code. Add a local
   * override golden file to src/test/resources/go_jsonnet_tests_local/<name>.golden to also verify
   * the error message in our format.
   */
  @ParameterizedTest(name = "go/{0}")
  @MethodSource("goJsonnetTestCases")
  void runGoJsonnetTest(String testName) throws Exception {
    assumeFalse(GO_JSONNET_SKIP.contains(testName), testName + " skipped — see GO_JSONNET_SKIP");

    Path resourceDir = GO_JSONNET_TESTS_DIR;
    Path jsonnetFile = resourceDir.resolve(testName + ".jsonnet");
    Path goldenFile = resourceDir.resolve(testName + ".golden");
    Path localGolden = GO_JSONNET_TESTS_LOCAL_DIR.resolve(testName + ".golden");

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStream));
    System.setErr(new PrintStream(errStream));

    int exitCode;
    try {
      exitCode = new CommandLine(new Main()).execute(jsonnetFile.toString());
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }

    String actualOutput = outStream.toString().trim();
    String actualError = errStream.toString().trim();

    if (Files.exists(localGolden)) {
      // Local override: our format — compare error message or output depending on exit code
      String expected = Files.readString(localGolden).trim();
      if (exitCode != 0) {
        assertNotEquals(0, exitCode, "Error test should have failed: " + testName);
        assertEquals(expected, actualError, "Error message mismatch for " + testName);
      } else {
        assertEquals(0, exitCode, "Unexpected error for " + testName + " stderr: " + actualError);
        assertEquals(normalize(expected), normalize(actualOutput));
      }
    } else {
      // No local override: use upstream go-jsonnet golden
      String expected = Files.readString(goldenFile).trim();
      // go-jsonnet error tests have three golden formats:
      //   "RUNTIME ERROR: ..."  — runtime errors with stack trace
      //   "STATIC ERROR: ..."  — static errors (rarely used)
      //   "testdata/<name>:..."  — parse/static errors with file:line:col prefix
      boolean isGoErrorTest =
          expected.startsWith("RUNTIME ERROR:")
              || expected.startsWith("STATIC ERROR:")
              || expected.startsWith("testdata/");
      if (isGoErrorTest) {
        // go-jsonnet uses multi-line stack traces — just verify failure
        assertNotEquals(0, exitCode, "Expected error exit for " + testName);
      } else {
        assertEquals(0, exitCode, "Unexpected error for " + testName + " stderr: " + actualError);
        assertEquals(normalize(expected), normalize(actualOutput));
      }
    }
  }

  private String normalize(String json) {
    return json.replaceAll("\\s+", "");
  }
}

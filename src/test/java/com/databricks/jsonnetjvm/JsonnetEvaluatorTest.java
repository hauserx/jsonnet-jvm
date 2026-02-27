package com.databricks.jsonnetjvm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonnetEvaluatorTest {

  @Test
  void basicSnippet() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result = eval.newEvaluation().evaluateSnippet("1 + 2");
      assertEquals("3", result);
    }
  }

  @Test
  void extStrBinding() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result =
          eval.newEvaluation()
              .extStr("name", "world")
              .evaluateSnippet("\"hello \" + std.extVar(\"name\")");
      assertEquals("\"hello world\"", result);
    }
  }

  @Test
  void extCodeBinding() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result =
          eval.newEvaluation().extCode("val", "{a: 1}").evaluateSnippet("std.extVar(\"val\").a");
      assertEquals("1", result);
    }
  }

  @Test
  void tlaBindings() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result =
          eval.newEvaluation()
              .tlaStr("name", "world")
              .evaluateSnippet("function(name) \"hello \" + name");
      assertEquals("\"hello world\"", result);
    }
  }

  @Test
  void repeatedEvaluationSharesEngine() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      for (int i = 0; i < 5; i++) {
        String result =
            eval.newEvaluation()
                .extCode("n", String.valueOf(i))
                .evaluateSnippet("std.extVar(\"n\") * 2");
        assertEquals(String.valueOf(i * 2), result);
      }
    }
  }

  @Test
  void evaluateFile(@TempDir Path tempDir) throws Exception {
    Path jsonnetFile = tempDir.resolve("test.jsonnet");
    Files.writeString(jsonnetFile, "{ greeting: \"hello \" + std.extVar(\"name\") }");

    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result = eval.newEvaluation().extStr("name", "alice").evaluateFile(jsonnetFile);
      assertTrue(result.contains("hello alice"), "Expected greeting in: " + result);
    }
  }

  @Test
  void differentTlaPerEvaluation() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String src = "function(x) x * x";
      assertEquals("4", eval.newEvaluation().tlaCode("x", "2").evaluateSnippet(src));
      assertEquals("9", eval.newEvaluation().tlaCode("x", "3").evaluateSnippet(src));
      assertEquals("100", eval.newEvaluation().tlaCode("x", "10").evaluateSnippet(src));
    }
  }

  @Test
  void jpathResolution(@TempDir Path tempDir) throws Exception {
    Path libDir = tempDir.resolve("lib");
    Files.createDirectories(libDir);
    Files.writeString(libDir.resolve("helper.jsonnet"), "{ greet(name):: \"hi \" + name }");

    Path mainFile = tempDir.resolve("main.jsonnet");
    Files.writeString(mainFile, "local h = import \"helper.jsonnet\"; h.greet(\"bob\")");

    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      eval.addJpath(libDir);
      String result = eval.newEvaluation().evaluateFile(mainFile);
      assertEquals("\"hi bob\"", result);
    }
  }

  @Test
  void extVarCachingWithinEvaluation() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result =
          eval.newEvaluation()
              .extStr("x", "cached")
              .evaluateSnippet("std.extVar(\"x\") + \" \" + std.extVar(\"x\")");
      assertEquals("\"cached cached\"", result);
    }
  }

  @Test
  void customNativeFunction() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      eval.registerNative(
          "double",
          new String[] {"x"},
          args -> {
            double x = (double) args[0];
            return x * 2;
          });
      String result = eval.newEvaluation().evaluateSnippet("std.native(\"double\")(21)");
      assertEquals("42", result);
    }
  }

  @Test
  void customNativeWithStrings() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      eval.registerNative(
          "greet",
          new String[] {"name"},
          args -> {
            return "Hello, " + args[0] + "!";
          });
      String result = eval.newEvaluation().evaluateSnippet("std.native(\"greet\")(\"World\")");
      assertEquals("\"Hello, World!\"", result);
    }
  }

  @Test
  void customNativePerEvaluation() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      eval.registerNative(
          "getTag",
          new String[] {},
          args -> {
            return "v1";
          });
      assertEquals("\"v1\"", eval.newEvaluation().evaluateSnippet("std.native(\"getTag\")()"));
    }
  }

  @Test
  void unknownNativeReturnsNull() {
    try (JsonnetEvaluator eval = new JsonnetEvaluator()) {
      String result = eval.newEvaluation().evaluateSnippet("std.native(\"nonexistent\") == null");
      assertEquals("true", result);
    }
  }
}

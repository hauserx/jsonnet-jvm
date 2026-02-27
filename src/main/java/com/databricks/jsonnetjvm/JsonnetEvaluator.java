package com.databricks.jsonnetjvm;

import com.databricks.jsonnetjvm.runtime.JsonnetConfig;
import com.databricks.jsonnetjvm.stdlib.NativeGzip;
import com.databricks.jsonnetjvm.stdlib.NativeRegex;
import com.databricks.jsonnetjvm.stdlib.NativeXz;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * High-level API for evaluating Jsonnet programs, optimized for repeated evaluation of the same (or
 * similar) sources with different bindings.
 *
 * <p>Shares a single Graal Engine across all evaluations so that the Truffle parse cache persists
 * across evaluations, JIT-compiled code is reused, and the std library is created once.
 */
public class JsonnetEvaluator implements Closeable {

  private final Engine engine;
  private final List<Path> jpaths = new ArrayList<>();
  private final List<NativeRegistration> nativeRegistrations = new ArrayList<>();

  public JsonnetEvaluator() {
    this.engine = Engine.newBuilder().allowExperimentalOptions(true).build();
  }

  /** Adds a library search path shared by all evaluations. */
  public JsonnetEvaluator addJpath(Path path) {
    jpaths.add(path);
    return this;
  }

  /**
   * Registers a native function accessible via {@code std.native("name")} in Jsonnet. Arguments are
   * automatically forced (thunks evaluated) before the function body runs.
   *
   * @param name the name used in {@code std.native("name")}
   * @param paramNames parameter names for error messages and named argument support
   * @param body the function body; receives forced arguments as Object[]
   * @return this evaluator for chaining
   */
  public JsonnetEvaluator registerNative(
      String name, String[] paramNames, Function<Object[], Object> body) {
    nativeRegistrations.add(new NativeRegistration(name, paramNames, body));
    return this;
  }

  /**
   * Registers the built-in native functions (regex, gzip, xz) that ship with jsonnet-jvm. Call this
   * unless you want a bare evaluator without built-in natives.
   *
   * @return this evaluator for chaining
   */
  public JsonnetEvaluator registerDefaultNatives() {
    NativeRegex.register(this);
    NativeGzip.register(this);
    NativeXz.register(this);
    return this;
  }

  /** Creates a new evaluation builder with fresh bindings. */
  public Evaluation newEvaluation() {
    return new Evaluation();
  }

  private record NativeRegistration(
      String name, String[] paramNames, Function<Object[], Object> body) {}

  @Override
  public void close() {
    engine.close();
  }

  /**
   * A single evaluation with its own ext vars, TLA vars, and caches. Each evaluation creates a
   * fresh {@link Context} that shares the underlying {@link Engine}.
   */
  public class Evaluation {
    private final Map<String, String> extVars = new HashMap<>();
    private final Map<String, String> tlaVars = new HashMap<>();
    private final List<Path> evalJpaths = new ArrayList<>();
    private boolean stringOutput;

    private Evaluation() {}

    public Evaluation extStr(String name, String value) {
      extVars.put(name, "\"" + escapeJson(value) + "\"");
      return this;
    }

    public Evaluation extCode(String name, String code) {
      extVars.put(name, code);
      return this;
    }

    /** Sets an external variable whose value is the string contents of a file. */
    public Evaluation extStrFile(String name, Path file) {
      try {
        extVars.put(name, "\"" + escapeJson(Files.readString(file)) + "\"");
      } catch (IOException e) {
        throw new RuntimeException("Cannot read " + file + ": " + e.getMessage(), e);
      }
      return this;
    }

    /** Sets an external variable whose value is Jsonnet code read from a file. */
    public Evaluation extCodeFile(String name, Path file) {
      try {
        extVars.put(name, Files.readString(file));
      } catch (IOException e) {
        throw new RuntimeException("Cannot read " + file + ": " + e.getMessage(), e);
      }
      return this;
    }

    public Evaluation tlaStr(String name, String value) {
      tlaVars.put(name, "\"" + escapeJson(value) + "\"");
      return this;
    }

    public Evaluation tlaCode(String name, String code) {
      tlaVars.put(name, code);
      return this;
    }

    /** Sets a top-level argument whose value is the string contents of a file. */
    public Evaluation tlaStrFile(String name, Path file) {
      try {
        tlaVars.put(name, "\"" + escapeJson(Files.readString(file)) + "\"");
      } catch (IOException e) {
        throw new RuntimeException("Cannot read " + file + ": " + e.getMessage(), e);
      }
      return this;
    }

    /** Sets a top-level argument whose value is Jsonnet code read from a file. */
    public Evaluation tlaCodeFile(String name, Path file) {
      try {
        tlaVars.put(name, Files.readString(file));
      } catch (IOException e) {
        throw new RuntimeException("Cannot read " + file + ": " + e.getMessage(), e);
      }
      return this;
    }

    public Evaluation addJpath(Path path) {
      evalJpaths.add(path);
      return this;
    }

    /** When true, strings are returned without JSON quotes. */
    public Evaluation stringOutput(boolean enabled) {
      this.stringOutput = enabled;
      return this;
    }

    /** Evaluates a Jsonnet source string, returning the JSON result. */
    public String evaluateSnippet(String code) {
      return evaluateSnippet(code, "<eval>");
    }

    /** Evaluates a Jsonnet source string with a given filename for error messages. */
    public String evaluateSnippet(String code, String fileName) {
      Source source = Source.newBuilder(JsonnetLanguage.ID, code, fileName).buildLiteral();
      return evaluate(source, fileName, Path.of("").toAbsolutePath());
    }

    /** Evaluates a Jsonnet file, returning the JSON result. */
    public String evaluateFile(Path file) {
      try {
        Path realFile = file.toAbsolutePath().toRealPath();
        Source source = Source.newBuilder(JsonnetLanguage.ID, realFile.toFile()).build();
        return evaluate(source, realFile.getFileName().toString(), realFile.getParent());
      } catch (IOException e) {
        throw new RuntimeException("Cannot read " + file + ": " + e.getMessage(), e);
      }
    }

    private String evaluate(Source source, String fileName, Path baseDir) {
      JsonnetConfig config = new JsonnetConfig();
      extVars.forEach(config::putExtVar);
      tlaVars.forEach(config::putTlaVar);
      jpaths.forEach(config::addJpath);
      evalJpaths.forEach(config::addJpath);
      for (NativeRegistration nr : nativeRegistrations) {
        config.registerNative(nr.name(), nr.paramNames(), nr.body());
      }
      config.setBaseDir(baseDir);

      JsonnetConfig.set(config);
      JsonnetConfig.setCurrentFile(fileName);

      try (Context context =
          Context.newBuilder(JsonnetLanguage.ID).engine(engine).allowAllAccess(true).build()) {
        Value result = context.eval(source);
        return formatValue(result);
      } finally {
        JsonnetConfig.clear();
      }
    }

    private String formatValue(Value result) {
      if (stringOutput) {
        return result.isString() ? result.asString() : result.toString();
      }
      if (result.isNumber()) {
        double d = result.asDouble();
        if (d == (long) d) return String.valueOf((long) d);
        return String.valueOf(d);
      }
      if (result.isString()) return "\"" + result.asString() + "\"";
      if (result.isBoolean()) return String.valueOf(result.asBoolean());
      if (result.isNull()) return "null";
      return result.toString();
    }
  }

  private static String escapeJson(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}

package com.databricks.jsonnetjvm.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Runtime configuration passed from the CLI to the language. Holds ext vars, TLA vars, jpaths, and
 * caches. Set on a thread-local before evaluation begins.
 *
 * <p>Caches:
 *
 * <ul>
 *   <li>{@code importCache} — evaluated results of {@code import} expressions, keyed by canonical
 *       path
 *   <li>{@code extVarCache} — evaluated results of {@code std.extVar()}, keyed by variable name
 *   <li>{@code importStrCache} — file contents for {@code importstr}, keyed by canonical path
 * </ul>
 */
public class JsonnetConfig {
  private static final ThreadLocal<JsonnetConfig> CURRENT = new ThreadLocal<>();

  private final Map<String, String> extVars = new HashMap<>();
  private final Map<String, String> tlaVars = new HashMap<>();
  private final Map<Path, Object> importCache = new ConcurrentHashMap<>();
  private final Map<String, Object> extVarCache = new ConcurrentHashMap<>();
  private final Map<Path, String> importStrCache = new ConcurrentHashMap<>();
  private static final ThreadLocal<String> CURRENT_FILE = new ThreadLocal<>();
  private Path baseDir;
  private final List<Path> jpaths = new ArrayList<>();
  private final Map<String, NativeFunctionDef> customNatives = new HashMap<>();

  /** Definition for a custom native function registered via JsonnetEvaluator. */
  public record NativeFunctionDef(String[] paramNames, Function<Object[], Object> body) {}

  public static void setCurrentFile(String name) {
    CURRENT_FILE.set(name);
  }

  public static String getCurrentFile() {
    String f = CURRENT_FILE.get();
    return f != null ? f : "<stdin>";
  }

  public void putExtVar(String key, String jsonnetCode) {
    extVars.put(key, jsonnetCode);
  }

  public Map<String, String> getExtVars() {
    return extVars;
  }

  public void putTlaVar(String key, String jsonnetCode) {
    tlaVars.put(key, jsonnetCode);
  }

  public Map<String, String> getTlaVars() {
    return tlaVars;
  }

  public Map<Path, Object> getImportCache() {
    return importCache;
  }

  public Map<String, Object> getExtVarCache() {
    return extVarCache;
  }

  public Map<Path, String> getImportStrCache() {
    return importStrCache;
  }

  public Path getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(Path baseDir) {
    this.baseDir = baseDir;
  }

  public List<Path> getJpaths() {
    return jpaths;
  }

  public void addJpath(Path path) {
    jpaths.add(path);
  }

  public void registerNative(String name, String[] paramNames, Function<Object[], Object> body) {
    customNatives.put(name, new NativeFunctionDef(paramNames, body));
  }

  public Map<String, NativeFunctionDef> getCustomNatives() {
    return customNatives;
  }

  public static void set(JsonnetConfig config) {
    CURRENT.set(config);
  }

  public static JsonnetConfig get() {
    JsonnetConfig c = CURRENT.get();
    if (c == null) {
      c = new JsonnetConfig();
      CURRENT.set(c);
    }
    return c;
  }

  public static void clear() {
    CURRENT.remove();
  }
}

package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetConfig;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Implements the `import` keyword. Resolves the import path relative to the current file, parses
 * the imported file, evaluates it, and caches the result.
 */
public class JsonnetImportNode extends JsonnetExpressionNode {
  private final String importPath;
  private final String currentSourceName;
  private final JsonnetLanguage language;

  public JsonnetImportNode(String importPath, String currentSourceName, JsonnetLanguage language) {
    this.importPath = importPath;
    this.currentSourceName = currentSourceName;
    this.language = language;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doImport();
  }

  @TruffleBoundary
  private Object doImport() {
    Path resolved =
        resolveImportPath(currentSourceName, importPath, JsonnetConfig.get().getJpaths());
    if (resolved == null || !Files.exists(resolved)) {
      throw new JsonnetException("Couldn't import file: \"" + importPath + "\"", this);
    }

    Map<Path, Object> cache = JsonnetConfig.get().getImportCache();
    Path canonical;
    try {
      canonical = resolved.toRealPath();
    } catch (IOException e) {
      canonical = resolved;
    }

    Object cached = cache.get(canonical);
    if (cached != null) {
      return cached;
    }

    try {
      String displayName = computeRelativeName(resolved);
      // Use absolute path as source name so chained imports resolve correctly
      String absoluteName = resolved.toAbsolutePath().toString();
      String code = Files.readString(resolved);
      Source source = Source.newBuilder(JsonnetLanguage.ID, code, absoluteName).build();
      String prevFile = JsonnetConfig.getCurrentFile();
      try {
        JsonnetConfig.setCurrentFile(displayName);
        Object result = language.parseSource(source).call();
        cache.put(canonical, result);
        return result;
      } finally {
        JsonnetConfig.setCurrentFile(prevFile);
      }
    } catch (IOException e) {
      throw new JsonnetException(
          "Error reading import: " + importPath + ": " + e.getMessage(), this);
    }
  }

  /**
   * Resolve an import path by trying: 1) current file's directory, 2) each jpath in order. Returns
   * the first path that exists, or null if none found.
   */
  static Path resolveImportPath(String currentSourceName, String importPath, List<Path> jpaths) {
    Path currentFile = Path.of(currentSourceName);
    Path parent = currentFile.getParent();

    // Try relative to the current file's directory first
    Path candidate;
    if (parent != null) {
      candidate = parent.resolve(importPath);
    } else {
      candidate = Path.of(importPath);
    }
    if (Files.exists(candidate)) {
      return candidate;
    }

    // Then try each jpath search directory
    for (Path jp : jpaths) {
      candidate = jp.resolve(importPath);
      if (Files.exists(candidate)) {
        return candidate;
      }
    }

    // Return the original resolution even though it doesn't exist (for error
    // messages)
    if (parent != null) {
      return parent.resolve(importPath);
    }
    return Path.of(importPath);
  }

  private static String computeRelativeName(Path resolved) {
    Path baseDir = JsonnetConfig.get().getBaseDir();
    if (baseDir == null) {
      baseDir = Path.of("").toAbsolutePath();
    }
    try {
      return baseDir.relativize(resolved.toAbsolutePath()).toString();
    } catch (IllegalArgumentException e) {
      return resolved.toString();
    }
  }
}

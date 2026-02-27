package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetConfig;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Implements the `importstr` keyword. Resolves the path relative to the current file and reads the
 * file as a string. Results are cached by canonical path in {@link
 * JsonnetConfig#getImportStrCache()}.
 */
public class JsonnetImportStrNode extends JsonnetExpressionNode {
  private final String importPath;
  private final String currentSourceName;

  public JsonnetImportStrNode(String importPath, String currentSourceName) {
    this.importPath = importPath;
    this.currentSourceName = currentSourceName;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doImportStr();
  }

  @TruffleBoundary
  private Object doImportStr() {
    Path resolved =
        JsonnetImportNode.resolveImportPath(
            currentSourceName, importPath, JsonnetConfig.get().getJpaths());
    if (resolved == null || !Files.exists(resolved)) {
      throw new JsonnetException("Couldn't import file: \"" + importPath + "\"", this);
    }

    Path canonical;
    try {
      canonical = resolved.toRealPath();
    } catch (IOException e) {
      canonical = resolved;
    }

    Map<Path, String> cache = JsonnetConfig.get().getImportStrCache();
    String cached = cache.get(canonical);
    if (cached != null) {
      return cached;
    }

    try {
      String content = Files.readString(resolved);
      cache.put(canonical, content);
      return content;
    } catch (IOException e) {
      throw new JsonnetException(
          "Error reading importstr: " + importPath + ": " + e.getMessage(), this);
    }
  }
}

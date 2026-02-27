package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;

public class JsonnetException extends AbstractTruffleException {
  public JsonnetException(String message) {
    super(message);
  }

  public JsonnetException(String message, Node location) {
    super(message, location);
  }

  @TruffleBoundary
  public static String typeName(Object x) {
    if (x == null || x instanceof JNull) return "null";
    if (x instanceof Boolean) return "boolean";
    if (x instanceof Double) return "number";
    if (x instanceof String) return "string";
    if (x instanceof JArray) return "array";
    if (x instanceof JObject) return "object";
    if (x instanceof JFunction) return "function";
    return x.getClass().getSimpleName();
  }
}

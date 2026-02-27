package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JsonnetStrings;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetLessThanNode extends JsonnetBinaryNode {

  @Specialization
  protected boolean doDouble(double left, double right) {
    return left < right;
  }

  @TruffleBoundary
  @Specialization
  protected boolean doString(String left, String right) {
    return JsonnetStrings.compareByCodepoint(left, right) < 0;
  }

  @TruffleBoundary
  @Specialization
  protected boolean doArray(JArray left, JArray right) {
    return compareArrays(left, right) < 0;
  }

  @TruffleBoundary
  static int compareArrays(JArray a, JArray b) {
    int len = Math.min(a.size(), b.size());
    for (int i = 0; i < len; i++) {
      int cmp = compareElements(a.get(i), b.get(i));
      if (cmp != 0) return cmp;
    }
    return Integer.compare(a.size(), b.size());
  }

  static int compareElements(Object a, Object b) {
    if (a instanceof Double da && b instanceof Double db) {
      return Double.compare(da, db);
    }
    if (a instanceof String sa && b instanceof String sb) {
      return JsonnetStrings.compareByCodepoint(sa, sb);
    }
    if (a instanceof Boolean ba && b instanceof Boolean bb) {
      return Boolean.compare(ba, bb);
    }
    if (a instanceof JArray ja && b instanceof JArray jb) {
      return compareArrays(ja, jb);
    }
    return 0;
  }
}

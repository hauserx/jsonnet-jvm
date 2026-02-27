package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonnetAddNode extends JsonnetBinaryNode {

  @Specialization
  protected double doDouble(double left, double right) {
    return left + right;
  }

  @Specialization
  protected String doString(String left, String right) {
    return concat(left, right);
  }

  @Specialization
  protected String doBooleanString(boolean left, String right) {
    return concat(left, right);
  }

  @Specialization
  protected String doStringBoolean(String left, boolean right) {
    return concat(left, right);
  }

  @Specialization
  protected JArray doArrayConcat(JArray left, JArray right) {
    return concatArrays(left, right);
  }

  @Specialization
  protected JObject doObjectMerge(JObject left, JObject right) {
    return mergeObjects(left, right);
  }

  @Specialization
  protected String doStringLeft(String left, Object right) {
    return concat(left, right);
  }

  @Specialization
  protected String doStringRight(Object left, String right) {
    return concat(left, right);
  }

  @TruffleBoundary
  static JObject mergeObjects(JObject left, JObject right) {
    return JObject.merge(left, right);
  }

  @TruffleBoundary
  private JArray concatArrays(JArray left, JArray right) {
    List<Object> items = new ArrayList<>(left.size() + right.size());
    for (int i = 0; i < left.size(); i++) {
      items.add(left.get(i));
    }
    for (int i = 0; i < right.size(); i++) {
      items.add(right.get(i));
    }
    return new JGenericArray(items);
  }

  @TruffleBoundary
  private String concat(Object left, Object right) {
    return formatForConcat(left) + formatForConcat(right);
  }

  private static String formatForConcat(Object value) {
    if (value instanceof Double d) {
      if (d == Math.floor(d) && !Double.isInfinite(d)) {
        long l = d.longValue();
        return Long.toString(l);
      }
      return Double.toString(d);
    }
    if (value instanceof JNull) {
      return "null";
    }
    return String.valueOf(value);
  }
}

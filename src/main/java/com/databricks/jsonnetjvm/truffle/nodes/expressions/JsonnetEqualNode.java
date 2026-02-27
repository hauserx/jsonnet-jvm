package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetEqualNode extends JsonnetBinaryNode {

  @Specialization
  protected boolean doDouble(double left, double right) {
    return left == right;
  }

  @Specialization
  protected boolean doBoolean(boolean left, boolean right) {
    return left == right;
  }

  @Specialization
  protected boolean doString(String left, String right) {
    return left.equals(right);
  }

  @Specialization
  protected boolean doNull(JNull left, JNull right) {
    return true;
  }

  @Specialization
  @TruffleBoundary
  protected boolean doGeneric(Object left, Object right) {
    try {
      return deepEquals(left, right);
    } catch (JsonnetException e) {
      throw (e.getLocation() != null) ? e : new JsonnetException(e.getMessage(), this);
    }
  }

  @TruffleBoundary
  public static boolean deepEquals(Object a, Object b) {
    if (a == b) return true;
    if (a == null || b == null) return false;

    // null singleton
    if (a instanceof JNull && b instanceof JNull) return true;
    if (a instanceof JNull || b instanceof JNull) return false;

    // numbers
    if (a instanceof Double && b instanceof Double) {
      return ((Double) a).doubleValue() == ((Double) b).doubleValue();
    }

    // booleans
    if (a instanceof Boolean && b instanceof Boolean) {
      return a.equals(b);
    }

    // strings
    if (a instanceof String && b instanceof String) {
      return a.equals(b);
    }

    // arrays
    if (a instanceof JArray && b instanceof JArray) {
      JArray arrA = (JArray) a;
      JArray arrB = (JArray) b;
      if (arrA.size() != arrB.size()) return false;
      for (int i = 0; i < arrA.size(); i++) {
        if (!deepEquals(arrA.get(i), arrB.get(i))) return false;
      }
      return true;
    }

    // objects
    if (a instanceof JObject && b instanceof JObject) {
      JObject objA = (JObject) a;
      JObject objB = (JObject) b;
      var keysA = objA.getFieldNames();
      var keysB = objB.getFieldNames();
      if (!keysA.equals(keysB)) return false;
      for (String key : keysA) {
        if (!deepEquals(objA.getField(key), objB.getField(key))) return false;
      }
      return true;
    }

    // functions are not comparable
    if (a instanceof JFunction || b instanceof JFunction) {
      throw new JsonnetException("cannot test equality of functions");
    }

    return a.equals(b);
  }
}

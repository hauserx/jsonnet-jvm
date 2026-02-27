package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetIndexNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode targetNode;
  @Child private JsonnetExpressionNode indexNode;

  public JsonnetIndexNode(JsonnetExpressionNode targetNode, JsonnetExpressionNode indexNode) {
    this.targetNode = targetNode;
    this.indexNode = indexNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object target = targetNode.executeGeneric(frame);
    Object index = indexNode.executeGeneric(frame);
    return doIndex(target, index);
  }

  @TruffleBoundary
  private Object doIndex(Object target, Object index) {
    if (target instanceof JArray arr) {
      if (!(index instanceof Double)) {
        throw new JsonnetException(
            "array index must be a number, got " + JsonnetException.typeName(index), this);
      }
      double d = (Double) index;
      if (d != Math.floor(d) || d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
        throw new JsonnetException("index value is not a valid integer", this);
      }
      int idx = (int) d;
      if (idx < 0) idx += arr.size();
      if (idx < 0 || idx >= arr.size()) {
        throw new JsonnetException(
            "array index " + (int) d + " out of range, size " + arr.size(), this);
      }
      return arr.get(idx);
    } else if (target instanceof JObject obj) {
      if (!(index instanceof String key)) {
        throw new JsonnetException(
            "attempted to index a object with " + JsonnetException.typeName(index), this);
      }
      Object value = obj.getField(key);
      if (value == null) {
        throw new JsonnetException("Field does not exist: " + key, this);
      }
      return value;
    } else if (target instanceof String str) {
      if (!(index instanceof Double)) {
        throw new JsonnetException(
            "attempted to index a string with " + JsonnetException.typeName(index), this);
      }
      int idx = (int) ((double) index);
      int cpCount = str.codePointCount(0, str.length());
      if (idx < 0) idx += cpCount;
      if (idx < 0 || idx >= cpCount) {
        throw new JsonnetException("string bounds error", this);
      }
      int cpIndex = str.offsetByCodePoints(0, idx);
      int cp = str.codePointAt(cpIndex);
      return new String(Character.toChars(cp));
    }
    throw new JsonnetException(
        "attempted to index a "
            + JsonnetException.typeName(target)
            + " with "
            + JsonnetException.typeName(index),
        this);
  }
}

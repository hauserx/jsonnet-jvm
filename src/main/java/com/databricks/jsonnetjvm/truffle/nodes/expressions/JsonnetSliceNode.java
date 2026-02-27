package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import java.util.ArrayList;
import java.util.List;

/** Implements array/string slicing: expr[start:end:step]. */
public class JsonnetSliceNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode targetNode;
  @Child private JsonnetExpressionNode startNode;
  @Child private JsonnetExpressionNode endNode;
  @Child private JsonnetExpressionNode stepNode;

  public JsonnetSliceNode(
      JsonnetExpressionNode target,
      JsonnetExpressionNode start,
      JsonnetExpressionNode end,
      JsonnetExpressionNode step) {
    this.targetNode = target;
    this.startNode = start;
    this.endNode = end;
    this.stepNode = step;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object target = targetNode.executeGeneric(frame);
    Object startVal = startNode != null ? startNode.executeGeneric(frame) : null;
    Object endVal = endNode != null ? endNode.executeGeneric(frame) : null;
    Object stepVal = stepNode != null ? stepNode.executeGeneric(frame) : null;
    return doSlice(target, startVal, endVal, stepVal);
  }

  @TruffleBoundary
  private Object doSlice(Object target, Object startVal, Object endVal, Object stepVal) {
    int step = stepVal != null ? ((Number) stepVal).intValue() : 1;
    if (step == 0) {
      throw new JsonnetException("Slice step cannot be zero", this);
    }

    if (target instanceof JArray arr) {
      int len = arr.size();
      int start = resolveIndex(startVal, 0, len);
      int end = resolveIndex(endVal, len, len);

      List<Object> result = new ArrayList<>();
      for (int i = start; i < end && i < len; i += step) {
        if (i >= 0) {
          result.add(arr.get(i));
        }
      }
      return new JGenericArray(result);
    } else if (target instanceof String str) {
      int len = str.codePointCount(0, str.length());
      int start = resolveIndex(startVal, 0, len);
      int end = resolveIndex(endVal, len, len);

      StringBuilder sb = new StringBuilder();
      int cpIndex = 0;
      int offset = 0;
      while (offset < str.length() && cpIndex < end && cpIndex < len) {
        int cp = str.codePointAt(offset);
        if (cpIndex >= start && (cpIndex - start) % step == 0) {
          sb.appendCodePoint(cp);
        }
        offset += Character.charCount(cp);
        cpIndex++;
      }
      return sb.toString();
    }

    throw new JsonnetException(
        "Cannot slice " + (target == null ? "null" : target.getClass().getSimpleName()), this);
  }

  private static int resolveIndex(Object val, int defaultVal, int len) {
    if (val == null) return defaultVal;
    int idx = ((Number) val).intValue();
    if (idx < 0) {
      idx = len + idx;
      if (idx < 0) idx = 0;
    }
    return idx;
  }
}

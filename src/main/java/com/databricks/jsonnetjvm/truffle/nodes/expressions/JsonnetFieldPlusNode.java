package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements +: field semantics: if super has the field, returns super.field + val; otherwise
 * returns just val.
 */
public class JsonnetFieldPlusNode extends JsonnetExpressionNode {
  private final String staticFieldName;
  private final int computedKeySlot;
  @Child private JsonnetExpressionNode valueExpr;

  public JsonnetFieldPlusNode(String staticFieldName, JsonnetExpressionNode valueExpr) {
    this.staticFieldName = staticFieldName;
    this.computedKeySlot = -1;
    this.valueExpr = valueExpr;
  }

  public JsonnetFieldPlusNode(int computedKeySlot, JsonnetExpressionNode valueExpr) {
    this.staticFieldName = null;
    this.computedKeySlot = computedKeySlot;
    this.valueExpr = valueExpr;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    String fieldName = staticFieldName;
    if (fieldName == null) {
      fieldName = (String) frame.getObject(computedKeySlot);
    }
    Object val = valueExpr.executeGeneric(frame);
    return doPlus(fieldName, val);
  }

  @TruffleBoundary
  private Object doPlus(String fieldName, Object val) {
    JObject self = JObject.getCurrentSelf();
    int superMaxLayer = JObject.getCurrentSuperMaxLayer();
    if (self == null || superMaxLayer < 0) {
      return val;
    }
    Object superVal = self.evaluateField(fieldName, self, superMaxLayer);
    if (superVal == null) {
      return val;
    }
    return add(superVal, val);
  }

  @TruffleBoundary
  static Object add(Object left, Object right) {
    if (left instanceof Double l && right instanceof Double r) return l + r;
    if (left instanceof String || right instanceof String)
      return stringify(left) + stringify(right);
    if (left instanceof JArray la && right instanceof JArray ra) {
      List<Object> items = new ArrayList<>(la.size() + ra.size());
      for (int i = 0; i < la.size(); i++) items.add(la.get(i));
      for (int i = 0; i < ra.size(); i++) items.add(ra.get(i));
      return new JGenericArray(items);
    }
    if (left instanceof JObject lo && right instanceof JObject ro) return JObject.merge(lo, ro);
    throw new JsonnetException(
        "binary operator + not applicable to types "
            + JsonnetException.typeName(left)
            + " and "
            + JsonnetException.typeName(right));
  }

  private static String stringify(Object val) {
    if (val instanceof Double d) {
      if (d == Math.floor(d) && !Double.isInfinite(d)) return Long.toString(d.longValue());
      return Double.toString(d);
    }
    if (val instanceof JNull) return "null";
    return String.valueOf(val);
  }
}

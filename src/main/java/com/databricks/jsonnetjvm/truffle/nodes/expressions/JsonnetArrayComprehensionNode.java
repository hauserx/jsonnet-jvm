package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.ArrayBuilder;
import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetArrayComprehensionNode extends JsonnetExpressionNode {
  @Child private JsonnetToValNode resultToValNode;
  @Child private JsonnetExpressionNode filterNode;
  private final int[] slots;
  @Children private final JsonnetExpressionNode[] listNodes;

  public JsonnetArrayComprehensionNode(
      JsonnetExpressionNode resultExpr,
      int[] slots,
      JsonnetExpressionNode[] listNodes,
      JsonnetExpressionNode filterNode) {
    this.resultToValNode = JsonnetToValNodeGen.create(resultExpr);
    this.slots = slots;
    this.listNodes = listNodes;
    this.filterNode = filterNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doEvaluate(frame.materialize());
  }

  @TruffleBoundary
  private JArray doEvaluate(MaterializedFrame frame) {
    JArray[] lists = new JArray[listNodes.length];
    for (int i = 0; i < listNodes.length; i++) {
      Object res = listNodes[i].executeGeneric(frame);
      if (!(res instanceof JArray)) {
        throw new JsonnetException(
            "In comprehension, can only iterate over array, not " + JsonnetException.typeName(res),
            this);
      }
      lists[i] = (JArray) res;
    }

    ArrayBuilder builder = new ArrayBuilder(16);
    evaluateNested(frame, 0, lists, builder);
    return builder.build();
  }

  private void evaluateNested(
      MaterializedFrame frame, int depth, JArray[] lists, ArrayBuilder builder) {
    if (depth == lists.length) {
      if (filterNode != null) {
        Object filterResult = filterNode.executeGeneric(frame);
        if (!(filterResult instanceof Boolean)) {
          throw new JsonnetException(
              "Condition must be boolean, got " + JsonnetException.typeName(filterResult),
              JsonnetArrayComprehensionNode.this);
        }
        if (!(Boolean) filterResult) {
          return;
        }
      }
      Object result = resultToValNode.executeGeneric(frame);
      // Force lazy values (like object fields) since the shared frame will be
      // mutated on the next iteration
      builder.add(forceValue(result));
      return;
    }

    JArray list = lists[depth];
    for (int i = 0; i < list.size(); i++) {
      frame.setObject(slots[depth], list.get(i));
      evaluateNested(frame, depth + 1, lists, builder);
    }
  }

  /**
   * Force all lazy parts of a value to capture the current frame state. Objects created inside
   * comprehensions have field suppliers that reference the shared loop frame; forcing them here
   * ensures each iteration captures its own values.
   */
  private static Object forceValue(Object value) {
    if (value instanceof JObject obj) {
      JObject forced = new JObject();
      for (String name : obj.getFieldNames()) {
        Object fieldVal = obj.getField(name);
        forced.addField(name, () -> fieldVal);
      }
      // Preserve captured dollar
      if (obj.getCapturedDollar() != null) {
        forced.setCapturedDollar(obj.getCapturedDollar());
      }
      return forced;
    }
    return value;
  }
}

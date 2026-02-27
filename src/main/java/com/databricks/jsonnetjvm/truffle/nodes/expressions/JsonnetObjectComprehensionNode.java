package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Object comprehension: { [keyExpr]: valueExpr for id in listExpr ... }. Supports multiple for/if
 * clauses via nested iteration, object-locals that reference loop variables, and +: field override
 * syntax.
 */
public class JsonnetObjectComprehensionNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode keyNode;
  @Child private JsonnetExpressionNode valueNode;
  @Child private JsonnetExpressionNode filterNode;
  private final int[] slots;
  @Children private final JsonnetExpressionNode[] listNodes;
  private final boolean hasPlus;

  private final String[] localNames;
  @Children private final JsonnetExpressionNode[] localValueNodes;

  public JsonnetObjectComprehensionNode(
      JsonnetExpressionNode keyNode,
      JsonnetExpressionNode valueNode,
      int[] slots,
      JsonnetExpressionNode[] listNodes,
      JsonnetExpressionNode filterNode,
      boolean hasPlus,
      String[] localNames,
      JsonnetExpressionNode[] localValueNodes) {
    this.keyNode = keyNode;
    this.valueNode = valueNode;
    this.slots = slots;
    this.listNodes = listNodes;
    this.filterNode = filterNode;
    this.hasPlus = hasPlus;
    this.localNames = localNames;
    this.localValueNodes = localValueNodes;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doEvaluate(frame.materialize());
  }

  @TruffleBoundary
  private JObject doEvaluate(MaterializedFrame frame) {
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

    JObject result = new JObject();

    evaluateNested(frame, 0, lists, result);

    JObject dollar = JObject.getCurrentDollar();
    if (dollar != null) {
      result.setCapturedDollar(dollar);
    }

    return result;
  }

  private void evaluateNested(MaterializedFrame frame, int depth, JArray[] lists, JObject result) {
    if (depth == lists.length) {
      if (filterNode != null) {
        Object filterResult = filterNode.executeGeneric(frame);
        if (!(filterResult instanceof Boolean)) {
          throw new JsonnetException(
              "Condition must be boolean, got " + JsonnetException.typeName(filterResult),
              JsonnetObjectComprehensionNode.this);
        }
        if (!(Boolean) filterResult) {
          return;
        }
      }

      Object keyVal = keyNode.executeGeneric(frame);
      if (keyVal == null || keyVal instanceof JNull) {
        return;
      }
      if (!(keyVal instanceof String)) {
        throw new JsonnetException(
            "Field name must be a string, got " + JsonnetException.typeName(keyVal),
            JsonnetObjectComprehensionNode.this);
      }
      String key = (String) keyVal;

      if (!hasPlus && result.hasFieldAll(key)) {
        throw new JsonnetException(
            "Duplicate key " + key + " in evaluated object comprehension.",
            JsonnetObjectComprehensionNode.this);
      }
      final Object[] capturedLoopVars = new Object[slots.length];
      for (int s = 0; s < slots.length; s++) {
        capturedLoopVars[s] = frame.getObject(slots[s]);
      }

      for (int i = 0; i < localNames.length; i++) {
        final int idx = i;
        final Object[] localCaptured = capturedLoopVars.clone();
        result.addLocal(
            localNames[idx],
            () -> {
              for (int s = 0; s < localCaptured.length; s++) {
                frame.setObject(slots[s], localCaptured[s]);
              }
              return localValueNodes[idx].executeGeneric(frame);
            });
      }

      if (hasPlus) {
        result.addField(
            key,
            () -> {
              for (int s = 0; s < capturedLoopVars.length; s++) {
                frame.setObject(slots[s], capturedLoopVars[s]);
              }
              Object val = valueNode.executeGeneric(frame);
              return applyPlus(key, val);
            });
      } else {
        result.addField(
            key,
            () -> {
              for (int s = 0; s < capturedLoopVars.length; s++) {
                frame.setObject(slots[s], capturedLoopVars[s]);
              }
              return valueNode.executeGeneric(frame);
            });
      }
      return;
    }

    JArray list = lists[depth];
    for (int i = 0; i < list.size(); i++) {
      frame.setObject(slots[depth], list.get(i));
      evaluateNested(frame, depth + 1, lists, result);
    }
  }

  @TruffleBoundary
  private static Object applyPlus(String fieldName, Object val) {
    JObject self = JObject.getCurrentSelf();
    int superMaxLayer = JObject.getCurrentSuperMaxLayer();
    if (self == null || superMaxLayer < 0) {
      return val;
    }
    Object superVal = self.evaluateField(fieldName, self, superMaxLayer);
    if (superVal == null) {
      return val;
    }
    return JsonnetFieldPlusNode.add(superVal, val);
  }
}

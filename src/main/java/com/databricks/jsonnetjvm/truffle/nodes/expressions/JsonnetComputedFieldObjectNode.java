package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * An object with a single computed field and optional object-local bindings, but no comprehension.
 * Handles: { local x = ..., [keyExpr]: valExpr }
 */
public class JsonnetComputedFieldObjectNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode keyNode;
  @Child private JsonnetExpressionNode valueNode;
  private final String[] localNames;
  @Children private final JsonnetExpressionNode[] localValueNodes;

  public JsonnetComputedFieldObjectNode(
      JsonnetExpressionNode keyNode,
      JsonnetExpressionNode valueNode,
      String[] localNames,
      JsonnetExpressionNode[] localValueNodes) {
    this.keyNode = keyNode;
    this.valueNode = valueNode;
    this.localNames = localNames;
    this.localValueNodes = localValueNodes;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doEvaluate(frame.materialize());
  }

  @TruffleBoundary
  private JObject doEvaluate(MaterializedFrame frame) {
    JObject result = new JObject();

    for (int i = 0; i < localNames.length; i++) {
      final int idx = i;
      result.addLocal(localNames[i], () -> localValueNodes[idx].executeGeneric(frame));
    }

    Object keyVal = keyNode.executeGeneric(frame);
    if (keyVal == null || keyVal instanceof JNull) {
      // null key means skip the field
    } else {
      String key = (String) keyVal;
      result.addField(key, () -> valueNode.executeGeneric(frame));
    }

    JObject dollar = JObject.getCurrentDollar();
    if (dollar != null) {
      result.setCapturedDollar(dollar);
    }

    return result;
  }
}

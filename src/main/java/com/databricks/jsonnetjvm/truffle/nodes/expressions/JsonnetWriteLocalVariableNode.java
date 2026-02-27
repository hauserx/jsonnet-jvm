package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetWriteLocalVariableNode extends JsonnetExpressionNode {
  private final int slot;
  @Child private JsonnetExpressionNode valueNode;

  public JsonnetWriteLocalVariableNode(int slot, JsonnetExpressionNode valueNode) {
    this.slot = slot;
    this.valueNode = valueNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object value = valueNode.executeGeneric(frame);
    frame.setObject(slot, value);
    return value;
  }
}

package com.databricks.jsonnetjvm.truffle.nodes.literals;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetBooleanLiteralNode extends JsonnetExpressionNode {
  private final boolean value;

  public JsonnetBooleanLiteralNode(boolean value) {
    this.value = value;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return value;
  }
}

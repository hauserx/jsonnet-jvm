package com.databricks.jsonnetjvm.truffle.nodes.literals;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetNumberLiteralNode extends JsonnetExpressionNode {
  private final double value;

  public JsonnetNumberLiteralNode(double value) {
    this.value = value;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return value;
  }
}

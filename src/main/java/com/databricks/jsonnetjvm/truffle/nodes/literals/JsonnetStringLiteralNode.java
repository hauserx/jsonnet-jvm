package com.databricks.jsonnetjvm.truffle.nodes.literals;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetStringLiteralNode extends JsonnetExpressionNode {
  private final String value;

  public JsonnetStringLiteralNode(String value) {
    this.value = value;
  }

  @Override
  public String executeString(VirtualFrame frame) {
    return value;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return value;
  }
}

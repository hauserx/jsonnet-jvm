package com.databricks.jsonnetjvm.truffle.nodes.literals;

import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.Val;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetNullLiteralNode extends JsonnetExpressionNode {
  @Override
  public Val executeGeneric(VirtualFrame frame) {
    return JNull.INSTANCE;
  }
}

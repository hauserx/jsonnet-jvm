package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("operandNode")
public abstract class JsonnetUnaryNotNode extends JsonnetExpressionNode {

  @Specialization
  protected boolean doBoolean(boolean operand) {
    return !operand;
  }
}

package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("operandNode")
public abstract class JsonnetUnaryMinusNode extends JsonnetExpressionNode {

  @Specialization
  protected double doDouble(double operand) {
    return -operand;
  }
}

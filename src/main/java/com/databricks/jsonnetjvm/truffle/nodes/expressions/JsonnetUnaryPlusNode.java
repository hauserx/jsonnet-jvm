package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("operand")
public abstract class JsonnetUnaryPlusNode extends JsonnetExpressionNode {

  @Specialization
  protected double doDouble(double operand) {
    return operand;
  }
}

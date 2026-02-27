package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("operand")
public abstract class JsonnetUnaryBitNotNode extends JsonnetExpressionNode {

  @Specialization
  protected double doDouble(double operand) {
    if (Math.abs(operand) >= (1L << 53)) {
      throw new JsonnetException(
          "numeric value outside safe integer range for bitwise operation", this);
    }
    return ~(long) operand;
  }
}

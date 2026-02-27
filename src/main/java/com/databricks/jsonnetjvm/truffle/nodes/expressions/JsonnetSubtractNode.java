package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetSubtractNode extends JsonnetBinaryNode {

  @Specialization
  protected double doDouble(double left, double right) {
    return left - right;
  }
}

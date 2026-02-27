package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetMultiplyNode extends JsonnetBinaryNode {

  @Specialization
  protected double doDouble(double left, double right) {
    double result = left * right;
    if (Double.isInfinite(result) && !Double.isInfinite(left) && !Double.isInfinite(right)) {
      throw new JsonnetException("overflow", this);
    }
    return result;
  }
}

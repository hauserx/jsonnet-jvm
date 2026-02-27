package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetShiftLeftNode extends JsonnetBinaryNode {

  @Specialization
  protected double doDouble(double left, double right) {
    if (Math.abs(left) >= (1L << 53)) {
      throw new JsonnetException(
          "numeric value outside safe integer range for bitwise operation", this);
    }
    if (Math.abs(right) >= (1L << 53)) {
      throw new JsonnetException(
          "numeric value outside safe integer range for bitwise operation", this);
    }
    if (right < 0) throw new JsonnetException("shift by negative exponent", this);
    long result = (long) left << (long) right;
    if (result > Long.MAX_VALUE / 2 || result < Long.MIN_VALUE / 2) {
      throw new JsonnetException(
          "numeric value outside safe integer range for bitwise operation", this);
    }
    return result;
  }
}

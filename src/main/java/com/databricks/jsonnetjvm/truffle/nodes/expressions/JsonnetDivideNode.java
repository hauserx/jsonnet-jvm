package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetDivideNode extends JsonnetBinaryNode {

  @Specialization
  protected double doDouble(double left, double right) {
    if (right == 0) throw new JsonnetException("division by zero", this);
    return left / right;
  }
}

package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.stdlib.JsonnetFormat;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetModuloNode extends JsonnetBinaryNode {

  @Specialization
  protected double doDouble(double left, double right) {
    if (right == 0) throw new JsonnetException("division by zero", this);
    return left % right;
  }

  @TruffleBoundary
  @Specialization
  protected Object doStringFormat(String left, Object right) {
    try {
      return JsonnetFormat.apply(left, right);
    } catch (JsonnetException e) {
      throw (e.getLocation() != null) ? e : new JsonnetException(e.getMessage(), this);
    }
  }
}

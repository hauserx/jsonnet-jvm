package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JsonnetStrings;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetGreaterEqualNode extends JsonnetBinaryNode {

  @Specialization
  protected boolean doDouble(double left, double right) {
    return left >= right;
  }

  @TruffleBoundary
  @Specialization
  protected boolean doString(String left, String right) {
    return JsonnetStrings.compareByCodepoint(left, right) >= 0;
  }

  @TruffleBoundary
  @Specialization
  protected boolean doArray(JArray left, JArray right) {
    return JsonnetLessThanNode.compareArrays(left, right) >= 0;
  }
}

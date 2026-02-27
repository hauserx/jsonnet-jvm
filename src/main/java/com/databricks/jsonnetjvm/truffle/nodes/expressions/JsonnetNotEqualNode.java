package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JNull;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetNotEqualNode extends JsonnetBinaryNode {

  @Specialization
  protected boolean doDouble(double left, double right) {
    return left != right;
  }

  @Specialization
  protected boolean doBoolean(boolean left, boolean right) {
    return left != right;
  }

  @Specialization
  protected boolean doString(String left, String right) {
    return !left.equals(right);
  }

  @Specialization
  protected boolean doNull(JNull left, JNull right) {
    return false;
  }

  @Specialization
  @TruffleBoundary
  protected boolean doGeneric(Object left, Object right) {
    return !JsonnetEqualNode.deepEquals(left, right);
  }
}

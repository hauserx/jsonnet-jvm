package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Reads a function argument from the call arguments array. Arguments[0] is the closure frame, so
 * user-visible arguments start at index 1.
 */
public class JsonnetArgReadNode extends JsonnetExpressionNode {
  private final int index;
  private final String paramName;

  public JsonnetArgReadNode(int index, String paramName) {
    this.index = index;
    this.paramName = paramName;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object[] args = frame.getArguments();
    int actualIndex = index + 1;
    if (actualIndex < args.length) {
      Object val = args[actualIndex];
      if (val == JFunction.UNSET_ARG) {
        throwUnbound();
      }
      return val;
    }
    throwUnbound();
    return null;
  }

  @TruffleBoundary
  private void throwUnbound() {
    throw new JsonnetException("Function parameter " + paramName + " not bound in call", this);
  }
}

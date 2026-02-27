package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Resolves a function parameter: uses the provided argument if present, otherwise wraps the default
 * expression in a lazy thunk. This enables mutually recursive default arguments (e.g.
 * function(a=[1,b[1]], b=[a[0],2])).
 */
public class JsonnetParamDefaultNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode defaultExpr;
  private final int argIndex;
  private final String name;

  public JsonnetParamDefaultNode(int argIndex, String name, JsonnetExpressionNode defaultExpr) {
    this.argIndex = argIndex;
    this.name = name;
    this.defaultExpr = defaultExpr;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object[] args = frame.getArguments();
    int idx = argIndex + 1;
    if (idx < args.length && args[idx] != JFunction.UNSET_ARG) {
      Object val = args[idx];
      if (val instanceof JsonnetThunk thunk) {
        return thunk.force();
      }
      return val;
    }
    MaterializedFrame mf = frame.materialize();
    return JsonnetThunk.withSelfCapture(() -> defaultExpr.executeGeneric(mf));
  }
}

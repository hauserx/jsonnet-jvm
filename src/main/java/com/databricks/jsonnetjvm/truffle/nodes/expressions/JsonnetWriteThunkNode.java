package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Writes a thunk (lazy value) to a frame slot instead of eagerly evaluating the expression. The
 * thunk captures the materialized frame and the value node, deferring evaluation until the variable
 * is read.
 */
public class JsonnetWriteThunkNode extends JsonnetExpressionNode {
  private final int slot;
  @Child private JsonnetExpressionNode valueNode;

  public JsonnetWriteThunkNode(int slot, JsonnetExpressionNode valueNode) {
    this.slot = slot;
    this.valueNode = valueNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    MaterializedFrame mf = frame.materialize();
    JsonnetThunk thunk = createThunk(mf);
    frame.setObject(slot, thunk);
    return thunk;
  }

  @TruffleBoundary
  private JsonnetThunk createThunk(MaterializedFrame mf) {
    JsonnetExpressionNode node = valueNode;
    return JsonnetThunk.withSelfCapture(() -> node.executeGeneric(mf));
  }
}

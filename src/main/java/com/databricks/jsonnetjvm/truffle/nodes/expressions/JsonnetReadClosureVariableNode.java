package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Reads a variable from a closure (outer) frame. The closure frame reference is stored in a known
 * slot in the current frame. Transparently forces thunks.
 */
public class JsonnetReadClosureVariableNode extends JsonnetExpressionNode {
  private final int closureFrameSlot; // slot in current frame holding the MaterializedFrame
  private final int variableSlot; // slot in the closure frame holding the variable
  private final String name;

  public JsonnetReadClosureVariableNode(int closureFrameSlot, int variableSlot, String name) {
    this.closureFrameSlot = closureFrameSlot;
    this.variableSlot = variableSlot;
    this.name = name;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    MaterializedFrame closureFrame = (MaterializedFrame) frame.getObject(closureFrameSlot);
    return readFromClosure(closureFrame);
  }

  @TruffleBoundary
  private Object readFromClosure(MaterializedFrame closureFrame) {
    Object value = closureFrame.getObject(variableSlot);
    if (value instanceof JsonnetThunk thunk) {
      Object forced = thunk.force();
      closureFrame.setObject(variableSlot, forced);
      return forced;
    }
    return value;
  }
}

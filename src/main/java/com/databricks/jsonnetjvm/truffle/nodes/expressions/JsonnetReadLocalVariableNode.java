package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetReadLocalVariableNode extends JsonnetExpressionNode {
  private final int slot;
  private final String name;

  public JsonnetReadLocalVariableNode(int slot, String name) {
    this.slot = slot;
    this.name = name;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object value = frame.getObject(slot);
    if (value instanceof JsonnetThunk thunk) {
      Object forced = thunk.force();
      frame.setObject(slot, forced); // cache: replace thunk with computed value
      return forced;
    }
    return value;
  }
}

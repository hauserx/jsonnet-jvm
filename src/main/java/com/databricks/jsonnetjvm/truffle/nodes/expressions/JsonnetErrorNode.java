package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public class JsonnetErrorNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode messageNode;

  public JsonnetErrorNode(JsonnetExpressionNode messageNode) {
    this.messageNode = messageNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object message = messageNode.executeGeneric(frame);
    throwError(message, this);
    return null; // unreachable
  }

  @TruffleBoundary
  private static void throwError(Object message, Node location) {
    throw new JsonnetException(String.valueOf(message), location);
  }
}

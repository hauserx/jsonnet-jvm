package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

/** Expression-level assert: evaluates condition, throws if false, then returns body. */
public class JsonnetAssertNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode condition;
  @Child private JsonnetExpressionNode message;
  @Child private JsonnetExpressionNode body;

  public JsonnetAssertNode(
      JsonnetExpressionNode condition, JsonnetExpressionNode message, JsonnetExpressionNode body) {
    this.condition = condition;
    this.message = message;
    this.body = body;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object cond = condition.executeGeneric(frame);
    if (cond instanceof Boolean && !(Boolean) cond) {
      if (message != null) {
        Object msg = message.executeGeneric(frame);
        throwAssertionFailed(msg);
      }
      throwAssertionFailed(null);
    }
    return body.executeGeneric(frame);
  }

  @TruffleBoundary
  private void throwAssertionFailed(Object msg) {
    if (msg != null) {
      throw new JsonnetException("Assertion failed: " + msg, this);
    }
    throw new JsonnetException("Assertion failed", this);
  }
}

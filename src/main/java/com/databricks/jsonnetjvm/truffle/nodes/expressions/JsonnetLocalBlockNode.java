package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class JsonnetLocalBlockNode extends JsonnetExpressionNode {
  @Children private final JsonnetExpressionNode[] assignments;
  @Child private JsonnetExpressionNode body;

  public JsonnetLocalBlockNode(JsonnetExpressionNode[] assignments, JsonnetExpressionNode body) {
    this.assignments = assignments;
    this.body = body;
  }

  @Override
  @ExplodeLoop
  public Object executeGeneric(VirtualFrame frame) {
    for (JsonnetExpressionNode assignment : assignments) {
      assignment.executeGeneric(frame);
    }
    return body.executeGeneric(frame);
  }
}

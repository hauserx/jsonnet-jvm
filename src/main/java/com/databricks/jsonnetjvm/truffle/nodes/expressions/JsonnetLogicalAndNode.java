package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.JsonnetTypesGen;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class JsonnetLogicalAndNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode leftNode;
  @Child private JsonnetExpressionNode rightNode;

  public JsonnetLogicalAndNode(JsonnetExpressionNode leftNode, JsonnetExpressionNode rightNode) {
    this.leftNode = leftNode;
    this.rightNode = rightNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    boolean left;
    try {
      left = leftNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      left = JsonnetTypesGen.asBoolean(e.getResult());
    }
    if (!left) {
      return false;
    }
    boolean right;
    try {
      right = rightNode.executeBoolean(frame);
    } catch (UnexpectedResultException e) {
      right = JsonnetTypesGen.asBoolean(e.getResult());
    }
    return right;
  }
}

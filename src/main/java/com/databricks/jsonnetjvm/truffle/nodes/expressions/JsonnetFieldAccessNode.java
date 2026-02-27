package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetFieldAccessNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode objectNode;
  private final String fieldName;

  public JsonnetFieldAccessNode(JsonnetExpressionNode objectNode, String fieldName) {
    this.objectNode = objectNode;
    this.fieldName = fieldName;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object obj = objectNode.executeGeneric(frame);
    if (!(obj instanceof JObject)) {
      throwTypeError(obj);
    }
    return doFieldAccess((JObject) obj);
  }

  @TruffleBoundary
  private void throwTypeError(Object obj) {
    throw new JsonnetException(
        "attempted to index a " + JsonnetException.typeName(obj) + " with string " + fieldName,
        this);
  }

  @TruffleBoundary
  private Object doFieldAccess(JObject obj) {
    Object value = obj.getField(fieldName);
    if (value == null) {
      throw new JsonnetException("Field does not exist: " + fieldName, this);
    }
    return value;
  }
}

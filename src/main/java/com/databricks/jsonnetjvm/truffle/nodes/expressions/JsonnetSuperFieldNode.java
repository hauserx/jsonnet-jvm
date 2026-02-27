package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

/** Accesses a field on 'super' (the parent layer in object inheritance). */
public class JsonnetSuperFieldNode extends JsonnetExpressionNode {
  private final String fieldName;

  public JsonnetSuperFieldNode(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doAccess();
  }

  @TruffleBoundary
  private Object doAccess() {
    JObject self = JObject.getCurrentSelf();
    int superMaxLayer = JObject.getCurrentSuperMaxLayer();
    if (self == null || superMaxLayer < 0) {
      throw new JsonnetStaticError("Can't use super outside of an object", this);
    }
    Object result = self.evaluateField(fieldName, self, superMaxLayer);
    if (result == null) {
      throw new JsonnetException("super does not have field: " + fieldName, this);
    }
    return result;
  }
}

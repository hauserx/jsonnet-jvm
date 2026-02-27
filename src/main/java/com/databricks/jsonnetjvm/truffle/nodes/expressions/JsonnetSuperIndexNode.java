package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

/** Accesses a field on 'super' by computed key: super[expr]. */
public class JsonnetSuperIndexNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode indexNode;

  public JsonnetSuperIndexNode(JsonnetExpressionNode indexNode) {
    this.indexNode = indexNode;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object key = indexNode.executeGeneric(frame);
    return doAccess(key);
  }

  @TruffleBoundary
  private Object doAccess(Object key) {
    JObject self = JObject.getCurrentSelf();
    int superMaxLayer = JObject.getCurrentSuperMaxLayer();
    if (self == null || superMaxLayer < 0) {
      throw new JsonnetStaticError("Can't use super outside of an object", this);
    }
    String fieldName = (String) key;
    Object result = self.evaluateField(fieldName, self, superMaxLayer);
    if (result == null) {
      throw new JsonnetException("super does not have field: " + fieldName, this);
    }
    return result;
  }
}

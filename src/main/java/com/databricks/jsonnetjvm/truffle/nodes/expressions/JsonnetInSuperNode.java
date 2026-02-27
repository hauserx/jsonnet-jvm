package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Checks if a key exists in 'super' (the parent layer in object inheritance). Syntax: expr 'in'
 * 'super'
 */
public class JsonnetInSuperNode extends JsonnetExpressionNode {
  @Child private JsonnetExpressionNode keyExpr;

  public JsonnetInSuperNode(JsonnetExpressionNode keyExpr) {
    this.keyExpr = keyExpr;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object key = keyExpr.executeGeneric(frame);
    return doCheck((String) key);
  }

  @TruffleBoundary
  private boolean doCheck(String key) {
    JObject self = JObject.getCurrentSelf();
    int superMaxLayer = JObject.getCurrentSuperMaxLayer();
    if (self == null || superMaxLayer < 0) {
      throw new JsonnetStaticError("Can't use super outside of an object", this);
    }
    return self.hasFieldInLayers(key, superMaxLayer);
  }
}

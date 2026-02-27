package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

/** Returns the current 'self' object during field evaluation. */
public class JsonnetSelfNode extends JsonnetExpressionNode {

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    JObject self = JObject.getCurrentSelf();
    if (self == null) {
      throw new JsonnetStaticError("Can't use self outside of an object", this);
    }
    return self;
  }
}

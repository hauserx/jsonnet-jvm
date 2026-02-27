package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

/** Returns the current '$' (outermost object) during field evaluation. */
public class JsonnetDollarNode extends JsonnetExpressionNode {

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    JObject dollar = JObject.getCurrentDollar();
    if (dollar == null) {
      throw new JsonnetException("$ used outside of object", this);
    }
    return dollar;
  }
}

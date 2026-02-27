package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("left")
@NodeChild("right")
public abstract class JsonnetInNode extends JsonnetExpressionNode {
  @TruffleBoundary
  @Specialization
  protected boolean doCheck(String key, JObject obj) {
    return obj.hasFieldAll(key);
  }

  @TruffleBoundary
  @Specialization(guards = "!isJObject(right)")
  protected boolean doError(Object left, Object right) {
    throw new JsonnetException(
        "RHS of 'in' operator must be an object, got " + JsonnetException.typeName(right));
  }

  protected static boolean isJObject(Object val) {
    return val instanceof JObject;
  }
}

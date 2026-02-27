package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.Val;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import java.util.Deque;

public class JsonnetFunctionLiteralNode extends JsonnetExpressionNode {
  private final String name;
  private final CallTarget callTarget;
  private final int paramCount;
  private final String[] paramNames;

  public JsonnetFunctionLiteralNode(String name, CallTarget callTarget) {
    this(name, callTarget, -1, null);
  }

  public JsonnetFunctionLiteralNode(String name, CallTarget callTarget, int paramCount) {
    this(name, callTarget, paramCount, null);
  }

  public JsonnetFunctionLiteralNode(
      String name, CallTarget callTarget, int paramCount, String[] paramNames) {
    this.name = name;
    this.callTarget = callTarget;
    this.paramCount = paramCount;
    this.paramNames = paramNames;
  }

  @Override
  public Val executeGeneric(VirtualFrame frame) {
    MaterializedFrame mf = frame.materialize();
    return createFunction(mf);
  }

  @TruffleBoundary
  private JFunction createFunction(MaterializedFrame frame) {
    JFunction func = new JFunction(name, callTarget, frame, paramCount, paramNames);
    JObject dollar = JObject.getCurrentDollar();
    if (dollar != null) {
      func.setCapturedDollar(dollar);
    }
    JObject self = JObject.getCurrentSelf();
    if (self != null) {
      func.setCapturedSelf(self, JObject.getCurrentSuperMaxLayer());
    }
    Deque<JObject> scopeStack = JObject.getEnclosingScopeStack();
    if (!scopeStack.isEmpty()) {
      func.setCapturedEnclosingScopes(scopeStack.toArray(new JObject[0]));
    }
    return func;
  }
}

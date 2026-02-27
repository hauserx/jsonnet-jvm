package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Reads an object-local variable from the current self object. Object-local bindings are
 * re-evaluated each time because they depend on the current 'self' context.
 */
public class JsonnetObjectLocalReadNode extends JsonnetExpressionNode {
  private final String name;

  public JsonnetObjectLocalReadNode(String name) {
    this.name = name;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return doRead();
  }

  @TruffleBoundary
  private Object doRead() {
    // Walk the enclosing scope stack to find the object that has this local.
    // The stack is populated by:
    // 1. evaluateField() pushing self when a field is accessed
    // 2. Field suppliers pushing capturedEnclosing (the self at object creation
    // time)
    // This ensures both same-object locals and enclosing-object locals are found
    // with the correct self binding.
    for (JObject scope : JObject.getEnclosingScopeStack()) {
      if (scope != null && scope.hasLocal(name)) {
        return scope.evaluateLocal(name, scope);
      }
    }

    throw new JsonnetStaticError("Unknown variable: " + name, this);
  }
}

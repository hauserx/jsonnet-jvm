package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class JFunction extends Val {
  /** Sentinel value used to mark unbound argument positions when named args skip a slot. */
  public static final Object UNSET_ARG =
      new Object() {
        @Override
        public String toString() {
          return "<unset>";
        }
      };

  private final CallTarget callTarget;
  private final String name;
  private final MaterializedFrame closureFrame;
  private final int paramCount; // -1 for builtins (unlimited)
  private final String[] paramNames; // null for builtins
  private JObject capturedDollar;
  private JObject capturedSelf;
  private int capturedSuperMaxLayer = -1;
  private JObject[] capturedEnclosingScopes;

  public JFunction(String name, CallTarget callTarget) {
    this(name, callTarget, null, -1, null);
  }

  public JFunction(String name, CallTarget callTarget, MaterializedFrame closureFrame) {
    this(name, callTarget, closureFrame, -1, null);
  }

  public JFunction(
      String name, CallTarget callTarget, MaterializedFrame closureFrame, int paramCount) {
    this(name, callTarget, closureFrame, paramCount, null);
  }

  public JFunction(
      String name,
      CallTarget callTarget,
      MaterializedFrame closureFrame,
      int paramCount,
      String[] paramNames) {
    this.callTarget = callTarget;
    this.name = name;
    this.closureFrame = closureFrame;
    this.paramCount = paramCount;
    this.paramNames = paramNames;
  }

  public int getParamCount() {
    return paramCount;
  }

  public String[] getParamNames() {
    return paramNames;
  }

  public void setCapturedDollar(JObject dollar) {
    this.capturedDollar = dollar;
  }

  public JObject getCapturedDollar() {
    return capturedDollar;
  }

  public void setCapturedSelf(JObject self, int superMaxLayer) {
    this.capturedSelf = self;
    this.capturedSuperMaxLayer = superMaxLayer;
  }

  public JObject getCapturedSelf() {
    return capturedSelf;
  }

  public int getCapturedSuperMaxLayer() {
    return capturedSuperMaxLayer;
  }

  public void setCapturedEnclosingScopes(JObject[] scopes) {
    this.capturedEnclosingScopes = scopes;
  }

  public JObject[] getCapturedEnclosingScopes() {
    return capturedEnclosingScopes;
  }

  /** Push captured enclosing scopes onto the thread-local scope stack. */
  public void pushEnclosingScopes() {
    if (capturedEnclosingScopes != null) {
      for (int i = capturedEnclosingScopes.length - 1; i >= 0; i--) {
        JObject.pushEnclosingScope(capturedEnclosingScopes[i]);
      }
    }
  }

  /** Pop captured enclosing scopes from the thread-local scope stack. */
  public void popEnclosingScopes() {
    if (capturedEnclosingScopes != null) {
      for (int i = 0; i < capturedEnclosingScopes.length; i++) {
        JObject.popEnclosingScope();
      }
    }
  }

  public CallTarget getCallTarget() {
    return callTarget;
  }

  public MaterializedFrame getClosureFrame() {
    return closureFrame;
  }

  /**
   * Call this function with the given arguments, automatically prepending the closure frame if this
   * is a user-defined function with a closure.
   */
  @TruffleBoundary
  public Object call(Object... args) {
    JObject prevSelf = null;
    int prevSuperMax = -1;
    boolean restoreSelf = capturedSelf != null;
    if (restoreSelf) {
      prevSelf = JObject.getCurrentSelf();
      prevSuperMax = JObject.getCurrentSuperMaxLayer();
      JObject.setCurrentSelf(capturedSelf);
      JObject.setCurrentSuperMaxLayer(capturedSuperMaxLayer);
    }
    pushEnclosingScopes();
    try {
      if (closureFrame != null) {
        Object[] callArgs = new Object[args.length + 1];
        callArgs[0] = closureFrame;
        System.arraycopy(args, 0, callArgs, 1, args.length);
        return callTarget.call(callArgs);
      }
      return callTarget.call(args);
    } finally {
      popEnclosingScopes();
      if (restoreSelf) {
        JObject.setCurrentSelf(prevSelf);
        JObject.setCurrentSuperMaxLayer(prevSuperMax);
      }
    }
  }

  @Override
  @TruffleBoundary
  public String toStringValue() {
    return "function " + name;
  }

  @ExportMessage
  boolean isExecutable() {
    return true;
  }

  @ExportMessage
  Object execute(Object[] arguments)
      throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
    return call(arguments);
  }

  @Override
  @TruffleBoundary
  public String toJson() {
    String params = paramNames != null ? String.join(", ", paramNames) : "";
    throw new JsonnetException("Couldn't manifest function with params [" + params + "]");
  }
}

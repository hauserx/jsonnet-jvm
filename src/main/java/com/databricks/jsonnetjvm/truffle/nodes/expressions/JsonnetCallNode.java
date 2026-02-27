package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.runtime.TailCallException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import java.util.Arrays;

public class JsonnetCallNode extends JsonnetExpressionNode {

  private static final ThreadLocal<Boolean> IN_TRAMPOLINE = ThreadLocal.withInitial(() -> false);

  @Child private JsonnetExpressionNode functionNode;
  @Children private final JsonnetExpressionNode[] argumentNodes;
  @Child private IndirectCallNode callNode = IndirectCallNode.create();
  private final String[] argNames;
  private final boolean hasNamedArgs;
  private final boolean tailstrict;

  public JsonnetCallNode(
      JsonnetExpressionNode functionNode,
      JsonnetExpressionNode[] argumentNodes,
      String[] argNames,
      boolean tailstrict) {
    this.functionNode = functionNode;
    this.argumentNodes = argumentNodes;
    this.argNames = argNames;
    this.tailstrict = tailstrict;
    boolean named = false;
    for (String n : argNames) {
      if (n != null) {
        named = true;
        break;
      }
    }
    this.hasNamedArgs = named;
  }

  @Override
  @ExplodeLoop
  public Object executeGeneric(VirtualFrame frame) {
    Object funcVal = functionNode.executeGeneric(frame);
    if (!(funcVal instanceof JFunction)) {
      throwNotFunction(funcVal);
    }
    JFunction function = (JFunction) funcVal;

    MaterializedFrame mFrame = frame.materialize();
    Object[] rawArgs = new Object[argumentNodes.length];

    if (tailstrict) {
      for (int i = 0; i < argumentNodes.length; i++) {
        rawArgs[i] = argumentNodes[i].executeGeneric(mFrame);
        if (rawArgs[i] instanceof JsonnetThunk thunk) {
          rawArgs[i] = thunk.force();
        }
      }
    } else {
      for (int i = 0; i < argumentNodes.length; i++) {
        rawArgs[i] = createLazyArg(i, mFrame);
      }
    }

    if (hasNamedArgs) {
      return doCallNamed(function, rawArgs);
    }
    return doCall(function, rawArgs);
  }

  @TruffleBoundary
  private Object doCall(JFunction function, Object[] rawArgs) {
    int paramCount = function.getParamCount();
    if (paramCount >= 0 && rawArgs.length > paramCount) {
      throw new JsonnetException(
          "Too many args, function has " + paramCount + " parameter(s)", this);
    }
    if (tailstrict) {
      return invokeTailstrict(function, rawArgs);
    }
    return invoke(function, rawArgs);
  }

  @TruffleBoundary
  private Object doCallNamed(JFunction function, Object[] rawArgs) {
    String[] paramNames = function.getParamNames();
    int paramCount = function.getParamCount();

    if (paramNames == null || paramCount < 0) {
      throw new JsonnetException("Named arguments not supported for builtin functions", this);
    }

    Object[] reordered = new Object[paramCount];
    Arrays.fill(reordered, JFunction.UNSET_ARG);

    int numPositional = 0;
    for (int i = 0; i < argNames.length; i++) {
      if (argNames[i] == null) numPositional++;
      else break;
    }
    if (numPositional > paramCount) {
      throw new JsonnetException(
          "Too many args, function has " + paramCount + " parameter(s)", this);
    }
    for (int i = 0; i < numPositional; i++) {
      reordered[i] = rawArgs[i];
    }

    for (int i = numPositional; i < rawArgs.length; i++) {
      String name = argNames[i];
      int paramIdx = findParamIndex(paramNames, name);
      if (paramIdx < 0) {
        throw new JsonnetException("Function has no parameter " + name, this);
      }
      if (reordered[paramIdx] != JFunction.UNSET_ARG) {
        throw new JsonnetException("binding parameter a second time: " + name, this);
      }
      reordered[paramIdx] = rawArgs[i];
    }

    if (tailstrict) {
      return invokeTailstrict(function, reordered);
    }
    return invoke(function, reordered);
  }

  @TruffleBoundary
  private Object createLazyArg(int idx, MaterializedFrame mFrame) {
    return JsonnetThunk.withSelfCapture(() -> argumentNodes[idx].executeGeneric(mFrame));
  }

  @TruffleBoundary
  private void throwNotFunction(Object funcVal) {
    throw new JsonnetException(
        "Expected function, found " + JsonnetException.typeName(funcVal), this);
  }

  private static int findParamIndex(String[] paramNames, String name) {
    for (int i = 0; i < paramNames.length; i++) {
      if (paramNames[i].equals(name)) return i;
    }
    return -1;
  }

  @TruffleBoundary
  private Object invokeTailstrict(JFunction function, Object[] args) {
    if (IN_TRAMPOLINE.get()) {
      throw new TailCallException(function, args);
    }

    IN_TRAMPOLINE.set(true);
    try {
      JFunction currentFunc = function;
      Object[] currentArgs = args;
      while (true) {
        try {
          return invoke(currentFunc, currentArgs);
        } catch (TailCallException tce) {
          currentFunc = tce.getFunction();
          currentArgs = tce.getArgs();
        }
      }
    } finally {
      IN_TRAMPOLINE.set(false);
    }
  }

  private Object invoke(JFunction function, Object[] args) {
    JObject prevSelf = null;
    int prevSuperMax = -1;
    JObject capturedSelf = function.getCapturedSelf();
    if (capturedSelf != null) {
      prevSelf = JObject.getCurrentSelf();
      prevSuperMax = JObject.getCurrentSuperMaxLayer();
      JObject.setCurrentSelf(capturedSelf);
      JObject.setCurrentSuperMaxLayer(function.getCapturedSuperMaxLayer());
    }
    function.pushEnclosingScopes();
    try {
      if (function.getClosureFrame() != null) {
        Object[] callArgs = new Object[args.length + 1];
        callArgs[0] = function.getClosureFrame();
        System.arraycopy(args, 0, callArgs, 1, args.length);

        JObject capturedDollar = function.getCapturedDollar();
        if (capturedDollar != null) {
          JObject prevDollar = JObject.getCurrentDollar();
          try {
            JObject.setCurrentDollar(capturedDollar);
            return callNode.call(function.getCallTarget(), callArgs);
          } finally {
            JObject.setCurrentDollar(prevDollar);
          }
        }
        return callNode.call(function.getCallTarget(), callArgs);
      } else {
        return callNode.call(function.getCallTarget(), args);
      }
    } finally {
      function.popEnclosingScopes();
      if (capturedSelf != null) {
        JObject.setCurrentSelf(prevSelf);
        JObject.setCurrentSuperMaxLayer(prevSuperMax);
      }
    }
  }
}

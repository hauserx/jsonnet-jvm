package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import java.util.function.Function;

/**
 * Generic root node for stdlib builtin functions. Wraps a Java lambda that takes Object[] args and
 * returns Object. By default, forces all thunk args before calling the lambda. Set {@code
 * lazyArgs=true} to pass thunks through for builtins that need to control evaluation order.
 */
public class JsonnetBuiltinRootNode extends RootNode {
  private final String name;
  private final Function<Object[], Object> body;
  private final boolean lazyArgs;

  public JsonnetBuiltinRootNode(
      JsonnetLanguage language, String name, Function<Object[], Object> body) {
    this(language, name, body, false);
  }

  public JsonnetBuiltinRootNode(
      JsonnetLanguage language, String name, Function<Object[], Object> body, boolean lazyArgs) {
    super(language, FrameDescriptor.newBuilder().build());
    this.name = name;
    this.body = body;
    this.lazyArgs = lazyArgs;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return doExecute(frame.getArguments());
  }

  @TruffleBoundary
  private Object doExecute(Object[] arguments) {
    try {
      if (!lazyArgs) {
        for (int i = 0; i < arguments.length; i++) {
          arguments[i] = JsonnetThunk.forceIfThunk(arguments[i]);
        }
      }
      return body.apply(arguments);
    } catch (AbstractTruffleException e) {
      throw e;
    } catch (StackOverflowError e) {
      throw new JsonnetException(
          "Stackoverflow while materializing, possibly due to recursive value");
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new JsonnetException("Function parameter not bound in call");
    } catch (ClassCastException e) {
      throw new JsonnetException("Wrong parameter type in std." + name);
    } catch (Exception e) {
      throw new JsonnetException("std." + name + ": " + e.getMessage());
    }
  }

  @Override
  public String getName() {
    return "std." + name;
  }
}

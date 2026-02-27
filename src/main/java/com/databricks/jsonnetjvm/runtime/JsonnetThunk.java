package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.function.Supplier;

/**
 * Lazy evaluation wrapper. Wraps an unevaluated expression that is computed on first access and
 * cached thereafter. Used for local bindings to support forward references and avoid unnecessary
 * computation.
 */
public class JsonnetThunk {
  private Supplier<Object> supplier;
  private Object value;
  private boolean forced;
  private boolean forcing;

  public JsonnetThunk(Supplier<Object> supplier) {
    this.supplier = supplier;
  }

  /**
   * Creates a thunk that captures the current self/super context so that expressions containing
   * {@code self} evaluate correctly even when the thunk is forced outside the originating object
   * scope.
   */
  @TruffleBoundary
  public static JsonnetThunk withSelfCapture(Supplier<Object> supplier) {
    JObject capturedSelf = JObject.getCurrentSelf();
    int capturedSuperMax = JObject.getCurrentSuperMaxLayer();
    if (capturedSelf == null) {
      return new JsonnetThunk(supplier);
    }
    return new JsonnetThunk(
        () -> {
          JObject prevSelf = JObject.getCurrentSelf();
          int prevSuperMax = JObject.getCurrentSuperMaxLayer();
          JObject.setCurrentSelf(capturedSelf);
          JObject.setCurrentSuperMaxLayer(capturedSuperMax);
          try {
            return supplier.get();
          } finally {
            JObject.setCurrentSelf(prevSelf);
            JObject.setCurrentSuperMaxLayer(prevSuperMax);
          }
        });
  }

  @TruffleBoundary
  public Object force() {
    if (forced) {
      return value;
    }
    if (forcing) {
      throw new JsonnetException("Infinite recursion in lazy value");
    }
    forcing = true;
    try {
      value = supplier.get();
      supplier = null; // allow GC
      forced = true;
      return value;
    } finally {
      forcing = false;
    }
  }

  public boolean isForced() {
    return forced;
  }

  /** Force if thunk, otherwise return as-is. Convenience for lazy-arg builtins. */
  public static Object forceIfThunk(Object val) {
    if (val instanceof JsonnetThunk thunk) {
      return thunk.force();
    }
    return val;
  }
}

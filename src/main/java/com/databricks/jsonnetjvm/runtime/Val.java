package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;

public abstract class Val implements TruffleObject {
  @TruffleBoundary
  public String toStringValue() {
    throw new JsonnetException(
        "Wrong parameter type: expected String, got " + JsonnetException.typeName(this));
  }

  @TruffleBoundary
  public abstract String toJson();

  @Override
  @TruffleBoundary
  public String toString() {
    return toJson();
  }

  @TruffleBoundary
  public double asNumber() {
    throw new JsonnetException(
        "Wrong parameter type: expected Number, got " + JsonnetException.typeName(this));
  }

  @TruffleBoundary
  public boolean asBoolean() {
    throw new JsonnetException(
        "Wrong parameter type: expected Boolean, got " + JsonnetException.typeName(this));
  }
}

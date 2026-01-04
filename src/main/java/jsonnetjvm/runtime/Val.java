package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;

public abstract class Val implements TruffleObject {
    @TruffleBoundary
    public String toStringValue() {
        throw new UnsupportedOperationException("Not a string: " + getClass().getSimpleName());
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
        throw new UnsupportedOperationException("Not a number: " + getClass().getSimpleName());
    }

    @TruffleBoundary
    public boolean asBoolean() {
        throw new UnsupportedOperationException("Not a boolean: " + getClass().getSimpleName());
    }
}

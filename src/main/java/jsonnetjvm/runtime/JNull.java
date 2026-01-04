package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class JNull extends Val {
    public static final JNull INSTANCE = new JNull();

    private JNull() {
    }

    @Override
    @TruffleBoundary
    public String toStringValue() {
        throw new UnsupportedOperationException("Cannot convert null to string.");
    }

    @ExportMessage
    boolean isNull() {
        return true;
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        return "null";
    }
}
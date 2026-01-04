package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class JString extends Val {
    private final String value;

    public JString(String value) {
        this.value = value;
    }

    @Override
    @TruffleBoundary
    public String toStringValue() {
        return value;
    }

    @ExportMessage
    boolean isString() {
        return true;
    }

    @ExportMessage
    String asString() {
        return value;
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }
}

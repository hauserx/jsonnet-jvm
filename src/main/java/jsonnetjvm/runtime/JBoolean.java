package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class JBoolean extends Val {
    public static final JBoolean TRUE = new JBoolean(true);
    public static final JBoolean FALSE = new JBoolean(false);

    private final boolean value;

    private JBoolean(boolean value) {
        this.value = value;
    }

    public static JBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    @TruffleBoundary
    public String toStringValue() {
        return String.valueOf(value);
    }

    @ExportMessage
    boolean isBoolean() {
        return true;
    }

    @ExportMessage
    public boolean asBoolean() {
        return value;
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        return value ? "true" : "false";
    }
}

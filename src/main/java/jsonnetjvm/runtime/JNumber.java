package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.math.BigInteger;

@ExportLibrary(InteropLibrary.class)
public class JNumber extends Val {
    private final double value;

    public JNumber(double value) {
        this.value = value;
    }

    @Override
    @TruffleBoundary
    public String toStringValue() {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    @Override
    public double asNumber() {
        return value;
    }

    @ExportMessage
    boolean isNumber() {
        return true;
    }

    @ExportMessage
    boolean fitsInByte() {
        return value == (byte) value;
    }

    @ExportMessage
    boolean fitsInShort() {
        return value == (short) value;
    }

    @ExportMessage
    boolean fitsInInt() {
        return value == (int) value;
    }

    @ExportMessage
    boolean fitsInLong() {
        return value == (long) value;
    }

    @ExportMessage
    boolean fitsInFloat() {
        return true;
    }

    @ExportMessage
    boolean fitsInDouble() {
        return true;
    }

    @ExportMessage
    boolean fitsInBigInteger() {
        return false; // For simplicity
    }

    @ExportMessage
    byte asByte() {
        return (byte) value;
    }

    @ExportMessage
    short asShort() {
        return (short) value;
    }

    @ExportMessage
    int asInt() {
        return (int) value;
    }

    @ExportMessage
    long asLong() {
        return (long) value;
    }

    @ExportMessage
    float asFloat() {
        return (float) value;
    }

    @ExportMessage
    double asDouble() {
        return value;
    }

    @ExportMessage
    @TruffleBoundary
    BigInteger asBigInteger() {
        return BigInteger.valueOf((long) value);
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}

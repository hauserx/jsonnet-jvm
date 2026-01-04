package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.stream.Collectors;
import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
public class JNumberArray extends JArray {
    private final double[] values;

    public JNumberArray(double[] values) {
        this.values = values;
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public Object get(int index) {
        return values[index];
    }

    public double getDouble(int index) {
        return values[index];
    }

    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return values.length;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < values.length;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(index)) {
            throw InvalidArrayIndexException.create(index);
        }
        return values[(int) index];
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        return "[" + Arrays.stream(values).mapToObj(Double::toString).collect(Collectors.joining(", ")) + "]";
    }
}
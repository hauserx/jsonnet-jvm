package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@ExportLibrary(InteropLibrary.class)
public class JGenericArray extends JArray implements Iterable<Object> {
    private final List<Object> items;

    public JGenericArray(List<Object> items) {
        this.items = items;
    }

    public List<Object> getItems() {
        return items;
    }

    @Override
    public Object get(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public Iterator<Object> iterator() {
        return items.iterator();
    }

    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return items.size();
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < items.size();
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(index)) {
            throw InvalidArrayIndexException.create(index);
        }
        return items.get((int) index);
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            Object val = items.get(i);
            if (val instanceof Val) {
                sb.append(((Val) val).toJson());
            } else if (val instanceof String) {
                sb.append("\"").append(((String) val).replace("\"", "\\\"")).append("\"");
            } else {
                sb.append(String.valueOf(val));
            }
            if (i < items.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}

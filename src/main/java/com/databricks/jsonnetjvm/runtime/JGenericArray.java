package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.Iterator;
import java.util.List;

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
    Object val = items.get(index);
    if (val instanceof JsonnetThunk thunk) {
      Object forced = thunk.force();
      items.set(index, forced);
      return forced;
    }
    return val;
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
    return get((int) index);
  }

  @Override
  @TruffleBoundary
  public String toJson() {
    return JsonnetJson.renderInline(this);
  }
}

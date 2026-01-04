package jsonnetjvm.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility to build specialized arrays based on content.
 */
public class ArrayBuilder {
    private double[] doubles;
    private boolean[] booleans;
    private String[] strings;
    private List<Object> generic;
    private int size = 0;
    private final int capacity;

    public ArrayBuilder(int capacity) {
        this.capacity = capacity;
    }

    public void add(Object val) {
        if (generic != null) {
            generic.add(val);
        } else if (val instanceof Double) {
            addDouble((Double) val);
        } else if (val instanceof Boolean) {
            addBoolean((Boolean) val);
        } else if (val instanceof String) {
            addString((String) val);
        } else {
            ensureGeneric();
            generic.add(val);
        }
        size++;
    }

    private void addDouble(double d) {
        if (booleans != null || strings != null) {
            ensureGeneric();
            generic.add(d);
            return;
        }
        if (doubles == null)
            doubles = new double[capacity];
        doubles[size] = d;
    }

    private void addBoolean(boolean b) {
        if (doubles != null || strings != null) {
            ensureGeneric();
            generic.add(b);
            return;
        }
        if (booleans == null)
            booleans = new boolean[capacity];
        booleans[size] = b;
    }

    private void addString(String s) {
        if (doubles != null || booleans != null) {
            ensureGeneric();
            generic.add(s);
            return;
        }
        if (strings == null)
            strings = new String[capacity];
        strings[size] = s;
    }

    private void ensureGeneric() {
        if (generic != null)
            return;
        generic = new ArrayList<>(capacity);
        if (doubles != null) {
            for (int i = 0; i < size; i++)
                generic.add(doubles[i]);
            doubles = null;
        } else if (booleans != null) {
            for (int i = 0; i < size; i++)
                generic.add(booleans[i]);
            booleans = null;
        } else if (strings != null) {
            for (int i = 0; i < size; i++)
                generic.add(strings[i]);
            strings = null;
        }
    }

    public JArray build() {
        if (generic != null)
            return new JGenericArray(generic);
        if (doubles != null) {
            if (size < doubles.length) {
                double[] result = new double[size];
                System.arraycopy(doubles, 0, result, 0, size);
                return new JNumberArray(result);
            }
            return new JNumberArray(doubles);
        }
        if (booleans != null) {
            if (size < booleans.length) {
                boolean[] result = new boolean[size];
                System.arraycopy(booleans, 0, result, 0, size);
                return new JBooleanArray(result);
            }
            return new JBooleanArray(booleans);
        }
        if (strings != null) {
            if (size < strings.length) {
                String[] result = new String[size];
                System.arraycopy(strings, 0, result, 0, size);
                return new JStringArray(result);
            }
            return new JStringArray(strings);
        }
        return new JGenericArray(new ArrayList<>());
    }
}

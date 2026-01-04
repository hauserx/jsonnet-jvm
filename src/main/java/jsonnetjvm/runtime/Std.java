package jsonnetjvm.runtime;

import java.util.ArrayList;

public class Std {
    public static JArray range(double from, double to) {
        int start = (int) from;
        int end = (int) to;
        if (end < start) {
            return new JGenericArray(new ArrayList<>());
        }
        double[] array = new double[end - start + 1];
        for (int i = 0; i <= end - start; i++) {
            array[i] = start + i;
        }
        return new JNumberArray(array);
    }

    public static double sum(JArray array) {
        double total = 0.0;
        if (array instanceof JNumberArray) {
            JNumberArray nArray = (JNumberArray) array;
            for (int i = 0; i < nArray.size(); i++) {
                total += nArray.getDouble(i);
            }
        } else {
            JGenericArray gArray = (JGenericArray) array;
            for (Object item : gArray.getItems()) {
                if (item instanceof Double) {
                    total += (Double) item;
                } else if (item instanceof Boolean) {
                    total += (Boolean) item ? 1 : 0;
                }
            }
        }
        return total;
    }

    public static int length(JArray array) {
        return array.size();
    }
}

package jsonnetjvm.runtime;

import java.util.ArrayList;
import java.util.List;

public class Std {
    public static Val range(Val from, Val to) {
        int start = (int) from.asNumber();
        int end = (int) to.asNumber();
        List<Val> list = new ArrayList<>();
        // Jsonnet range is inclusive [from, to]
        for (int i = start; i <= end; i++) {
            list.add(new JNumber(i));
        }
        return new JArray(list);
    }

    public static Val sum(Val array) {
        if (!(array instanceof JArray)) {
            throw new IllegalArgumentException(
                    "std.sum expects an array, but got: " + array.getClass().getSimpleName());
        }
        JArray jArray = (JArray) array;
        double total = 0.0;
        for (Val item : jArray.getItems()) {
            total += item.asNumber(); // Jsonnet coerces booleans/strings to numbers in arithmetic context
        }
        return new JNumber(total);
    }
}
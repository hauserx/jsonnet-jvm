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
}

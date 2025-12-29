package jsonnetjvm.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class JArray extends Val implements Iterable<Val> {
    private final List<Val> items;

    public JArray(List<Val> items) {
        this.items = items;
    }

    public List<Val> getItems() {
        return items;
    }

    @Override
    public Iterator<Val> iterator() {
        return items.iterator();
    }

    @Override
    public String toJson() {
        return "[" + items.stream().map(Val::toJson).collect(Collectors.joining(", ")) + "]";
    }
}

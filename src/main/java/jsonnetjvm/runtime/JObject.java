package jsonnetjvm.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JObject extends Val {
    private final Map<String, Supplier<Val>> fields = new LinkedHashMap<>();

    public void addField(String name, Supplier<Val> thunk) {
        fields.put(name, thunk);
    }

    @Override
    public String toJson() {
        return "{" + fields.entrySet().stream().map(e -> "\"" + e.getKey() + "\": " + e.getValue().get().toJson())
                .collect(Collectors.joining(",")) + "}";
    }
}

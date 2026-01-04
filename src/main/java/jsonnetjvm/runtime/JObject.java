package jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JObject extends Val {
    private final Map<String, Supplier<Object>> fields = new LinkedHashMap<>();

    public void addField(String name, Supplier<Object> thunk) {
        fields.put(name, thunk);
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        return "{" + fields.entrySet().stream().map(e -> {
            Object val = e.getValue().get();
            String jsonVal;
            if (val instanceof Val) {
                jsonVal = ((Val) val).toJson();
            } else if (val instanceof String) {
                jsonVal = "\"" + ((String) val).replace("\"", "\\\"") + "\"";
            } else {
                jsonVal = String.valueOf(val);
            }
            return "\"" + e.getKey() + "\": " + jsonVal;
        }).collect(Collectors.joining(",")) + "}";
    }
}

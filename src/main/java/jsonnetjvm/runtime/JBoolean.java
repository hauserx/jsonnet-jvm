package jsonnetjvm.runtime;

public class JBoolean extends Val {
    private final boolean value;

    public JBoolean(boolean value) {
        this.value = value;
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public double asNumber() {
        return value ? 1.0 : 0.0;
    }

    @Override
    public boolean asBoolean() {
        return value;
    }

    @Override
    public String toJson() {
        return value ? "true" : "false";
    }
}

package jsonnetjvm.runtime;

public class JNull extends Val {
    public static final JNull INSTANCE = new JNull();

    private JNull() {
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("Cannot convert null to string.");
    }

    @Override
    public double asNumber() {
        throw new UnsupportedOperationException("Cannot convert null to number.");
    }

    @Override
    public boolean asBoolean() {
        // Jsonnet 'null' is falsey
        return false;
    }

    @Override
    public String toJson() {
        return "null";
    }
}

package jsonnetjvm.runtime;

public class JNumber extends Val {
    private final double value;

    public JNumber(double value) {
        this.value = value;
    }

    @Override
    public double asNumber() {
        return value;
    }

    @Override
    public String toJson() {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%f", value);
    }
}

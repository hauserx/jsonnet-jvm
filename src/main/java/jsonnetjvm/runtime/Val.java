package jsonnetjvm.runtime;

public abstract class Val {

    public String asString() {
        throw new UnsupportedOperationException("Not a string: " + getClass().getSimpleName());
    }

    public double asNumber() {
        throw new UnsupportedOperationException("Not a number: " + getClass().getSimpleName());
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException("Not a boolean: " + getClass().getSimpleName());
    }

    public abstract String toJson();

}

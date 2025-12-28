package jsonnetjvm.runtime;

public abstract class Val {
    public String asString() { throw new UnsupportedOperationException("Not a string"); }
    public abstract String toJson();
}

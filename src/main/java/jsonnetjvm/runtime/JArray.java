package jsonnetjvm.runtime;

public abstract class JArray extends Val {
    public abstract int size();
    public abstract Object get(int index);
}

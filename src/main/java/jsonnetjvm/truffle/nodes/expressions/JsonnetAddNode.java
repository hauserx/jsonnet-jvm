package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetAddNode extends JsonnetBinaryNode {

    @Specialization
    protected double doDouble(double left, double right) {
        return left + right;
    }

    @Specialization
    protected String doString(String left, String right) {
        return concat(left, right);
    }

    @Specialization
    protected String doBooleanString(boolean left, String right) {
        return concat(left, right);
    }

    @Specialization
    protected String doStringBoolean(String left, boolean right) {
        return concat(left, right);
    }

    @Specialization
    protected String doStringLeft(String left, Object right) {
        return concat(left, right);
    }

    @Specialization
    protected String doStringRight(Object left, String right) {
        return concat(left, right);
    }

    @TruffleBoundary
    private String concat(Object left, Object right) {
        return String.valueOf(left) + String.valueOf(right);
    }
}
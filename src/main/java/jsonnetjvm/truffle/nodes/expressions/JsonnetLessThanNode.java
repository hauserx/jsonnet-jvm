package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class JsonnetLessThanNode extends JsonnetBinaryNode {

    @Specialization
    protected boolean doDouble(double left, double right) {
        return left < right;
    }

    @Specialization
    protected boolean doString(String left, String right) {
        return left.compareTo(right) < 0;
    }
}

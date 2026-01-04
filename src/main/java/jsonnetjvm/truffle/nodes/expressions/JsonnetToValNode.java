package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import jsonnetjvm.runtime.Val;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

@NodeChild("valueNode")
public abstract class JsonnetToValNode extends JsonnetExpressionNode {

    @Override
    public abstract Object executeGeneric(com.oracle.truffle.api.frame.VirtualFrame frame);

    @Specialization
    protected boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    protected double doDouble(double value) {
        return value;
    }

    @Specialization
    protected String doString(String value) {
        return value;
    }

    @Specialization
    protected Object doObject(Object value) {
        return value;
    }
}

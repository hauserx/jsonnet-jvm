package jsonnetjvm.truffle.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetNumberLiteralNode extends JsonnetExpressionNode {
    private final double value;

    public JsonnetNumberLiteralNode(double value) {
        this.value = value;
    }

    @Override
    public double executeDouble(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}

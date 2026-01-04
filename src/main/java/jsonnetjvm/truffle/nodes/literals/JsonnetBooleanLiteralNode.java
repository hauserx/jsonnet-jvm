package jsonnetjvm.truffle.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetBooleanLiteralNode extends JsonnetExpressionNode {
    private final boolean value;

    public JsonnetBooleanLiteralNode(boolean value) {
        this.value = value;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}

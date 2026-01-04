package jsonnetjvm.truffle.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetStringLiteralNode extends JsonnetExpressionNode {
    private final String value;

    public JsonnetStringLiteralNode(String value) {
        this.value = value;
    }

    @Override
    public String executeString(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }
}
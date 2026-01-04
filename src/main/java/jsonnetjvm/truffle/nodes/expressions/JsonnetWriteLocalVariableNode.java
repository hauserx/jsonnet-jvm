package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetWriteLocalVariableNode extends JsonnetExpressionNode {
    private final int slot;
    @Child
    private JsonnetExpressionNode valueNode;

    public JsonnetWriteLocalVariableNode(int slot, JsonnetExpressionNode valueNode) {
        this.slot = slot;
        this.valueNode = valueNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object value = valueNode.executeGeneric(frame);
        frame.setObject(slot, value);
        return value;
    }
}
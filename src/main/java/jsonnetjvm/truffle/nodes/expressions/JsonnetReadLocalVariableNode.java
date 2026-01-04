package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetReadLocalVariableNode extends JsonnetExpressionNode {
    private final int slot;
    private final String name;

    public JsonnetReadLocalVariableNode(int slot, String name) {
        this.slot = slot;
        this.name = name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return frame.getObject(slot);
    }
}
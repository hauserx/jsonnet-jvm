package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetArgReadNode extends JsonnetExpressionNode {
    private final int index;

    public JsonnetArgReadNode(int index) {
        this.index = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        if (index < args.length) {
            return args[index];
        }
        return null;
    }
}
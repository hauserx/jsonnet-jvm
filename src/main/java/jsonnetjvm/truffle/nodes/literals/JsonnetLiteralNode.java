package jsonnetjvm.truffle.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public abstract class JsonnetLiteralNode extends JsonnetExpressionNode {
    @Override
    public abstract Object executeGeneric(VirtualFrame frame);
}

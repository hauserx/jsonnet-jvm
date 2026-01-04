package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import jsonnetjvm.runtime.JFunction;
import jsonnetjvm.runtime.Val;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetCallNode extends JsonnetExpressionNode {
    @Child
    private JsonnetExpressionNode functionNode;
    @Children
    private final JsonnetExpressionNode[] argumentNodes;
    @Child
    private IndirectCallNode callNode = IndirectCallNode.create();

    public JsonnetCallNode(JsonnetExpressionNode functionNode, JsonnetExpressionNode[] argumentNodes) {
        this.functionNode = functionNode;
        this.argumentNodes = argumentNodes;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object funcVal = functionNode.executeGeneric(frame);
        if (!(funcVal instanceof JFunction)) {
            throw new RuntimeException("Target is not a function");
        }
        JFunction function = (JFunction) funcVal;

        Object[] arguments = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            arguments[i] = argumentNodes[i].executeGeneric(frame);
        }

        return callNode.call(function.getCallTarget(), arguments);
    }
}
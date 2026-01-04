package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.runtime.JFunction;
import jsonnetjvm.runtime.Val;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetFunctionLiteralNode extends JsonnetExpressionNode {
    private final String name;
    private final CallTarget callTarget;

    public JsonnetFunctionLiteralNode(String name, CallTarget callTarget) {
        this.name = name;
        this.callTarget = callTarget;
    }

    @Override
    public Val executeGeneric(VirtualFrame frame) {
        return new JFunction(name, callTarget);
    }
}

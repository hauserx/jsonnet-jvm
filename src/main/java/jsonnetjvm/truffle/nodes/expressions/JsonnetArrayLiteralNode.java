package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.runtime.JGenericArray;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

import java.util.ArrayList;
import java.util.List;

public class JsonnetArrayLiteralNode extends JsonnetExpressionNode {
    @Children
    private final JsonnetExpressionNode[] elementNodes;

    public JsonnetArrayLiteralNode(JsonnetExpressionNode[] elementNodes) {
        this.elementNodes = elementNodes;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        List<Object> elements = new ArrayList<>(elementNodes.length);
        for (JsonnetExpressionNode node : elementNodes) {
            elements.add(node.executeGeneric(frame));
        }
        return new JGenericArray(elements);
    }
}

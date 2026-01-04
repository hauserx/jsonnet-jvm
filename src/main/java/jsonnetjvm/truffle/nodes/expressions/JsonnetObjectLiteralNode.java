package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import jsonnetjvm.runtime.JObject;
import jsonnetjvm.runtime.Val;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetObjectLiteralNode extends JsonnetExpressionNode {
    private final String[] fieldNames;
    @Children
    private final JsonnetExpressionNode[] valueNodes;
    @Children
    private final JsonnetToValNode[] toValNodes;

    public JsonnetObjectLiteralNode(String[] fieldNames, JsonnetExpressionNode[] valueNodes) {
        this.fieldNames = fieldNames;
        this.valueNodes = valueNodes;
        this.toValNodes = new JsonnetToValNode[valueNodes.length];
        for (int i = 0; i < valueNodes.length; i++) {
            this.toValNodes[i] = JsonnetToValNodeGen.create(valueNodes[i]);
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return createAndPopulate(frame.materialize());
    }

    @TruffleBoundary
    private JObject createAndPopulate(MaterializedFrame frame) {
        JObject obj = new JObject();
        for (int i = 0; i < fieldNames.length; i++) {
            final int index = i;
            obj.addField(fieldNames[i], () -> toValNodes[index].executeGeneric(frame));
        }
        return obj;
    }
}
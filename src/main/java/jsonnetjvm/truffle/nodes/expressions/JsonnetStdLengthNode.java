package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import jsonnetjvm.runtime.JArray;
import jsonnetjvm.runtime.Std;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

@NodeChild(value = "array", type = JsonnetExpressionNode.class)
public abstract class JsonnetStdLengthNode extends JsonnetBuiltinNode {
    @Specialization
    protected int doLength(JArray array) {
        return Std.length(array);
    }
}

package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import jsonnetjvm.runtime.Std;
import jsonnetjvm.runtime.Val;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

@NodeChild(value = "from", type = JsonnetExpressionNode.class)
@NodeChild(value = "to", type = JsonnetExpressionNode.class)
public abstract class JsonnetStdRangeNode extends JsonnetBuiltinNode {
    @Specialization
    protected Val doRange(double from, double to) {
        return Std.range(from, to);
    }

    @Specialization
    protected Val doGeneric(Val from, Val to) {
        return Std.range(from.asNumber(), to.asNumber());
    }
}

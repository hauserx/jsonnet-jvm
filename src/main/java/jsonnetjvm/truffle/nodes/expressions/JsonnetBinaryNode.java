package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class JsonnetBinaryNode extends JsonnetExpressionNode {
}

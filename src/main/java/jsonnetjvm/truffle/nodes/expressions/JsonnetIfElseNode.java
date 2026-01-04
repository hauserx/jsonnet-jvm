package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import jsonnetjvm.truffle.JsonnetTypesGen;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetIfElseNode extends JsonnetExpressionNode {
    @Child
    private JsonnetExpressionNode condition;
    @Child
    private JsonnetExpressionNode thenExpr;
    @Child
    private JsonnetExpressionNode elseExpr;

    public JsonnetIfElseNode(JsonnetExpressionNode condition, JsonnetExpressionNode thenExpr,
            JsonnetExpressionNode elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        boolean cond;
        try {
            cond = condition.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            cond = JsonnetTypesGen.asBoolean(e.getResult());
        }

        if (cond) {
            return thenExpr.executeGeneric(frame);
        } else {
            return elseExpr.executeGeneric(frame);
        }
    }
}

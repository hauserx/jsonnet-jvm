package jsonnetjvm.truffle.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.runtime.ArrayBuilder;
import jsonnetjvm.runtime.JArray;
import jsonnetjvm.runtime.JGenericArray;
import jsonnetjvm.runtime.JNumberArray;
import jsonnetjvm.runtime.JStringArray;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;

public class JsonnetArrayComprehensionNode extends JsonnetExpressionNode {
    @Child
    private JsonnetToValNode resultToValNode;
    private final int[] slots;
    @Children
    private final JsonnetExpressionNode[] listNodes;

    public JsonnetArrayComprehensionNode(JsonnetExpressionNode resultExpr, int[] slots,
            JsonnetExpressionNode[] listNodes) {
        this.resultToValNode = JsonnetToValNodeGen.create(resultExpr);
        this.slots = slots;
        this.listNodes = listNodes;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return doEvaluate(frame.materialize());
    }

    @TruffleBoundary
    private JArray doEvaluate(MaterializedFrame frame) {
        // Calculate total iterations to pre-allocate
        int totalIterations = 1;
        JArray[] lists = new JArray[listNodes.length];
        for (int i = 0; i < listNodes.length; i++) {
            Object res = listNodes[i].executeGeneric(frame);
            if (!(res instanceof JArray)) {
                throw new RuntimeException("Comprehension source is not an array");
            }
            lists[i] = (JArray) res;
            totalIterations *= lists[i].size();
        }

        ArrayBuilder builder = new ArrayBuilder(totalIterations);
        evaluateNested(frame, 0, lists, builder);
        return builder.build();
    }

    private void evaluateNested(MaterializedFrame frame, int depth, JArray[] lists, ArrayBuilder builder) {
        if (depth == lists.length) {
            builder.add(resultToValNode.executeGeneric(frame));
            return;
        }

        JArray list = lists[depth];
        if (list instanceof JNumberArray) {
            JNumberArray nList = (JNumberArray) list;
            for (int i = 0; i < nList.size(); i++) {
                frame.setObject(slots[depth], nList.getDouble(i));
                evaluateNested(frame, depth + 1, lists, builder);
            }
        } else if (list instanceof JStringArray) {
            JStringArray sList = (JStringArray) list;
            for (int i = 0; i < sList.size(); i++) {
                frame.setObject(slots[depth], sList.getString(i));
                evaluateNested(frame, depth + 1, lists, builder);
            }
        } else {
            JGenericArray gList = (JGenericArray) list;
            for (int i = 0; i < gList.size(); i++) {
                frame.setObject(slots[depth], gList.get(i));
                evaluateNested(frame, depth + 1, lists, builder);
            }
        }
    }
}
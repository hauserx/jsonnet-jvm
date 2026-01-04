package jsonnetjvm.truffle.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import jsonnetjvm.truffle.JsonnetLanguage;
import jsonnetjvm.truffle.JsonnetTypes;
import jsonnetjvm.truffle.JsonnetTypesGen;

@TypeSystemReference(JsonnetTypes.class)
public abstract class JsonnetExpressionNode extends Node {

    public abstract Object executeGeneric(VirtualFrame frame);

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return JsonnetTypesGen.expectBoolean(executeGeneric(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return JsonnetTypesGen.expectDouble(executeGeneric(frame));
    }

    public String executeString(VirtualFrame frame) throws UnexpectedResultException {
        return JsonnetTypesGen.expectString(executeGeneric(frame));
    }

    public RootNode asRootNode(JsonnetLanguage language, com.oracle.truffle.api.frame.FrameDescriptor frameDescriptor) {
        return new JsonnetRootNode(language, frameDescriptor, this);
    }
}

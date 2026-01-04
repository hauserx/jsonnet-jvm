package jsonnetjvm.truffle.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import jsonnetjvm.truffle.JsonnetLanguage;

public class JsonnetRootNode extends RootNode {

    @Child
    private JsonnetExpressionNode body;

    public JsonnetRootNode(JsonnetLanguage language, FrameDescriptor frameDescriptor, JsonnetExpressionNode body) {
        super(language, frameDescriptor);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }
}

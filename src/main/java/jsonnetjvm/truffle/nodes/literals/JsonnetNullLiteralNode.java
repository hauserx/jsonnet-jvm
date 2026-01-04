package jsonnetjvm.truffle.nodes.literals;

import com.oracle.truffle.api.frame.VirtualFrame;
import jsonnetjvm.runtime.JNull;
import jsonnetjvm.runtime.Val;

public class JsonnetNullLiteralNode extends JsonnetLiteralNode {
    @Override
    public Val executeGeneric(VirtualFrame frame) {
        return JNull.INSTANCE;
    }
}

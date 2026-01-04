package jsonnetjvm.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class JFunction extends Val {
    private final CallTarget callTarget;
    private final String name;

    public JFunction(String name, CallTarget callTarget) {
        this.callTarget = callTarget;
        this.name = name;
    }

    public CallTarget getCallTarget() {
        return callTarget;
    }

    @Override
    @TruffleBoundary
    public String toStringValue() {
        return "function " + name;
    }

    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments) throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        return callTarget.call(arguments);
    }

    @Override
    @TruffleBoundary
    public String toJson() {
        throw new UnsupportedOperationException("Functions cannot be serialized to JSON");
    }
}
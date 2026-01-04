package jsonnetjvm.truffle;

import com.oracle.truffle.api.dsl.TypeSystem;
import jsonnetjvm.runtime.JArray;
import jsonnetjvm.runtime.JFunction;
import jsonnetjvm.runtime.JNull;

@TypeSystem({double.class, boolean.class, String.class, JArray.class, JFunction.class, JNull.class})
public class JsonnetTypes {
}

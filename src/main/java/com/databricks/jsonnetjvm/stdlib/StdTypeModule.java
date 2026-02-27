package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetJson;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetEqualNode;
import java.util.function.Function;

/** Type-checking and equality functions for the std library. */
public class StdTypeModule {

  public static void register(JObject std, JsonnetLanguage language) {
    std.addField(
        "type",
        () ->
            createFunction(
                language,
                "type",
                new String[] {"x"},
                args -> {
                  Object x = args[0];
                  return typeName(x);
                }));

    std.addField(
        "isString",
        () ->
            createFunction(
                language, "isString", new String[] {"v"}, args -> args[0] instanceof String));
    std.addField(
        "isBoolean",
        () ->
            createFunction(
                language, "isBoolean", new String[] {"v"}, args -> args[0] instanceof Boolean));
    std.addField(
        "isNumber",
        () ->
            createFunction(
                language, "isNumber", new String[] {"v"}, args -> args[0] instanceof Double));
    std.addField(
        "isObject",
        () ->
            createFunction(
                language, "isObject", new String[] {"v"}, args -> args[0] instanceof JObject));
    std.addField(
        "isArray",
        () ->
            createFunction(
                language, "isArray", new String[] {"v"}, args -> args[0] instanceof JArray));
    std.addField(
        "isFunction",
        () ->
            createFunction(
                language, "isFunction", new String[] {"v"}, args -> args[0] instanceof JFunction));
    std.addField(
        "isNull",
        () ->
            createFunction(
                language, "isNull", new String[] {"v"}, args -> args[0] instanceof JNull));

    std.addField(
        "assertEqual",
        () ->
            createFunction(
                language,
                "assertEqual",
                new String[] {"a", "b"},
                args -> {
                  Object a = args[0];
                  Object b = args[1];
                  if (JsonnetEqualNode.deepEquals(a, b)) {
                    return true;
                  }
                  throw new JsonnetException(
                      "assertEqual failed: " + materialize(a) + " != " + materialize(b));
                }));

    std.addField(
        "equals",
        () ->
            createFunction(
                language,
                "equals",
                new String[] {"a", "b"},
                args -> {
                  return JsonnetEqualNode.deepEquals(args[0], args[1]);
                }));

    std.addField(
        "trace",
        () ->
            createFunction(
                language,
                "trace",
                new String[] {"str", "rest"},
                args -> {
                  if (args.length < 2)
                    throw new JsonnetException("Function parameters str, rest not bound in call");
                  if (!(args[0] instanceof String))
                    throw new JsonnetException(
                        "Wrong parameter type: expected String, got "
                            + JsonnetException.typeName(args[0]));
                  String msg = (String) args[0];
                  Object value = args[1];
                  if (!msg.isEmpty()) {
                    System.err.println("TRACE: " + msg);
                  }
                  return value;
                }));

    std.addField(
        "primitiveEquals",
        () ->
            createFunction(
                language,
                "primitiveEquals",
                new String[] {"x", "y"},
                args -> {
                  Object x = args[0];
                  Object y = args[1];
                  String xType = typeName(x);
                  String yType = typeName(y);
                  if (!xType.equals(yType)) {
                    return false;
                  }
                  if (x instanceof Double) {
                    return ((Double) x).doubleValue() == ((Double) y).doubleValue();
                  }
                  if (x instanceof String) {
                    return x.equals(y);
                  }
                  if (x instanceof Boolean) {
                    return x.equals(y);
                  }
                  if (x instanceof JNull) {
                    return true;
                  }
                  throw new JsonnetException(
                      "primitiveEquals operates on primitive types, got "
                          + xType
                          + " and "
                          + yType);
                }));
  }

  private static String typeName(Object x) {
    if (x instanceof JNull) return "null";
    if (x instanceof Boolean) return "boolean";
    if (x instanceof Double) return "number";
    if (x instanceof String) return "string";
    if (x instanceof JArray) return "array";
    if (x instanceof JObject) return "object";
    if (x instanceof JFunction) return "function";
    return "unknown";
  }

  private static String materialize(Object val) {
    return JsonnetJson.renderCompact(val);
  }

  private static JFunction createFunction(
      JsonnetLanguage language, String name, String[] paramNames, Function<Object[], Object> body) {
    JsonnetBuiltinRootNode rootNode = new JsonnetBuiltinRootNode(language, name, body);
    return new JFunction(name, rootNode.getCallTarget(), null, paramNames.length, paramNames);
  }
}

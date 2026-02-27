package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetStrings;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/** Object functions for the std library. */
public class StdObjectModule {

  public static void register(JObject std, JsonnetLanguage language) {
    // objectHas(o, f) -> boolean (visible fields only)
    std.addField(
        "objectHas",
        () ->
            fn(
                language,
                "objectHas",
                new String[] {"o", "f"},
                args -> {
                  JObject obj = obj(args[0]);
                  String field = str(args[1]);
                  return obj.hasField(field);
                }));

    // objectHasAll(o, f) -> boolean (all fields including hidden)
    std.addField(
        "objectHasAll",
        () ->
            fn(
                language,
                "objectHasAll",
                new String[] {"o", "f"},
                args -> {
                  JObject obj = obj(args[0]);
                  String field = str(args[1]);
                  return obj.hasFieldAll(field);
                }));

    // objectHasEx(o, k, inc_hidden) -> boolean
    std.addField(
        "objectHasEx",
        () ->
            fn(
                language,
                "objectHasEx",
                new String[] {"o", "k", "inc_hidden"},
                args -> {
                  JObject obj = obj(args[0]);
                  String field = str(args[1]);
                  boolean incHidden = bool(args[2]);
                  if (incHidden) {
                    return obj.getAllFieldNames().contains(field);
                  }
                  return obj.getFieldNames().contains(field);
                }));

    // objectFields(o) -> array of field names (visible), sorted
    std.addField(
        "objectFields",
        () ->
            fn(
                language,
                "objectFields",
                new String[] {"o"},
                args -> {
                  JObject obj = obj(args[0]);
                  List<Object> result = new ArrayList<>(obj.getFieldNames());
                  result.sort((a, b) -> JsonnetStrings.compareByCodepoint((String) a, (String) b));
                  return new JGenericArray(result);
                }));

    // objectFieldsAll(o) -> array of all field names, sorted
    std.addField(
        "objectFieldsAll",
        () ->
            fn(
                language,
                "objectFieldsAll",
                new String[] {"o"},
                args -> {
                  JObject obj = obj(args[0]);
                  List<Object> result = new ArrayList<>(obj.getAllFieldNames());
                  result.sort((a, b) -> JsonnetStrings.compareByCodepoint((String) a, (String) b));
                  return new JGenericArray(result);
                }));

    // objectFieldsEx(o, inc_hidden) -> array of field names
    std.addField(
        "objectFieldsEx",
        () ->
            fn(
                language,
                "objectFieldsEx",
                new String[] {"o", "inc_hidden"},
                args -> {
                  JObject obj = obj(args[0]);
                  boolean incHidden = bool(args[1]);
                  Set<String> keys = incHidden ? obj.getAllFieldNames() : obj.getFieldNames();
                  List<Object> result = new ArrayList<>(keys);
                  result.sort((a, b) -> JsonnetStrings.compareByCodepoint((String) a, (String) b));
                  return new JGenericArray(result);
                }));

    // objectValues(o) -> array of field values (visible)
    std.addField(
        "objectValues",
        () ->
            fn(
                language,
                "objectValues",
                new String[] {"o"},
                args -> {
                  JObject obj = obj(args[0]);
                  Set<String> keys = obj.getFieldNames();
                  List<Object> result = new ArrayList<>(keys.size());
                  for (String k : keys) result.add(obj.getField(k));
                  return new JGenericArray(result);
                }));

    // objectValuesAll(o) -> array of all field values
    std.addField(
        "objectValuesAll",
        () ->
            fn(
                language,
                "objectValuesAll",
                new String[] {"o"},
                args -> {
                  JObject obj = obj(args[0]);
                  Set<String> keys = obj.getAllFieldNames();
                  List<Object> result = new ArrayList<>(keys.size());
                  for (String k : keys) result.add(obj.getField(k));
                  return new JGenericArray(result);
                }));

    // objectKeysValues(o) -> array of {key, value} objects
    std.addField(
        "objectKeysValues",
        () ->
            fn(
                language,
                "objectKeysValues",
                new String[] {"o"},
                args -> {
                  JObject obj = obj(args[0]);
                  return keysValues(obj);
                }));

    // objectKeysValuesAll(o) -> array of {key, value} objects (all fields)
    std.addField(
        "objectKeysValuesAll",
        () ->
            fn(
                language,
                "objectKeysValuesAll",
                new String[] {"o"},
                args -> {
                  JObject obj = obj(args[0]);
                  return keysValuesAll(obj);
                }));

    std.addField(
        "objectRemoveKey",
        () ->
            fn(
                language,
                "objectRemoveKey",
                new String[] {"obj", "key"},
                args -> {
                  JObject obj = obj(args[0]);
                  String key = str(args[1]);
                  return obj.removeKey(key);
                }));

    // get(o, f, default=null, inc_hidden=true) -> value
    std.addField(
        "get",
        () ->
            lazyFn(
                language,
                "get",
                new String[] {"o", "f", "default", "inc_hidden"},
                args -> {
                  JObject obj = obj(JsonnetThunk.forceIfThunk(args[0]));
                  String field = str(JsonnetThunk.forceIfThunk(args[1]));
                  boolean incHidden =
                      args.length > 3 && args[3] != JFunction.UNSET_ARG
                          ? bool(JsonnetThunk.forceIfThunk(args[3]))
                          : true;
                  boolean exists = incHidden ? obj.hasFieldAll(field) : obj.hasField(field);
                  if (exists) {
                    return obj.getField(field);
                  }
                  return args.length > 2 && args[2] != JFunction.UNSET_ARG
                      ? JsonnetThunk.forceIfThunk(args[2])
                      : JNull.INSTANCE;
                }));

    // mapWithKey(func, obj) -> new object
    std.addField(
        "mapWithKey",
        () ->
            fn(
                language,
                "mapWithKey",
                new String[] {"func", "obj"},
                args -> {
                  JFunction func = func(args[0]);
                  JObject obj = obj(args[1]);
                  JObject result = new JObject();
                  for (String k : obj.getFieldNames()) {
                    final String key = k;
                    result.addField(key, () -> func.call(key, obj.getField(key)));
                  }
                  return result;
                }));

    // mergePatch(target, patch) -> merged object
    std.addField(
        "mergePatch",
        () ->
            fn(
                language,
                "mergePatch",
                new String[] {"target", "patch"},
                args -> {
                  Object target = args[0];
                  Object patch = args[1];
                  return mergePatch(target, patch);
                }));

    // prune(a) -> pruned value (remove null, empty arrays, empty objects)
    std.addField("prune", () -> fn(language, "prune", new String[] {"a"}, args -> prune(args[0])));
  }

  private static JArray keysValues(JObject obj) {
    Set<String> keys = obj.getFieldNames();
    List<Object> result = new ArrayList<>(keys.size());
    for (String k : keys) {
      JObject kv = new JObject();
      Object val = obj.getField(k);
      kv.addField("key", () -> k);
      kv.addField("value", () -> val);
      result.add(kv);
    }
    return new JGenericArray(result);
  }

  private static JArray keysValuesAll(JObject obj) {
    Set<String> keys = obj.getAllFieldNames();
    List<Object> result = new ArrayList<>(keys.size());
    for (String k : keys) {
      JObject kv = new JObject();
      Object val = obj.getField(k);
      kv.addField("key", () -> k);
      kv.addField("value", () -> val);
      result.add(kv);
    }
    return new JGenericArray(result);
  }

  private static Object mergePatch(Object target, Object patch) {
    if (patch instanceof JObject patchObj) {
      JObject targetObj = (target instanceof JObject) ? (JObject) target : new JObject();
      JObject result = new JObject();

      for (String k : targetObj.getFieldNames()) {
        if (patchObj.hasField(k)) {
          Object patchVal = patchObj.getField(k);
          if (!(patchVal instanceof JNull)) {
            Supplier<Object> targetSupplier = targetObj.getFieldSupplier(k);
            result.addField(
                k,
                () -> {
                  Object tv = targetSupplier.get();
                  if (tv instanceof JObject && patchVal instanceof JObject) {
                    return mergePatch(tv, patchVal);
                  }
                  return patchVal;
                });
          }
          // else: null in patch means remove the key
        } else {
          Supplier<Object> supplier = targetObj.getFieldSupplier(k);
          result.addField(k, supplier);
        }
      }

      for (String k : patchObj.getFieldNames()) {
        if (!targetObj.hasField(k)) {
          Object patchVal = patchObj.getField(k);
          if (!(patchVal instanceof JNull)) {
            Object merged = mergePatch(new JObject(), patchVal);
            result.addField(k, () -> merged);
          }
        }
      }

      return result;
    }
    return patch;
  }

  private static Object prune(Object val) {
    if (val instanceof JObject) {
      JObject obj = (JObject) val;
      JObject result = new JObject();
      for (String k : obj.getFieldNames()) {
        Object v = prune(obj.getField(k));
        if (!shouldPrune(v)) {
          result.addField(k, () -> v);
        }
      }
      return result;
    } else if (val instanceof JArray) {
      JArray arr = (JArray) val;
      List<Object> result = new ArrayList<>();
      for (int i = 0; i < arr.size(); i++) {
        Object v = prune(arr.get(i));
        if (!shouldPrune(v)) {
          result.add(v);
        }
      }
      return new JGenericArray(result);
    }
    return val;
  }

  private static boolean shouldPrune(Object val) {
    if (val instanceof JNull) return true;
    if (val instanceof JArray && ((JArray) val).size() == 0) return true;
    if (val instanceof JObject && ((JObject) val).getFieldNames().isEmpty()) return true;
    return false;
  }

  private static JObject obj(Object arg) {
    if (arg instanceof JObject) return (JObject) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected Object, got " + JsonnetException.typeName(arg));
  }

  private static String str(Object arg) {
    if (arg instanceof String) return (String) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected String, got " + JsonnetException.typeName(arg));
  }

  private static boolean bool(Object arg) {
    if (arg instanceof Boolean) return (Boolean) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected Boolean, got " + JsonnetException.typeName(arg));
  }

  private static JFunction func(Object arg) {
    if (arg instanceof JFunction) return (JFunction) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected Function, got " + JsonnetException.typeName(arg));
  }

  private static JFunction fn(
      JsonnetLanguage language, String name, String[] paramNames, Function<Object[], Object> body) {
    return new JFunction(
        name,
        new JsonnetBuiltinRootNode(language, name, body).getCallTarget(),
        null,
        paramNames.length,
        paramNames);
  }

  /**
   * Builtin that receives raw thunks — must force args explicitly via JsonnetThunk.forceIfThunk.
   */
  private static JFunction lazyFn(
      JsonnetLanguage language, String name, String[] paramNames, Function<Object[], Object> body) {
    return new JFunction(
        name,
        new JsonnetBuiltinRootNode(language, name, body, true).getCallTarget(),
        null,
        paramNames.length,
        paramNames);
  }
}

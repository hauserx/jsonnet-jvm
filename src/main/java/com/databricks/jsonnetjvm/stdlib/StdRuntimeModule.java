package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetConfig;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import com.oracle.truffle.api.source.Source;
import java.util.Map;
import java.util.function.Function;

/** Runtime stdlib functions: extVar, thisFile, native. */
public class StdRuntimeModule {

  public static void register(JObject std, JsonnetLanguage language) {
    std.addField(
        "extVar",
        () ->
            createFunction(
                language,
                "extVar",
                new String[] {"x"},
                args -> {
                  String name = (String) args[0];
                  JsonnetConfig config = JsonnetConfig.get();
                  Map<String, Object> cache = config.getExtVarCache();
                  Object cached = cache.get(name);
                  if (cached != null) {
                    return cached;
                  }
                  String code = config.getExtVars().get(name);
                  if (code == null) {
                    throw new JsonnetException("Unknown extVar: " + name);
                  }
                  Source source =
                      Source.newBuilder(JsonnetLanguage.ID, code, "<extvar:" + name + ">").build();
                  Object result = language.parseSource(source).call();
                  cache.put(name, result);
                  return result;
                }));

    std.addField("thisFile", () -> JsonnetConfig.getCurrentFile());

    std.addField(
        "native",
        () ->
            createFunction(
                language,
                "native",
                new String[] {"name"},
                args -> {
                  return resolveNative(language, (String) args[0]);
                }));
  }

  private static Object resolveNative(JsonnetLanguage language, String name) {
    JsonnetConfig config = JsonnetConfig.get();
    JsonnetConfig.NativeFunctionDef def = config.getCustomNatives().get(name);
    if (def != null) {
      return createFunction(language, name, def.paramNames(), def.body());
    }
    return JNull.INSTANCE;
  }

  static JFunction createFunction(
      JsonnetLanguage language, String name, String[] paramNames, Function<Object[], Object> body) {
    JsonnetBuiltinRootNode rootNode = new JsonnetBuiltinRootNode(language, name, body);
    return new JFunction(name, rootNode.getCallTarget(), null, paramNames.length, paramNames);
  }
}

package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.JsonnetEvaluator;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Native regex functions using RE2J, matching sjsonnet's NativeRegex. */
public class NativeRegex {

  private static final ConcurrentHashMap<String, Pattern> CACHE = new ConcurrentHashMap<>();
  private static final Pattern DASH = getPattern("-");

  private static Pattern getPattern(String pat) {
    return CACHE.computeIfAbsent(pat, Pattern::compile);
  }

  public static void register(JsonnetEvaluator evaluator) {
    evaluator.registerNative(
        "regexPartialMatch",
        new String[] {"pattern", "str"},
        args -> {
          return regexPartialMatch((String) args[0], (String) args[1]);
        });
    evaluator.registerNative(
        "regexFullMatch",
        new String[] {"pattern", "str"},
        args -> {
          return regexPartialMatch("^" + args[0] + "$", (String) args[1]);
        });
    evaluator.registerNative(
        "regexReplace",
        new String[] {"str", "pattern", "to"},
        args -> {
          return getPattern((String) args[1])
              .matcher((String) args[0])
              .replaceFirst((String) args[2]);
        });
    evaluator.registerNative(
        "regexGlobalReplace",
        new String[] {"str", "pattern", "to"},
        args -> {
          return getPattern((String) args[1])
              .matcher((String) args[0])
              .replaceAll((String) args[2]);
        });
    evaluator.registerNative(
        "regexQuoteMeta",
        new String[] {"str"},
        args -> {
          return regexQuote((String) args[0]);
        });
  }

  private static Object regexPartialMatch(String patternStr, String str) {
    Pattern compiled = getPattern(patternStr);
    Matcher matcher = compiled.matcher(str);
    if (!matcher.find()) {
      return JNull.INSTANCE;
    }

    List<Object> captures = new ArrayList<>();
    for (int i = 1; i <= matcher.groupCount(); i++) {
      String g = matcher.group(i);
      captures.add(g != null ? g : "");
    }

    JObject namedCaptures = new JObject();
    Map<String, Integer> namedGroups = compiled.namedGroups();
    if (namedGroups != null) {
      for (Map.Entry<String, Integer> entry : namedGroups.entrySet()) {
        String name = entry.getKey();
        int idx = entry.getValue();
        String val = captures.size() >= idx ? (String) captures.get(idx - 1) : "";
        namedCaptures.addField(name, () -> val);
      }
    }

    JObject result = new JObject();
    result.addField("string", () -> str);
    result.addField("captures", () -> new JGenericArray(captures));
    result.addField("namedCaptures", () -> namedCaptures);
    return result;
  }

  private static String regexQuote(String s) {
    String quoted = Pattern.quote(s);
    Matcher m = DASH.matcher(quoted);
    if (m.find()) {
      return m.replaceAll("\\\\-");
    }
    return quoted;
  }
}

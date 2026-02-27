package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetStrings;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Set operations for the std library. Operates on sorted, unique arrays. */
public class StdSetModule {

  public static void register(JObject std, JsonnetLanguage language) {
    // setUnion(a, b, keyF=identity) -> sorted unique array
    std.addField(
        "setUnion",
        () ->
            fn(
                language,
                "setUnion",
                new String[] {"a", "b", "keyF"},
                args -> {
                  JArray a = arr(args[0]);
                  JArray b = arr(args[1]);
                  JFunction keyF =
                      args.length > 2 && args[2] instanceof JFunction ? (JFunction) args[2] : null;

                  if (a.size() == 0) return b;
                  if (b.size() == 0) return a;

                  List<Object> result = new ArrayList<>(a.size() + b.size());
                  int idxA = 0, idxB = 0;
                  while (idxA < a.size() && idxB < b.size()) {
                    Object elemA = a.get(idxA);
                    Object elemB = b.get(idxB);
                    Object keyA = keyF != null ? keyF.call(elemA) : elemA;
                    Object keyB = keyF != null ? keyF.call(elemB) : elemB;
                    int cmp = compareValues(keyA, keyB);
                    if (cmp < 0) {
                      result.add(elemA);
                      idxA++;
                    } else if (cmp > 0) {
                      result.add(elemB);
                      idxB++;
                    } else {
                      result.add(elemA);
                      idxA++;
                      idxB++;
                    }
                  }
                  while (idxA < a.size()) result.add(a.get(idxA++));
                  while (idxB < b.size()) result.add(b.get(idxB++));
                  return new JGenericArray(result);
                }));

    // setInter(a, b, keyF=identity) -> sorted intersection
    std.addField(
        "setInter",
        () ->
            fn(
                language,
                "setInter",
                new String[] {"a", "b", "keyF"},
                args -> {
                  JArray a = arr(args[0]);
                  JArray b = arr(args[1]);
                  JFunction keyF =
                      args.length > 2 && args[2] instanceof JFunction ? (JFunction) args[2] : null;

                  List<Object> result = new ArrayList<>(Math.min(a.size(), b.size()));
                  int idxA = 0, idxB = 0;
                  while (idxA < a.size() && idxB < b.size()) {
                    Object elemA = a.get(idxA);
                    Object elemB = b.get(idxB);
                    Object keyA = keyF != null ? keyF.call(elemA) : elemA;
                    Object keyB = keyF != null ? keyF.call(elemB) : elemB;
                    int cmp = compareValues(keyA, keyB);
                    if (cmp < 0) {
                      idxA++;
                    } else if (cmp > 0) {
                      idxB++;
                    } else {
                      result.add(elemA);
                      idxA++;
                      idxB++;
                    }
                  }
                  return new JGenericArray(result);
                }));

    // setDiff(a, b, keyF=identity) -> elements in a but not b
    std.addField(
        "setDiff",
        () ->
            fn(
                language,
                "setDiff",
                new String[] {"a", "b", "keyF"},
                args -> {
                  JArray a = arr(args[0]);
                  JArray b = arr(args[1]);
                  JFunction keyF =
                      args.length > 2 && args[2] instanceof JFunction ? (JFunction) args[2] : null;

                  List<Object> result = new ArrayList<>(a.size());
                  int idxA = 0, idxB = 0;
                  while (idxA < a.size()) {
                    Object elemA = a.get(idxA);
                    Object keyA = keyF != null ? keyF.call(elemA) : elemA;
                    boolean foundEqual = false;
                    while (idxB < b.size()) {
                      Object elemB = b.get(idxB);
                      Object keyB = keyF != null ? keyF.call(elemB) : elemB;
                      int cmp = compareValues(keyA, keyB);
                      if (cmp <= 0) {
                        foundEqual = (cmp == 0);
                        if (foundEqual) idxB++;
                        break;
                      }
                      idxB++;
                    }
                    if (!foundEqual) result.add(elemA);
                    idxA++;
                  }
                  return new JGenericArray(result);
                }));

    // setMember(x, arr, keyF=identity) -> boolean
    std.addField(
        "setMember",
        () ->
            fn(
                language,
                "setMember",
                new String[] {"x", "arr", "keyF"},
                args -> {
                  Object x = args[0];
                  JArray a = arr(args[1]);
                  JFunction keyF =
                      args.length > 2 && args[2] instanceof JFunction ? (JFunction) args[2] : null;
                  Object xKey = keyF != null ? keyF.call(x) : x;
                  // Binary search on sorted array
                  int lo = 0, hi = a.size() - 1;
                  while (lo <= hi) {
                    int mid = (lo + hi) / 2;
                    Object midKey = keyF != null ? keyF.call(a.get(mid)) : a.get(mid);
                    int cmp = compareValues(xKey, midKey);
                    if (cmp == 0) return true;
                    if (cmp < 0) hi = mid - 1;
                    else lo = mid + 1;
                  }
                  return false;
                }));
  }

  static int compareValues(Object a, Object b) {
    if (a instanceof Double && b instanceof Double) {
      return Double.compare((Double) a, (Double) b);
    }
    if (a instanceof String && b instanceof String) {
      return JsonnetStrings.compareByCodepoint((String) a, (String) b);
    }
    if (a instanceof Boolean && b instanceof Boolean) {
      return Boolean.compare((Boolean) a, (Boolean) b);
    }
    return 0;
  }

  private static JArray arr(Object arg) {
    if (arg instanceof JArray) return (JArray) arg;
    if (arg instanceof String s) {
      List<Object> chars = new ArrayList<>(s.length());
      for (int i = 0; i < s.length(); i++) {
        chars.add(String.valueOf(s.charAt(i)));
      }
      return new JGenericArray(chars);
    }
    throw new JsonnetException(
        "Wrong parameter type: expected Array, got " + JsonnetException.typeName(arg));
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
}

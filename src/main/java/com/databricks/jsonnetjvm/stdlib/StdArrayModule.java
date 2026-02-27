package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JNumberArray;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetStrings;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetEqualNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Array functions for the std library. Includes migrated functions from the old Std.java (range,
 * sum, length).
 */
public class StdArrayModule {

  public static void register(JObject std, JsonnetLanguage language) {
    // length works on arrays, strings, and objects
    std.addField(
        "length",
        () ->
            fn(
                language,
                "length",
                new String[] {"x"},
                args -> {
                  Object x = args[0];
                  if (x instanceof JArray) return (double) ((JArray) x).size();
                  if (x instanceof String s) return (double) s.codePointCount(0, s.length());
                  if (x instanceof JObject) return (double) ((JObject) x).getFieldNames().size();
                  if (x instanceof JFunction f) return (double) Math.max(0, f.getParamCount());
                  throw new JsonnetException(
                      "std.length: expected array, string, object, or function, got "
                          + typeName(x));
                }));

    // range(from, to) -> array of integers [from..to]
    std.addField(
        "range",
        () ->
            fn(
                language,
                "range",
                new String[] {"from", "to"},
                args -> {
                  int start = (int) num(args[0]);
                  int end = (int) num(args[1]);
                  if (end < start) {
                    return new JGenericArray(new ArrayList<>());
                  }
                  double[] array = new double[end - start + 1];
                  for (int i = 0; i <= end - start; i++) {
                    array[i] = start + i;
                  }
                  return new JNumberArray(array);
                }));

    // sum(arr) -> number
    std.addField(
        "sum",
        () ->
            fn(
                language,
                "sum",
                new String[] {"arr"},
                args -> {
                  JArray array = arr(args[0]);
                  double total = 0.0;
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof Double) {
                      total += (Double) item;
                    } else if (item instanceof Boolean) {
                      total += (Boolean) item ? 1 : 0;
                    }
                  }
                  return total;
                }));

    // makeArray(sz, func) -> array
    std.addField(
        "makeArray",
        () ->
            fn(
                language,
                "makeArray",
                new String[] {"sz", "func"},
                args -> {
                  int sz = (int) num(args[0]);
                  if (sz < 0)
                    throw new JsonnetException("index value is not a positive integer, got: " + sz);
                  JFunction func = func(args[1]);
                  List<Object> items = new ArrayList<>(sz);
                  for (int i = 0; i < sz; i++) {
                    items.add(func.call((double) i));
                  }
                  return new JGenericArray(items);
                }));

    // map(func, arr_or_str) -> array
    std.addField(
        "map",
        () ->
            fn(
                language,
                "map",
                new String[] {"func", "arr"},
                args -> {
                  JFunction func = func(args[0]);
                  if (args[1] instanceof String s) {
                    List<Object> result = new ArrayList<>(s.codePointCount(0, s.length()));
                    for (int i = 0; i < s.length(); ) {
                      int cp = s.codePointAt(i);
                      String ch = new String(Character.toChars(cp));
                      result.add(new JsonnetThunk(() -> func.call(ch)));
                      i += Character.charCount(cp);
                    }
                    return new JGenericArray(result);
                  }
                  JArray array = arr(args[1]);
                  List<Object> result = new ArrayList<>(array.size());
                  for (int i = 0; i < array.size(); i++) {
                    result.add(func.call(array.get(i)));
                  }
                  return new JGenericArray(result);
                }));

    // mapWithIndex(func, arr) -> array
    std.addField(
        "mapWithIndex",
        () ->
            fn(
                language,
                "mapWithIndex",
                new String[] {"func", "arr"},
                args -> {
                  JFunction func = func(args[0]);
                  JArray array = arr(args[1]);
                  List<Object> result = new ArrayList<>(array.size());
                  for (int i = 0; i < array.size(); i++) {
                    result.add(func.call((double) i, array.get(i)));
                  }
                  return new JGenericArray(result);
                }));

    // filter(func, arr) -> array
    std.addField(
        "filter",
        () ->
            fn(
                language,
                "filter",
                new String[] {"func", "arr"},
                args -> {
                  JFunction func = func(args[0]);
                  JArray array = arr(args[1]);
                  List<Object> result = new ArrayList<>();
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    Object keep = func.call(item);
                    if (!(keep instanceof Boolean)) {
                      throw new JsonnetException(
                          "filter function must return boolean, got "
                              + JsonnetException.typeName(keep));
                    }
                    if ((Boolean) keep) {
                      result.add(item);
                    }
                  }
                  return new JGenericArray(result);
                }));

    // filterMap(filterFunc, mapFunc, arr) -> array
    std.addField(
        "filterMap",
        () ->
            fn(
                language,
                "filterMap",
                new String[] {"filter_func", "map_func", "arr"},
                args -> {
                  JFunction filterFunc = func(args[0]);
                  JFunction mapFunc = func(args[1]);
                  JArray array = arr(args[2]);
                  List<Object> result = new ArrayList<>();
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    Object keep = filterFunc.call(item);
                    if (!(keep instanceof Boolean)) {
                      throw new JsonnetException(
                          "filterMap filter function must return boolean, got "
                              + JsonnetException.typeName(keep));
                    }
                    if ((Boolean) keep) {
                      result.add(mapFunc.call(item));
                    }
                  }
                  return new JGenericArray(result);
                }));

    // flatMap(func, arr_or_str) -> array or string
    std.addField(
        "flatMap",
        () ->
            fn(
                language,
                "flatMap",
                new String[] {"func", "arr"},
                args -> {
                  JFunction func = func(args[0]);
                  if (args[1] instanceof String s) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < s.length(); ) {
                      int cp = s.codePointAt(i);
                      Object mapped = func.call(new String(Character.toChars(cp)));
                      i += Character.charCount(cp);
                      if (mapped instanceof JNull || mapped == null) {
                        // skip
                      } else if (mapped instanceof String ms) {
                        sb.append(ms);
                      } else {
                        throw new JsonnetException(
                            "flatMap func must return string, got " + typeName(mapped));
                      }
                    }
                    return sb.toString();
                  }
                  JArray array = arr(args[1]);
                  List<Object> result = new ArrayList<>();
                  for (int i = 0; i < array.size(); i++) {
                    Object mapped = func.call(array.get(i));
                    if (mapped instanceof JArray inner) {
                      for (int j = 0; j < inner.size(); j++) {
                        result.add(inner.get(j));
                      }
                    } else {
                      result.add(mapped);
                    }
                  }
                  return new JGenericArray(result);
                }));

    // foldl(func, arr, init) -> value
    std.addField(
        "foldl",
        () ->
            fn(
                language,
                "foldl",
                new String[] {"func", "arr", "init"},
                args -> {
                  JFunction func = func(args[0]);
                  JArray array =
                      args[1] instanceof String ? toCharArray((String) args[1]) : arr(args[1]);
                  Object acc = args[2];
                  for (int i = 0; i < array.size(); i++) {
                    acc = func.call(acc, array.get(i));
                  }
                  return acc;
                }));

    // foldr(func, arr, init) -> value
    std.addField(
        "foldr",
        () ->
            fn(
                language,
                "foldr",
                new String[] {"func", "arr", "init"},
                args -> {
                  JFunction func = func(args[0]);
                  JArray array =
                      args[1] instanceof String ? toCharArray((String) args[1]) : arr(args[1]);
                  Object acc = args[2];
                  for (int i = array.size() - 1; i >= 0; i--) {
                    acc = func.call(array.get(i), acc);
                  }
                  return acc;
                }));

    // sort(arr_or_str, keyF=identity) -> array
    std.addField(
        "sort",
        () ->
            fn(
                language,
                "sort",
                new String[] {"arr", "keyF"},
                args -> {
                  JArray array =
                      args[0] instanceof String ? toCharArray((String) args[0]) : arr(args[0]);
                  JFunction keyF = args.length > 1 ? func(args[1]) : null;
                  List<Object> items = new ArrayList<>(array.size());
                  for (int i = 0; i < array.size(); i++) {
                    items.add(array.get(i));
                  }
                  items.sort(
                      (a, b) -> {
                        Object ka = keyF != null ? keyF.call(a) : a;
                        Object kb = keyF != null ? keyF.call(b) : b;
                        return compareValues(ka, kb, keyF != null);
                      });
                  return new JGenericArray(items);
                }));

    // reverse(arr) -> array
    std.addField(
        "reverse",
        () ->
            fn(
                language,
                "reverse",
                new String[] {"arrs"},
                args -> {
                  JArray array = arr(args[0]);
                  List<Object> items = new ArrayList<>(array.size());
                  for (int i = array.size() - 1; i >= 0; i--) {
                    items.add(array.get(i));
                  }
                  return new JGenericArray(items);
                }));

    // member(arr_or_str, x) -> boolean
    std.addField(
        "member",
        () ->
            fn(
                language,
                "member",
                new String[] {"arr", "x"},
                args -> {
                  Object container = args[0];
                  Object x = args[1];
                  if (container instanceof String str) {
                    if (!(x instanceof String sub)) {
                      throw new JsonnetException("std.member: string member test requires string");
                    }
                    return str.contains(sub);
                  }
                  JArray array = arr(container);
                  for (int i = 0; i < array.size(); i++) {
                    if (jsonnetEquals(array.get(i), x)) return true;
                  }
                  return false;
                }));

    // count(arr, x) -> number
    std.addField(
        "count",
        () ->
            fn(
                language,
                "count",
                new String[] {"arr", "x"},
                args -> {
                  JArray array = arr(args[0]);
                  Object x = args[1];
                  int count = 0;
                  for (int i = 0; i < array.size(); i++) {
                    if (jsonnetEquals(array.get(i), x)) count++;
                  }
                  return (double) count;
                }));

    // find(value, arr) -> array of indices
    std.addField(
        "find",
        () ->
            fn(
                language,
                "find",
                new String[] {"value", "arr"},
                args -> {
                  Object value = args[0];
                  JArray array = arr(args[1]);
                  List<Object> indices = new ArrayList<>();
                  for (int i = 0; i < array.size(); i++) {
                    if (jsonnetEquals(array.get(i), value)) {
                      indices.add((double) i);
                    }
                  }
                  return new JGenericArray(indices);
                }));

    // contains(arr, elem) -> boolean
    std.addField(
        "contains",
        () ->
            fn(
                language,
                "contains",
                new String[] {"arr", "elem"},
                args -> {
                  JArray array = arr(args[0]);
                  Object elem = args[1];
                  for (int i = 0; i < array.size(); i++) {
                    if (jsonnetEquals(array.get(i), elem)) return true;
                  }
                  return false;
                }));

    // remove(arr, elem) -> array
    std.addField(
        "remove",
        () ->
            fn(
                language,
                "remove",
                new String[] {"arr", "elem"},
                args -> {
                  JArray array = arr(args[0]);
                  Object elem = args[1];
                  List<Object> result = new ArrayList<>();
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (!jsonnetEquals(item, elem)) {
                      result.add(item);
                    }
                  }
                  return new JGenericArray(result);
                }));

    // removeAt(arr, idx) -> array
    std.addField(
        "removeAt",
        () ->
            fn(
                language,
                "removeAt",
                new String[] {"arr", "idx"},
                args -> {
                  JArray array = arr(args[0]);
                  int idx = (int) num(args[1]);
                  List<Object> result = new ArrayList<>();
                  for (int i = 0; i < array.size(); i++) {
                    if (i != idx) result.add(array.get(i));
                  }
                  return new JGenericArray(result);
                }));

    // avg(arr) -> number
    std.addField(
        "avg",
        () ->
            fn(
                language,
                "avg",
                new String[] {"arr"},
                args -> {
                  JArray array = arr(args[0]);
                  if (array.size() == 0) throw new JsonnetException("std.avg: empty array");
                  double total = 0.0;
                  for (int i = 0; i < array.size(); i++) {
                    total += toNum(array.get(i));
                  }
                  return total / array.size();
                }));

    // all(arr) -> boolean
    std.addField(
        "all",
        () ->
            fn(
                language,
                "all",
                new String[] {"arr"},
                args -> {
                  JArray array = arr(args[0]);
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof Boolean && !(Boolean) item) return false;
                  }
                  return true;
                }));

    // any(arr) -> boolean
    std.addField(
        "any",
        () ->
            fn(
                language,
                "any",
                new String[] {"arr"},
                args -> {
                  JArray array = arr(args[0]);
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof Boolean && (Boolean) item) return true;
                  }
                  return false;
                }));

    // repeat(arr, n) -> array
    std.addField(
        "repeat",
        () ->
            fn(
                language,
                "repeat",
                new String[] {"what", "count"},
                args -> {
                  Object what = args[0];
                  int n = (int) num(args[1]);
                  if (what instanceof JArray) {
                    JArray array = (JArray) what;
                    List<Object> result = new ArrayList<>(array.size() * n);
                    for (int r = 0; r < n; r++) {
                      for (int i = 0; i < array.size(); i++) {
                        result.add(array.get(i));
                      }
                    }
                    return new JGenericArray(result);
                  } else if (what instanceof String) {
                    String s = (String) what;
                    StringBuilder sb = new StringBuilder(s.length() * n);
                    for (int r = 0; r < n; r++) sb.append(s);
                    return sb.toString();
                  }
                  throw new JsonnetException("std.repeat: expected array or string");
                }));

    // flattenArrays(arr) -> array
    std.addField(
        "flattenArrays",
        () ->
            fn(
                language,
                "flattenArrays",
                new String[] {"arrs"},
                args -> {
                  JArray outer = arr(args[0]);
                  List<Object> result = new ArrayList<>();
                  for (int i = 0; i < outer.size(); i++) {
                    Object item = outer.get(i);
                    if (item instanceof JArray) {
                      JArray inner = (JArray) item;
                      for (int j = 0; j < inner.size(); j++) {
                        result.add(inner.get(j));
                      }
                    } else {
                      result.add(item);
                    }
                  }
                  return new JGenericArray(result);
                }));

    // flattenDeepArray(arr) -> array
    std.addField(
        "flattenDeepArray",
        () ->
            fn(
                language,
                "flattenDeepArray",
                new String[] {"value"},
                args -> {
                  List<Object> result = new ArrayList<>();
                  flattenDeep(args[0], result);
                  return new JGenericArray(result);
                }));

    // minArray(arr, keyF=identity, onEmpty=error) -> value
    std.addField(
        "minArray",
        () ->
            fn(
                language,
                "minArray",
                new String[] {"arr", "keyF", "onEmpty"},
                args -> {
                  JArray array = arr(args[0]);
                  JFunction keyF =
                      args.length > 1 && args[1] instanceof JFunction ? func(args[1]) : null;
                  if (array.size() == 0) {
                    if (args.length > 2 && args[2] != JFunction.UNSET_ARG) return args[2];
                    throw new JsonnetException("Expected at least one element in array. Got none");
                  }
                  Object minVal = array.get(0);
                  Object minKey = keyF != null ? keyF.call(minVal) : minVal;
                  for (int i = 1; i < array.size(); i++) {
                    Object item = array.get(i);
                    Object key = keyF != null ? keyF.call(item) : item;
                    if (compareValues(key, minKey) < 0) {
                      minVal = item;
                      minKey = key;
                    }
                  }
                  return minVal;
                }));

    // maxArray(arr, keyF=identity, onEmpty=error) -> value
    std.addField(
        "maxArray",
        () ->
            fn(
                language,
                "maxArray",
                new String[] {"arr", "keyF", "onEmpty"},
                args -> {
                  JArray array = arr(args[0]);
                  JFunction keyF =
                      args.length > 1 && args[1] instanceof JFunction ? func(args[1]) : null;
                  if (array.size() == 0) {
                    if (args.length > 2 && args[2] != JFunction.UNSET_ARG) return args[2];
                    throw new JsonnetException("Expected at least one element in array. Got none");
                  }
                  Object maxVal = array.get(0);
                  Object maxKey = keyF != null ? keyF.call(maxVal) : maxVal;
                  for (int i = 1; i < array.size(); i++) {
                    Object item = array.get(i);
                    Object key = keyF != null ? keyF.call(item) : item;
                    if (compareValues(key, maxKey) > 0) {
                      maxVal = item;
                      maxKey = key;
                    }
                  }
                  return maxVal;
                }));

    // slice(indexable, from, to, step) -> array or string
    std.addField(
        "slice",
        () ->
            fn(
                language,
                "slice",
                new String[] {"indexable", "index", "end", "step"},
                args -> {
                  if (args[0] instanceof String s) {
                    int cpLen = s.codePointCount(0, s.length());
                    int from =
                        args.length > 1 && args[1] instanceof Double ? (int) num(args[1]) : 0;
                    int to =
                        args.length > 2 && args[2] instanceof Double ? (int) num(args[2]) : cpLen;
                    int step =
                        args.length > 3 && args[3] instanceof Double ? (int) num(args[3]) : 1;
                    if (from < 0) from = cpLen + from;
                    if (to < 0) to = cpLen + to;
                    if (from < 0) from = 0;
                    if (to > cpLen) to = cpLen;
                    if (step <= 0) step = 1;
                    StringBuilder sb = new StringBuilder();
                    for (int i = from; i < to; i += step) {
                      int offset = s.offsetByCodePoints(0, i);
                      int cp = s.codePointAt(offset);
                      sb.appendCodePoint(cp);
                    }
                    return sb.toString();
                  }
                  JArray array = arr(args[0]);
                  int from = args.length > 1 && args[1] instanceof Double ? (int) num(args[1]) : 0;
                  int to =
                      args.length > 2 && args[2] instanceof Double
                          ? (int) num(args[2])
                          : array.size();
                  int step = args.length > 3 && args[3] instanceof Double ? (int) num(args[3]) : 1;
                  if (from < 0) from = array.size() + from;
                  if (to < 0) to = array.size() + to;
                  if (from < 0) from = 0;
                  if (to > array.size()) to = array.size();
                  if (step <= 0) step = 1;
                  List<Object> result = new ArrayList<>();
                  for (int i = from; i < to; i += step) {
                    result.add(array.get(i));
                  }
                  return new JGenericArray(result);
                }));

    // uniq(arr, keyF=identity) -> array
    std.addField(
        "uniq",
        () ->
            fn(
                language,
                "uniq",
                new String[] {"arr", "keyF"},
                args -> {
                  JArray array = arr(args[0]);
                  JFunction keyF =
                      args.length > 1 && args[1] instanceof JFunction ? func(args[1]) : null;
                  List<Object> result = new ArrayList<>();
                  Object lastKey = null;
                  boolean first = true;
                  for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    Object key = keyF != null ? keyF.call(item) : item;
                    if (first || !jsonnetEquals(key, lastKey)) {
                      result.add(item);
                      lastKey = key;
                      first = false;
                    }
                  }
                  return new JGenericArray(result);
                }));

    // set(arr, keyF=identity) -> sorted unique array
    std.addField(
        "set",
        () ->
            fn(
                language,
                "set",
                new String[] {"arr", "keyF"},
                args -> {
                  JArray array = arr(args[0]);
                  JFunction keyF =
                      args.length > 1 && args[1] instanceof JFunction ? func(args[1]) : null;
                  List<Object> items = new ArrayList<>(array.size());
                  for (int i = 0; i < array.size(); i++) {
                    items.add(array.get(i));
                  }
                  items.sort(
                      (a, b) -> {
                        Object ka = keyF != null ? keyF.call(a) : a;
                        Object kb = keyF != null ? keyF.call(b) : b;
                        return compareValues(ka, kb);
                      });
                  // Dedup
                  List<Object> result = new ArrayList<>();
                  Object lastKey = null;
                  boolean first = true;
                  for (Object item : items) {
                    Object key = keyF != null ? keyF.call(item) : item;
                    if (first || !jsonnetEquals(key, lastKey)) {
                      result.add(item);
                      lastKey = key;
                      first = false;
                    }
                  }
                  return new JGenericArray(result);
                }));
  }

  private static void flattenDeep(Object val, List<Object> result) {
    if (val instanceof JArray) {
      JArray arr = (JArray) val;
      for (int i = 0; i < arr.size(); i++) {
        flattenDeep(arr.get(i), result);
      }
    } else {
      result.add(val);
    }
  }

  static int compareValues(Object a, Object b) {
    return compareValues(a, b, false);
  }

  static int compareValues(Object a, Object b, boolean isKeyComparison) {
    if (a instanceof Double && b instanceof Double) {
      return Double.compare((Double) a, (Double) b);
    }
    if (a instanceof String && b instanceof String) {
      return JsonnetStrings.compareByCodepoint((String) a, (String) b);
    }
    if (a instanceof JArray aa && b instanceof JArray bb) {
      int len = Math.min(aa.size(), bb.size());
      for (int i = 0; i < len; i++) {
        int cmp = compareValues(aa.get(i), bb.get(i), isKeyComparison);
        if (cmp != 0) return cmp;
      }
      return Integer.compare(aa.size(), bb.size());
    }
    if (a instanceof Boolean || b instanceof Boolean) {
      String prefix = isKeyComparison ? "key values" : "values";
      throw new JsonnetException("Cannot sort with " + prefix + " that are booleans");
    }
    if (a != null && b != null && !a.getClass().equals(b.getClass())) {
      throw new JsonnetException("Cannot sort with values that are not all the same type");
    }
    return 0;
  }

  static boolean jsonnetEquals(Object a, Object b) {
    return JsonnetEqualNode.deepEquals(a, b);
  }

  private static JGenericArray toCharArray(String s) {
    List<Object> chars = new ArrayList<>(s.codePointCount(0, s.length()));
    for (int i = 0; i < s.length(); ) {
      int cp = s.codePointAt(i);
      chars.add(new String(Character.toChars(cp)));
      i += Character.charCount(cp);
    }
    return new JGenericArray(chars);
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

  private static double num(Object arg) {
    if (arg instanceof Double) return (Double) arg;
    throw new JsonnetException(
        "Expected number, got: " + (arg == null ? "null" : arg.getClass().getSimpleName()));
  }

  private static double toNum(Object arg) {
    if (arg instanceof Double) return (Double) arg;
    if (arg instanceof Boolean) return (Boolean) arg ? 1 : 0;
    throw new JsonnetException("Cannot convert to number: " + arg);
  }

  private static JArray arr(Object arg) {
    if (arg instanceof JArray) return (JArray) arg;
    throw new JsonnetException(
        "Expected array, got: " + (arg == null ? "null" : arg.getClass().getSimpleName()));
  }

  private static JFunction func(Object arg) {
    if (arg instanceof JFunction) return (JFunction) arg;
    throw new JsonnetException(
        "Expected function, got: " + (arg == null ? "null" : arg.getClass().getSimpleName()));
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

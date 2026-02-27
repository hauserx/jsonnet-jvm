package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JNumberArray;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetJson;
import com.databricks.jsonnetjvm.runtime.Val;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** String functions for the std library. */
public class StdStringModule {

  public static void register(JObject std, JsonnetLanguage language) {
    // toString(a) -> string
    std.addField(
        "toString",
        () ->
            fn(
                language,
                "toString",
                new String[] {"a"},
                args -> {
                  Object a = args[0];
                  if (a instanceof String) return a;
                  if (a instanceof Double d) return JsonnetJson.formatNumber(d);
                  if (a instanceof Val) return ((Val) a).toJson();
                  return String.valueOf(a);
                }));

    // codepoint(str) -> number
    std.addField(
        "codepoint",
        () ->
            fn(
                language,
                "codepoint",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  int cpCount = s.codePointCount(0, s.length());
                  if (cpCount != 1) {
                    throw new JsonnetException(
                        "std.codepoint: expected a single character string, got \"" + s + "\"");
                  }
                  return (double) s.codePointAt(0);
                }));

    // char(n) -> string
    std.addField(
        "char",
        () ->
            fn(
                language,
                "char",
                new String[] {"n"},
                args -> {
                  int cp = (int) num(args[0]);
                  if (!Character.isValidCodePoint(cp)) {
                    throw new JsonnetException("std.char: invalid unicode code point, got " + cp);
                  }
                  return Character.toString(cp);
                }));

    // substr(str, from, len) -> string
    std.addField(
        "substr",
        () ->
            fn(
                language,
                "substr",
                new String[] {"str", "from", "len"},
                args -> {
                  String s = str(args[0]);
                  int offset = (int) num(args[1]);
                  int length = (int) num(args[2]);
                  int unicodeLength = s.codePointCount(0, s.length());
                  int safeOffset = Math.min(offset, unicodeLength);
                  int safeLength = Math.min(length, unicodeLength - safeOffset);
                  if (safeLength <= 0) return "";
                  int startUtf16 = safeOffset == 0 ? 0 : s.offsetByCodePoints(0, safeOffset);
                  int endUtf16 = s.offsetByCodePoints(startUtf16, safeLength);
                  return s.substring(startUtf16, endUtf16);
                }));

    // startsWith(a, b) -> boolean
    std.addField(
        "startsWith",
        () ->
            fn(
                language,
                "startsWith",
                new String[] {"a", "b"},
                args -> str(args[0]).startsWith(str(args[1]))));

    // endsWith(a, b) -> boolean
    std.addField(
        "endsWith",
        () ->
            fn(
                language,
                "endsWith",
                new String[] {"a", "b"},
                args -> str(args[0]).endsWith(str(args[1]))));

    // strReplace(str, from, to) -> string
    std.addField(
        "strReplace",
        () ->
            fn(
                language,
                "strReplace",
                new String[] {"str", "from", "to"},
                args -> {
                  String s = str(args[0]);
                  String from = str(args[1]);
                  String to = str(args[2]);
                  if (from.isEmpty()) {
                    throw new JsonnetException("std.strReplace: cannot replace empty string");
                  }
                  return s.replace(from, to);
                }));

    // asciiUpper(str) -> string
    std.addField(
        "asciiUpper",
        () -> fn(language, "asciiUpper", new String[] {"str"}, args -> str(args[0]).toUpperCase()));

    // asciiLower(str) -> string
    std.addField(
        "asciiLower",
        () -> fn(language, "asciiLower", new String[] {"str"}, args -> str(args[0]).toLowerCase()));

    // split(str, c) -> array of strings
    std.addField(
        "split",
        () ->
            fn(
                language,
                "split",
                new String[] {"str", "c"},
                args -> splitLimit(str(args[0]), str(args[1]), -1)));

    // splitLimit(str, c, maxsplits) -> array of strings
    std.addField(
        "splitLimit",
        () ->
            fn(
                language,
                "splitLimit",
                new String[] {"str", "c", "maxsplits"},
                args -> splitLimit(str(args[0]), str(args[1]), (int) num(args[2]))));

    // splitLimitR(str, c, maxsplits) -> array of strings (from right)
    std.addField(
        "splitLimitR",
        () ->
            fn(
                language,
                "splitLimitR",
                new String[] {"str", "c", "maxsplits"},
                args -> {
                  String s = str(args[0]);
                  String c = str(args[1]);
                  int maxSplits = (int) num(args[2]);
                  String revS = new StringBuilder(s).reverse().toString();
                  String revC = new StringBuilder(c).reverse().toString();
                  JArray revResult = splitLimit(revS, revC, maxSplits);
                  List<Object> result = new ArrayList<>(revResult.size());
                  for (int i = revResult.size() - 1; i >= 0; i--) {
                    String part = (String) revResult.get(i);
                    result.add(new StringBuilder(part).reverse().toString());
                  }
                  return new JGenericArray(result);
                }));

    // join(sep, arr) -> string or array
    std.addField(
        "join",
        () ->
            fn(
                language,
                "join",
                new String[] {"sep", "arr"},
                args -> {
                  Object sep = args[0];
                  JArray arr = arr(args[1]);
                  if (sep instanceof String) {
                    String sepStr = (String) sep;
                    StringBuilder sb = new StringBuilder();
                    boolean added = false;
                    for (int i = 0; i < arr.size(); i++) {
                      Object item = arr.get(i);
                      if (item instanceof JNull) continue;
                      if (!(item instanceof String)) {
                        throw new JsonnetException(
                            "Cannot join " + JsonnetException.typeName(item));
                      }
                      if (added) sb.append(sepStr);
                      added = true;
                      sb.append((String) item);
                    }
                    return sb.toString();
                  } else if (sep instanceof JArray) {
                    JArray sepArr = (JArray) sep;
                    List<Object> result = new ArrayList<>();
                    boolean added = false;
                    for (int i = 0; i < arr.size(); i++) {
                      Object item = arr.get(i);
                      if (item instanceof JNull) continue;
                      if (!(item instanceof JArray)) {
                        throw new JsonnetException(
                            "Cannot join " + JsonnetException.typeName(item));
                      }
                      if (added) {
                        for (int j = 0; j < sepArr.size(); j++) result.add(sepArr.get(j));
                      }
                      added = true;
                      JArray inner = (JArray) item;
                      for (int j = 0; j < inner.size(); j++) result.add(inner.get(j));
                    }
                    return new JGenericArray(result);
                  }
                  throw new JsonnetException("std.join: separator must be string or array");
                }));

    // stringChars(str) -> array of single-char strings
    std.addField(
        "stringChars",
        () ->
            fn(
                language,
                "stringChars",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  List<Object> chars = new ArrayList<>(s.codePointCount(0, s.length()));
                  int i = 0;
                  while (i < s.length()) {
                    int cp = s.codePointAt(i);
                    chars.add(Character.toString(cp));
                    i += Character.charCount(cp);
                  }
                  return new JGenericArray(chars);
                }));

    // parseInt(str) -> number
    std.addField(
        "parseInt",
        () ->
            fn(
                language,
                "parseInt",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  try {
                    return (double) Long.parseLong(s);
                  } catch (NumberFormatException e) {
                    throw new JsonnetException(
                        "std.parseInt: cannot parse '" + s + "' as an integer");
                  }
                }));

    // parseOctal(str) -> number
    std.addField(
        "parseOctal",
        () ->
            fn(
                language,
                "parseOctal",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  try {
                    return (double) Long.parseLong(s, 8);
                  } catch (NumberFormatException e) {
                    throw new JsonnetException("std.parseOctal: cannot parse '" + s + "' as octal");
                  }
                }));

    // parseHex(str) -> number
    std.addField(
        "parseHex",
        () ->
            fn(
                language,
                "parseHex",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  try {
                    return (double) Long.parseLong(s, 16);
                  } catch (NumberFormatException e) {
                    throw new JsonnetException("std.parseHex: cannot parse '" + s + "' as hex");
                  }
                }));

    // encodeUTF8(str) -> array of numbers
    std.addField(
        "encodeUTF8",
        () ->
            fn(
                language,
                "encodeUTF8",
                new String[] {"str"},
                args -> {
                  byte[] bytes = str(args[0]).getBytes(StandardCharsets.UTF_8);
                  double[] result = new double[bytes.length];
                  for (int i = 0; i < bytes.length; i++) {
                    result[i] = bytes[i] & 0xff;
                  }
                  return new JNumberArray(result);
                }));

    // decodeUTF8(arr) -> string
    std.addField(
        "decodeUTF8",
        () ->
            fn(
                language,
                "decodeUTF8",
                new String[] {"arr"},
                args -> {
                  JArray a = arr(args[0]);
                  byte[] bytes = new byte[a.size()];
                  for (int i = 0; i < a.size(); i++) {
                    Object item = a.get(i);
                    if (!(item instanceof Double)) {
                      throw new JsonnetException(
                          "std.decodeUTF8: element " + i + " is not a number");
                    }
                    double d = (Double) item;
                    if (d != Math.floor(d) || Double.isInfinite(d)) {
                      throw new JsonnetException(
                          "std.decodeUTF8: element " + i + " was not an integer in range [0, 255]");
                    }
                    int val_ = (int) d;
                    if (val_ < 0 || val_ > 255) {
                      throw new JsonnetException(
                          "std.decodeUTF8: element " + i + " was not an integer in range [0, 255]");
                    }
                    bytes[i] = (byte) val_;
                  }
                  return new String(bytes, StandardCharsets.UTF_8);
                }));

    // stripChars(str, chars) -> string
    std.addField(
        "stripChars",
        () ->
            fn(
                language,
                "stripChars",
                new String[] {"str", "chars"},
                args -> strip(str(args[0]), str(args[1]), true, true)));

    // lstripChars(str, chars) -> string
    std.addField(
        "lstripChars",
        () ->
            fn(
                language,
                "lstripChars",
                new String[] {"str", "chars"},
                args -> strip(str(args[0]), str(args[1]), true, false)));

    // rstripChars(str, chars) -> string
    std.addField(
        "rstripChars",
        () ->
            fn(
                language,
                "rstripChars",
                new String[] {"str", "chars"},
                args -> strip(str(args[0]), str(args[1]), false, true)));

    // findSubstr(pat, str) -> array of indices
    std.addField(
        "findSubstr",
        () ->
            fn(
                language,
                "findSubstr",
                new String[] {"pat", "str"},
                args -> {
                  String pat = str(args[0]);
                  String s = str(args[1]);
                  List<Object> indices = new ArrayList<>();
                  if (pat.isEmpty()) return new JGenericArray(indices);
                  int matchIndex = s.indexOf(pat);
                  int prevCharIndex = 0;
                  int prevCodePointIndex = 0;
                  while (matchIndex >= 0 && matchIndex < s.length()) {
                    int cpIndex = prevCodePointIndex + s.codePointCount(prevCharIndex, matchIndex);
                    indices.add((double) cpIndex);
                    prevCharIndex = matchIndex;
                    prevCodePointIndex = cpIndex;
                    matchIndex = s.indexOf(pat, matchIndex + 1);
                  }
                  return new JGenericArray(indices);
                }));

    // isEmpty(str) -> boolean
    std.addField(
        "isEmpty",
        () -> fn(language, "isEmpty", new String[] {"str"}, args -> str(args[0]).isEmpty()));

    // trim(str) -> string
    std.addField(
        "trim",
        () ->
            fn(
                language,
                "trim",
                new String[] {"str"},
                args -> strip(str(args[0]), " \t\n\f\r\u0085\u00A0", true, true)));

    // equalsIgnoreCase(str1, str2) -> boolean
    std.addField(
        "equalsIgnoreCase",
        () ->
            fn(
                language,
                "equalsIgnoreCase",
                new String[] {"str1", "str2"},
                args -> str(args[0]).equalsIgnoreCase(str(args[1]))));

    std.addField(
        "escapeStringJson",
        () ->
            fn(
                language,
                "escapeStringJson",
                new String[] {"str_"},
                args -> {
                  String s = str(args[0]);
                  return JsonnetJson.renderCompact(s);
                }));

    std.addField(
        "escapeStringPython",
        () ->
            fn(
                language,
                "escapeStringPython",
                new String[] {"str"},
                args -> {
                  String quoted = JsonnetJson.renderCompact(str(args[0]));
                  return quoted.substring(1, quoted.length() - 1);
                }));

    // escapeStringXML(str) -> string
    std.addField(
        "escapeStringXML",
        () ->
            fn(
                language,
                "escapeStringXML",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  StringBuilder sb = new StringBuilder(s.length());
                  for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    switch (c) {
                      case '<' -> sb.append("&lt;");
                      case '>' -> sb.append("&gt;");
                      case '&' -> sb.append("&amp;");
                      case '"' -> sb.append("&quot;");
                      case '\'' -> sb.append("&apos;");
                      default -> sb.append(c);
                    }
                  }
                  return sb.toString();
                }));

    // escapeStringBash(str) -> string
    std.addField(
        "escapeStringBash",
        () ->
            fn(
                language,
                "escapeStringBash",
                new String[] {"str_"},
                args -> "'" + str(args[0]).replace("'", "'\"'\"'") + "'"));

    // escapeStringDollars(str) -> string
    std.addField(
        "escapeStringDollars",
        () ->
            fn(
                language,
                "escapeStringDollars",
                new String[] {"str_"},
                args -> str(args[0]).replace("$", "$$")));

    // format(str, vals) -> string (also callable via the % operator on strings)
    std.addField(
        "format",
        () ->
            fn(
                language,
                "format",
                new String[] {"str", "vals"},
                args -> JsonnetFormat.apply(str(args[0]), args[1])));
  }

  private static JArray splitLimit(String s, String c, int maxSplits) {
    if (c.isEmpty()) {
      throw new JsonnetException("std.split: cannot split by an empty string");
    }
    if (maxSplits < -1) {
      throw new JsonnetException(
          "std.splitLimit: maxSplits should be -1 or non-negative, got " + maxSplits);
    }
    List<Object> parts = new ArrayList<>();
    int start = 0;
    int sz = 0;
    int i = 0;
    while (i <= s.length() - c.length() && (maxSplits < 0 || sz < maxSplits)) {
      if (s.startsWith(c, i)) {
        parts.add(s.substring(start, i));
        start = i + c.length();
        sz++;
        i += c.length();
      } else {
        i++;
      }
    }
    parts.add(s.substring(start));
    return new JGenericArray(parts);
  }

  private static String strip(String s, String chars, boolean left, boolean right) {
    if (s.isEmpty()) return s;
    Set<Integer> charsSet = codePointsSet(chars);
    int start = 0;
    int end = s.length();
    while (left && start < end && charsSet.contains(s.codePointAt(start))) {
      start = s.offsetByCodePoints(start, 1);
    }
    while (right && end > start && charsSet.contains(s.codePointBefore(end))) {
      end = s.offsetByCodePoints(end, -1);
    }
    return s.substring(start, end);
  }

  private static Set<Integer> codePointsSet(String s) {
    Set<Integer> result = new HashSet<>();
    int i = 0;
    while (i < s.length()) {
      int cp = s.codePointAt(i);
      result.add(cp);
      i += Character.charCount(cp);
    }
    return result;
  }

  private static String str(Object arg) {
    if (arg instanceof String) return (String) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected String, got " + JsonnetException.typeName(arg));
  }

  private static double num(Object arg) {
    if (arg instanceof Double) return (Double) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected Number, got " + JsonnetException.typeName(arg));
  }

  private static JArray arr(Object arg) {
    if (arg instanceof JArray) return (JArray) arg;
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

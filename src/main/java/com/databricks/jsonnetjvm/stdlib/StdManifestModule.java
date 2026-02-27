package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.runtime.JsonnetJson;
import com.databricks.jsonnetjvm.runtime.JsonnetStrings;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.yaml.snakeyaml.Yaml;

/** Manifest/serialization functions for the std library. */
public class StdManifestModule {

  public static void register(JObject std, JsonnetLanguage language) {
    std.addField(
        "manifestJson",
        () ->
            fn(
                language,
                "manifestJson",
                new String[] {"v"},
                args -> JsonnetJson.render(args[0], "   ", "\n", ": ")));

    std.addField(
        "manifestJsonEx",
        () ->
            fn(
                language,
                "manifestJsonEx",
                new String[] {"value", "indent", "newline", "key_val_sep"},
                args -> {
                  Object val = args[0];
                  String indent = str(args[1]);
                  String newline =
                      args.length > 2 && args[2] instanceof String ? (String) args[2] : "\n";
                  String kvSep =
                      args.length > 3 && args[3] instanceof String ? (String) args[3] : ": ";
                  return JsonnetJson.render(val, indent, newline, kvSep);
                }));

    std.addField(
        "manifestJsonMinified",
        () ->
            fn(
                language,
                "manifestJsonMinified",
                new String[] {"value"},
                args -> JsonnetJson.render(args[0], "", "", ":")));

    std.addField(
        "parseJson",
        () ->
            fn(
                language,
                "parseJson",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  return JsonnetJson.parseJson(s);
                }));

    std.addField(
        "lines",
        () ->
            fn(
                language,
                "lines",
                new String[] {"arr"},
                args -> {
                  JArray arr = jarray(args[0]);
                  StringBuilder sb = new StringBuilder();
                  for (int i = 0; i < arr.size(); i++) {
                    Object item = arr.get(i);
                    if (item instanceof JNull) continue;
                    if (!(item instanceof String)) {
                      throw new JsonnetException(
                          "std.lines: expected string, got " + typeName(item));
                    }
                    sb.append((String) item).append("\n");
                  }
                  return sb.toString();
                }));

    std.addField(
        "deepJoin",
        () -> fn(language, "deepJoin", new String[] {"arr"}, args -> deepJoin(args[0])));

    std.addField(
        "parseYaml",
        () ->
            fn(
                language,
                "parseYaml",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  if (s.trim().isEmpty()) return JNull.INSTANCE;
                  Yaml yaml = new Yaml();
                  List<Object> docs = new ArrayList<>();
                  for (Object doc : yaml.loadAll(s)) {
                    docs.add(fromJavaObject(doc));
                  }
                  if (docs.size() == 1) return docs.get(0);
                  return new JGenericArray(docs);
                }));

    std.addField(
        "manifestYamlDoc",
        () ->
            fn(
                language,
                "manifestYamlDoc",
                new String[] {"value", "indent_array_in_object", "quote_keys"},
                args -> {
                  Object val = args[0];
                  boolean indentArrayInObject =
                      args.length > 1 && args[1] instanceof Boolean && (Boolean) args[1];
                  boolean quoteKeys =
                      !(args.length > 2 && args[2] instanceof Boolean && !(Boolean) args[2]);
                  return renderYaml(val, indentArrayInObject, quoteKeys);
                }));

    std.addField(
        "manifestYamlStream",
        () ->
            fn(
                language,
                "manifestYamlStream",
                new String[] {"value", "indent_array_in_object", "c_document_end", "quote_keys"},
                args -> {
                  if (!(args[0] instanceof JArray)) {
                    throw new JsonnetException("std.manifestYamlStream: expected array");
                  }
                  JArray arr = (JArray) args[0];
                  boolean indentArrayInObject =
                      args.length > 1 && args[1] instanceof Boolean && (Boolean) args[1];
                  boolean cDocumentEnd =
                      !(args.length > 2 && args[2] instanceof Boolean && !(Boolean) args[2]);
                  boolean quoteKeys =
                      !(args.length > 3 && args[3] instanceof Boolean && !(Boolean) args[3]);
                  StringBuilder sb = new StringBuilder();
                  for (int i = 0; i < arr.size(); i++) {
                    sb.append("---\n");
                    sb.append(renderYaml(arr.get(i), indentArrayInObject, quoteKeys));
                    sb.append("\n");
                  }
                  if (cDocumentEnd && arr.size() > 0) sb.append("...\n");
                  return sb.toString();
                }));

    std.addField(
        "manifestIni",
        () ->
            fn(
                language,
                "manifestIni",
                new String[] {"ini"},
                args -> {
                  if (!(args[0] instanceof JObject))
                    throw new JsonnetException("std.manifestIni: expected object");
                  JObject ini = (JObject) args[0];
                  StringBuilder sb = new StringBuilder();
                  if (ini.hasField("main")) {
                    Object mainObj = ini.getField("main");
                    if (!(mainObj instanceof JObject))
                      throw new JsonnetException("std.manifestIni: 'main' must be an object");
                    renderIniSection(sb, (JObject) mainObj);
                  }
                  if (ini.hasField("sections")) {
                    Object sectionsObj = ini.getField("sections");
                    if (!(sectionsObj instanceof JObject))
                      throw new JsonnetException("std.manifestIni: 'sections' must be an object");
                    JObject sections = (JObject) sectionsObj;
                    List<String> sectionNames = new ArrayList<>(sections.getFieldNames());
                    sectionNames.sort(JsonnetStrings::compareByCodepoint);
                    for (String name : sectionNames) {
                      sb.append("[").append(name).append("]\n");
                      Object sectionVal = sections.getField(name);
                      if (!(sectionVal instanceof JObject))
                        throw new JsonnetException(
                            "std.manifestIni: section '" + name + "' must be an object");
                      renderIniSection(sb, (JObject) sectionVal);
                    }
                  }
                  return sb.toString();
                }));

    std.addField(
        "manifestPython",
        () ->
            fn(
                language,
                "manifestPython",
                new String[] {"v"},
                args -> {
                  return renderPython(args[0]);
                }));

    std.addField(
        "manifestPythonVars",
        () ->
            fn(
                language,
                "manifestPythonVars",
                new String[] {"conf"},
                args -> {
                  if (!(args[0] instanceof JObject))
                    throw new JsonnetException("std.manifestPythonVars: expected object");
                  JObject obj = (JObject) args[0];
                  List<String> keys = new ArrayList<>(obj.getFieldNames());
                  keys.sort(JsonnetStrings::compareByCodepoint);
                  StringBuilder sb = new StringBuilder();
                  for (String key : keys) {
                    sb.append(key)
                        .append(" = ")
                        .append(renderPython(obj.getField(key)))
                        .append("\n");
                  }
                  return sb.toString();
                }));

    std.addField(
        "manifestXmlJsonml",
        () ->
            fn(
                language,
                "manifestXmlJsonml",
                new String[] {"value"},
                args -> {
                  return renderXmlJsonml(args[0]);
                }));

    std.addField(
        "manifestTomlEx",
        () ->
            fn(
                language,
                "manifestTomlEx",
                new String[] {"value", "indent"},
                args -> {
                  if (!(args[0] instanceof JObject))
                    throw new JsonnetException("std.manifestTomlEx: expected object");
                  JObject obj = (JObject) args[0];
                  String indent = str(args[1]);
                  StringBuilder sb = new StringBuilder();
                  renderTomlTable(sb, obj, "", indent, new ArrayList<>());
                  String result = sb.toString().strip();
                  return result;
                }));
  }

  // --- INI Renderer ---

  private static void renderIniSection(StringBuilder sb, JObject section) {
    List<String> keys = new ArrayList<>(section.getFieldNames());
    keys.sort(JsonnetStrings::compareByCodepoint);
    for (String key : keys) {
      Object val = section.getField(key);
      if (val instanceof JArray arr) {
        for (int i = 0; i < arr.size(); i++) {
          sb.append(key).append(" = ").append(renderIniValue(arr.get(i))).append("\n");
        }
      } else {
        sb.append(key).append(" = ").append(renderIniValue(val)).append("\n");
      }
    }
  }

  private static String renderIniValue(Object val) {
    if (val instanceof String s) return s;
    if (val instanceof Double d) return JsonnetJson.formatNumber(d);
    if (val instanceof Boolean b) return b.toString();
    if (val instanceof JNull) return "null";
    return JsonnetJson.renderInline(val);
  }

  // --- Python Renderer ---

  private static String renderPython(Object val) {
    if (val instanceof JNull) return "None";
    if (val instanceof Boolean b) return b ? "True" : "False";
    if (val instanceof Double d) return JsonnetJson.formatNumber(d);
    if (val instanceof String s) return JsonnetJson.renderCompact(s);
    if (val instanceof JArray arr) {
      if (arr.size() == 0) return "[]";
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < arr.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(renderPython(arr.get(i)));
      }
      sb.append("]");
      return sb.toString();
    }
    if (val instanceof JObject obj) {
      List<String> keys = new ArrayList<>(obj.getFieldNames());
      keys.sort(JsonnetStrings::compareByCodepoint);
      StringBuilder sb = new StringBuilder("{");
      for (int i = 0; i < keys.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(JsonnetJson.renderCompact(keys.get(i))).append(": ");
        sb.append(renderPython(obj.getField(keys.get(i))));
      }
      sb.append("}");
      return sb.toString();
    }
    return "None";
  }

  // --- XML JsonML Renderer ---

  private static String renderXmlJsonml(Object val) {
    if (val instanceof String s) return escapeXml(s);
    if (!(val instanceof JArray))
      throw new JsonnetException("Cannot call manifestXmlJsonml on " + typeName(val));
    JArray arr = (JArray) val;
    if (arr.size() == 0) throw new JsonnetException("Cannot call manifestXmlJsonml on empty array");
    Object first = arr.get(0);
    if (!(first instanceof String))
      throw new JsonnetException("Cannot call manifestXmlJsonml: element tag must be a string");
    String tag = (String) first;
    StringBuilder sb = new StringBuilder();
    sb.append("<").append(tag);
    int childStart = 1;
    if (arr.size() > 1 && arr.get(1) instanceof JObject) {
      JObject attrs = (JObject) arr.get(1);
      List<String> attrNames = new ArrayList<>(attrs.getFieldNames());
      attrNames.sort(JsonnetStrings::compareByCodepoint);
      for (String name : attrNames) {
        Object attrVal = attrs.getField(name);
        String rendered;
        if (attrVal instanceof String s) rendered = s;
        else if (attrVal instanceof Double d) rendered = JsonnetJson.formatNumber(d);
        else throw new JsonnetException("Cannot call manifestXmlJsonml on " + typeName(attrVal));
        sb.append(" ").append(name).append("=\"").append(escapeXml(rendered)).append("\"");
      }
      childStart = 2;
    }
    sb.append(">");
    for (int i = childStart; i < arr.size(); i++) {
      sb.append(renderXmlJsonml(arr.get(i)));
    }
    sb.append("</").append(tag).append(">");
    return sb.toString();
  }

  private static String escapeXml(String s) {
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
  }

  // --- TOML Renderer ---

  private static boolean isTomlSection(Object val) {
    if (val instanceof JObject) return true;
    if (val instanceof JArray arr) {
      if (arr.size() == 0) return false;
      for (int i = 0; i < arr.size(); i++) {
        if (!(arr.get(i) instanceof JObject)) return false;
      }
      return true;
    }
    return false;
  }

  private static void renderTomlTable(
      StringBuilder sb, JObject obj, String cumulatedIndent, String indent, List<String> path) {
    List<String> keys = new ArrayList<>(obj.getFieldNames());
    keys.sort(JsonnetStrings::compareByCodepoint);
    List<String> nonSections = new ArrayList<>();
    List<String> sections = new ArrayList<>();
    for (String k : keys) {
      if (isTomlSection(obj.getField(k))) {
        sections.add(k);
      } else {
        nonSections.add(k);
      }
    }
    for (String k : nonSections) {
      sb.append(cumulatedIndent);
      sb.append(escapeTomlKey(k));
      sb.append(" = ");
      renderTomlValue(sb, obj.getField(k), cumulatedIndent, indent);
    }
    sb.append("\n");
    for (String k : sections) {
      Object val = obj.getField(k);
      List<String> newPath = new ArrayList<>(path);
      newPath.add(k);
      if (val instanceof JObject subObj) {
        sb.append(cumulatedIndent);
        sb.append("[").append(tomlPath(newPath)).append("]\n");
        renderTomlTable(sb, subObj, cumulatedIndent + indent, indent, newPath);
      } else if (val instanceof JArray arr) {
        for (int i = 0; i < arr.size(); i++) {
          Object item = arr.get(i);
          if (!(item instanceof JObject))
            throw new JsonnetException("std.manifestTomlEx: table array elements must be objects");
          sb.append(cumulatedIndent);
          sb.append("[[").append(tomlPath(newPath)).append("]]\n");
          renderTomlTable(sb, (JObject) item, cumulatedIndent + indent, indent, newPath);
        }
      }
    }
  }

  private static void renderTomlValue(
      StringBuilder sb, Object val, String cumulatedIndent, String indent) {
    if (val instanceof JNull) {
      throw new JsonnetException("Tried to manifest \"null\"");
    } else if (val instanceof Boolean b) {
      sb.append(b ? "true" : "false").append("\n");
    } else if (val instanceof Double d) {
      sb.append(JsonnetJson.formatNumber(d)).append("\n");
    } else if (val instanceof String s) {
      sb.append(JsonnetJson.renderCompact(s)).append("\n");
    } else if (val instanceof JArray arr) {
      if (arr.size() == 0) {
        sb.append("[]\n");
      } else {
        sb.append("[\n");
        for (int i = 0; i < arr.size(); i++) {
          sb.append(cumulatedIndent).append(indent);
          renderTomlInlineValue(sb, arr.get(i));
          if (i < arr.size() - 1) sb.append(",");
          sb.append("\n");
        }
        sb.append(cumulatedIndent).append("]\n");
      }
    } else if (val instanceof JObject obj) {
      renderTomlInlineObject(sb, obj);
      sb.append("\n");
    }
  }

  private static void renderTomlInlineValue(StringBuilder sb, Object val) {
    if (val instanceof JNull) throw new JsonnetException("Tried to manifest \"null\"");
    if (val instanceof Boolean b) {
      sb.append(b ? "true" : "false");
      return;
    }
    if (val instanceof Double d) {
      sb.append(JsonnetJson.formatNumber(d));
      return;
    }
    if (val instanceof String s) {
      sb.append(JsonnetJson.renderCompact(s));
      return;
    }
    if (val instanceof JArray arr) {
      if (arr.size() == 0) {
        sb.append("[]");
        return;
      }
      sb.append("[ ");
      for (int i = 0; i < arr.size(); i++) {
        if (i > 0) sb.append(", ");
        renderTomlInlineValue(sb, arr.get(i));
      }
      sb.append(" ]");
      return;
    }
    if (val instanceof JObject obj) {
      renderTomlInlineObject(sb, obj);
    }
  }

  private static void renderTomlInlineObject(StringBuilder sb, JObject obj) {
    List<String> keys = new ArrayList<>(obj.getFieldNames());
    keys.sort(JsonnetStrings::compareByCodepoint);
    sb.append("{ ");
    for (int i = 0; i < keys.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(escapeTomlKey(keys.get(i))).append(" = ");
      renderTomlInlineValue(sb, obj.getField(keys.get(i)));
    }
    sb.append(" }");
  }

  private static String escapeTomlKey(String key) {
    if (key.matches("[A-Za-z0-9_-]+")) return key;
    return JsonnetJson.renderCompact(key);
  }

  private static String tomlPath(List<String> path) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < path.size(); i++) {
      if (i > 0) sb.append(".");
      sb.append(escapeTomlKey(path.get(i)));
    }
    return sb.toString();
  }

  // --- YAML Renderer ---

  private static String renderYaml(Object val, boolean indentArrayInObject, boolean quoteKeys) {
    return new YamlRenderer(indentArrayInObject, quoteKeys).render(val);
  }

  /**
   * Custom YAML renderer matching sjsonnet's output format. Supports quote_keys and
   * indent_array_in_object parameters.
   *
   * <p>Key formatting rules (matching sjsonnet YamlRenderer): - Array item is scalar: "- value"
   * (same line) - Array item is object: "- key: val" (first key on same line) - Array item is
   * array: "-" alone, sub-array indented on next line - Object value is array: depends on
   * indent_array_in_object flag
   */
  static class YamlRenderer {
    private final boolean indentArrayInObject;
    private final boolean quoteKeys;
    private static final int INDENT = 2;

    YamlRenderer(boolean indentArrayInObject, boolean quoteKeys) {
      this.indentArrayInObject = indentArrayInObject;
      this.quoteKeys = quoteKeys;
    }

    String render(Object val) {
      StringBuilder sb = new StringBuilder();
      renderTopLevel(sb, val, 0);
      return sb.toString();
    }

    private void renderTopLevel(StringBuilder sb, Object val, int indent) {
      if (val instanceof JArray arr) {
        renderArrayItems(sb, arr, indent);
      } else if (val instanceof JObject obj) {
        renderObjectEntries(sb, obj, indent);
      } else {
        renderScalar(sb, val, indent);
      }
    }

    private void renderScalar(StringBuilder sb, Object val, int indent) {
      if (val instanceof JNull) {
        sb.append("null");
      } else if (val instanceof Boolean b) {
        sb.append(b ? "true" : "false");
      } else if (val instanceof Double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
          sb.append((long) d.doubleValue());
        } else {
          sb.append(d);
        }
      } else if (val instanceof String s) {
        if (s.endsWith("\n") && s.length() > 1) {
          sb.append("|");
          String[] lines = s.split("\n", -1);
          for (int i = 0; i < lines.length; i++) {
            if (i == lines.length - 1 && lines[i].isEmpty()) break;
            sb.append("\n").append(pad(indent + INDENT)).append(lines[i]);
          }
        } else {
          renderQuotedString(sb, s);
        }
      } else {
        sb.append("null");
      }
    }

    private void renderArrayItems(StringBuilder sb, JArray arr, int indent) {
      if (arr.size() == 0) {
        sb.append("[]");
        return;
      }
      for (int i = 0; i < arr.size(); i++) {
        if (i > 0) sb.append("\n").append(pad(indent));
        Object item = arr.get(i);
        if (item instanceof JArray subArr) {
          if (subArr.size() == 0) {
            sb.append("- []");
          } else {
            sb.append("-");
            sb.append("\n").append(pad(indent + INDENT));
            renderArrayItems(sb, subArr, indent + INDENT);
          }
        } else if (item instanceof JObject subObj) {
          if (subObj.getFieldNames().isEmpty()) {
            sb.append("- {}");
          } else {
            sb.append("- ");
            renderObjectEntries(sb, subObj, indent + INDENT);
          }
        } else {
          sb.append("- ");
          renderScalar(sb, item, indent);
        }
      }
    }

    private void renderObjectEntries(StringBuilder sb, JObject obj, int indent) {
      Set<String> fieldNames = obj.getFieldNames();
      if (fieldNames.isEmpty()) {
        sb.append("{}");
        return;
      }
      List<String> keys = new ArrayList<>(fieldNames);
      keys.sort(JsonnetStrings::compareByCodepoint);
      for (int i = 0; i < keys.size(); i++) {
        if (i > 0) sb.append("\n").append(pad(indent));
        String key = keys.get(i);
        renderKey(sb, key);
        sb.append(":");
        Object value = obj.getField(key);
        renderObjectValue(sb, value, indent);
      }
    }

    private void renderObjectValue(StringBuilder sb, Object value, int indent) {
      if (value instanceof JObject subObj) {
        if (subObj.getFieldNames().isEmpty()) {
          sb.append(" {}");
        } else {
          sb.append("\n").append(pad(indent + INDENT));
          renderObjectEntries(sb, subObj, indent + INDENT);
        }
      } else if (value instanceof JArray subArr) {
        if (subArr.size() == 0) {
          sb.append(" []");
        } else {
          int arrIndent = indentArrayInObject ? indent + INDENT : indent;
          sb.append("\n").append(pad(arrIndent));
          renderArrayItems(sb, subArr, arrIndent);
        }
      } else {
        sb.append(" ");
        renderScalar(sb, value, indent);
      }
    }

    private void renderKey(StringBuilder sb, String key) {
      if (quoteKeys || !isSafeBareKey(key)) {
        renderQuotedString(sb, key);
      } else {
        sb.append(key);
      }
    }

    private void renderQuotedString(StringBuilder sb, String s) {
      sb.append('"');
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        switch (c) {
          case '"' -> sb.append("\\\"");
          case '\\' -> sb.append("\\\\");
          case '\b' -> sb.append("\\b");
          case '\f' -> sb.append("\\f");
          case '\n' -> sb.append("\\n");
          case '\r' -> sb.append("\\r");
          case '\t' -> sb.append("\\t");
          default -> {
            if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
          }
        }
      }
      sb.append('"');
    }

    private static String pad(int n) {
      return n <= 0 ? "" : " ".repeat(n);
    }

    private static boolean isSafeBareKey(String key) {
      if (!key.matches("^[a-zA-Z0-9/._-]+$")) return false;
      String lower = key.toLowerCase();
      if (YAML_RESERVED.contains(lower)) return false;
      if (lower.matches("^(?:[0-9]*-){2}[0-9]*$")) return false;
      if (key.matches("^[-+]?0b[0-1_]+$")) return false;
      if (key.matches("^[-+]?0x[0-9a-fA-F_]+$")) return false;
      if (lower.matches("^-?([0-9_]*)*(\\.[0-9_]*)?(e[-+][0-9_]+)?$")) return false;
      if (lower.matches("^[-+]?[0-9_]+$")) return false;
      return true;
    }

    private static final Set<String> YAML_RESERVED =
        Set.of(
            "true", "false", "yes", "no", "on", "off", "y", "n", ".nan", "+.inf", "-.inf", ".inf",
            "null", "-", "---", "''", "~");
  }

  // --- deepJoin ---

  private static String deepJoin(Object val) {
    if (val instanceof String) return (String) val;
    if (val instanceof JArray) {
      JArray arr = (JArray) val;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < arr.size(); i++) sb.append(deepJoin(arr.get(i)));
      return sb.toString();
    }
    throw new JsonnetException("std.deepJoin: cannot join " + typeName(val));
  }

  // --- YAML -> Jsonnet conversion ---

  @SuppressWarnings("unchecked")
  static Object fromJavaObject(Object o) {
    if (o == null) return JNull.INSTANCE;
    if (o instanceof Boolean) return o;
    if (o instanceof Integer) return ((Integer) o).doubleValue();
    if (o instanceof Long) return ((Long) o).doubleValue();
    if (o instanceof Double) return o;
    if (o instanceof Float) return ((Float) o).doubleValue();
    if (o instanceof Number) return ((Number) o).doubleValue();
    if (o instanceof String) return o;
    if (o instanceof List) {
      List<?> list = (List<?>) o;
      List<Object> result = new ArrayList<>(list.size());
      for (Object item : list) result.add(fromJavaObject(item));
      return new JGenericArray(result);
    }
    if (o instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) o;
      JObject obj = new JObject();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = String.valueOf(entry.getKey());
        Object val2 = fromJavaObject(entry.getValue());
        obj.addField(key, () -> val2);
      }
      return obj;
    }
    return String.valueOf(o);
  }

  // --- Helpers ---

  private static String typeName(Object x) {
    return JsonnetException.typeName(x);
  }

  private static String str(Object arg) {
    if (arg instanceof String) return (String) arg;
    throw new JsonnetException(
        "Expected string, got: " + (arg == null ? "null" : arg.getClass().getSimpleName()));
  }

  private static JArray jarray(Object arg) {
    if (arg instanceof JArray) return (JArray) arg;
    throw new JsonnetException(
        "Expected array, got: " + (arg == null ? "null" : arg.getClass().getSimpleName()));
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

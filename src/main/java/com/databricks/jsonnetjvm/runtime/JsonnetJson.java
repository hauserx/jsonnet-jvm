package com.databricks.jsonnetjvm.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Centralized JSON handling using Jackson for both parsing and rendering of Jsonnet values. */
public class JsonnetJson {

  private static final JsonFactory JSON_FACTORY =
      JsonFactory.builder()
          .streamWriteConstraints(
              StreamWriteConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
          .build();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  // --- Parsing: JSON string -> Jsonnet values ---

  public static Object parseJson(String json) {
    try (JsonParser parser = JSON_FACTORY.createParser(json)) {
      parser.nextToken();
      Object result = parseValue(parser);
      if (parser.nextToken() != null) {
        throw new JsonnetException("std.parseJson: unexpected trailing input");
      }
      return result;
    } catch (JsonnetException e) {
      throw e;
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg != null && msg.contains("`StreamReadFeature")) {
        int idx = msg.indexOf("\n at [Source:");
        if (idx < 0) idx = msg.indexOf("\n");
        if (idx > 0) msg = msg.substring(0, idx);
      }
      throw new JsonnetException("std.parseJson: " + msg);
    }
  }

  private static Object parseValue(JsonParser parser) throws IOException {
    JsonToken token = parser.currentToken();
    if (token == null) {
      throw new JsonnetException("std.parseJson: unexpected end of input");
    }
    return switch (token) {
      case VALUE_NULL -> JNull.INSTANCE;
      case VALUE_TRUE -> true;
      case VALUE_FALSE -> false;
      case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> parser.getDoubleValue();
      case VALUE_STRING -> parser.getText();
      case START_OBJECT -> parseObject(parser);
      case START_ARRAY -> parseArray(parser);
      default -> throw new JsonnetException("std.parseJson: unexpected token " + token);
    };
  }

  private static JObject parseObject(JsonParser parser) throws IOException {
    JObject obj = new JObject();
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String key = parser.currentName();
      parser.nextToken();
      Object val = parseValue(parser);
      obj.addField(key, () -> val);
    }
    return obj;
  }

  private static JArray parseArray(JsonParser parser) throws IOException {
    List<Object> items = new ArrayList<>();
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      items.add(parseValue(parser));
    }
    return new JGenericArray(items);
  }

  // --- Rendering: Jsonnet values -> JSON string ---

  /**
   * Render a Jsonnet value as a JSON string with customizable formatting. This is the backend for
   * manifestJson, manifestJsonEx, manifestJsonMinified.
   */
  public static String render(Object val, String indent, String newline, String kvSep) {
    try {
      StringWriter sw = new StringWriter();
      JsonGenerator gen = JSON_FACTORY.createGenerator(sw);
      gen.setPrettyPrinter(new JsonnetPrettyPrinter(indent, newline, kvSep));
      writeValue(gen, val);
      gen.flush();
      gen.close();
      return sw.toString();
    } catch (IOException e) {
      throw new JsonnetException("JSON rendering failed: " + e.getMessage());
    }
  }

  /** Render compact JSON (no whitespace). Used for assertEqual messages, etc. */
  public static String renderCompact(Object val) {
    try {
      StringWriter sw = new StringWriter();
      JsonGenerator gen = JSON_FACTORY.createGenerator(sw);
      writeValue(gen, val);
      gen.flush();
      gen.close();
      return sw.toString();
    } catch (IOException e) {
      throw new JsonnetException("JSON rendering failed: " + e.getMessage());
    }
  }

  /**
   * Render single-line JSON with spaces. Produces the format used by std.toString and error
   * messages: {@code {"a": 1, "b": 2}} for objects, {@code [1, 2, 3]} for arrays, {@code { }} for
   * empty objects, {@code [ ]} for empty arrays.
   */
  public static String renderInline(Object val) {
    if (val instanceof JNull || val == null) return "null";
    if (val instanceof Boolean b) return b.toString();
    if (val instanceof Double d) return formatNumber(d);
    if (val instanceof String s) return renderCompact(s);
    if (val instanceof JArray arr) {
      if (arr.size() == 0) return "[ ]";
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < arr.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(renderInline(arr.get(i)));
      }
      sb.append("]");
      return sb.toString();
    }
    if (val instanceof JObject obj) {
      List<String> keys = new ArrayList<>(obj.getFieldNames());
      keys.sort(JsonnetStrings::compareByCodepoint);
      if (keys.isEmpty()) return "{ }";
      StringBuilder sb = new StringBuilder("{");
      for (int i = 0; i < keys.size(); i++) {
        if (i > 0) sb.append(", ");
        String key = keys.get(i);
        sb.append(renderCompact(key)).append(": ").append(renderInline(obj.getField(key)));
      }
      sb.append("}");
      return sb.toString();
    }
    if (val instanceof JFunction fn) {
      String params = fn.getParamNames() != null ? String.join(", ", fn.getParamNames()) : "";
      throw new JsonnetException("Couldn't manifest function with params [" + params + "]");
    }
    return "null";
  }

  /** Write a Jsonnet runtime value to a Jackson JsonGenerator. */
  public static void writeValue(JsonGenerator gen, Object val) throws IOException {
    if (val instanceof JNull || val == null) {
      gen.writeNull();
    } else if (val instanceof Boolean b) {
      gen.writeBoolean(b);
    } else if (val instanceof Double d) {
      if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
        gen.writeNumber((long) d.doubleValue());
      } else {
        gen.writeNumber(d);
      }
    } else if (val instanceof String s) {
      gen.writeString(s);
    } else if (val instanceof JArray arr) {
      gen.writeStartArray();
      for (int i = 0; i < arr.size(); i++) {
        writeValue(gen, arr.get(i));
      }
      gen.writeEndArray();
    } else if (val instanceof JObject obj) {
      gen.writeStartObject();
      List<String> keys = new ArrayList<>(obj.getFieldNames());
      keys.sort(JsonnetStrings::compareByCodepoint);
      for (String key : keys) {
        gen.writeFieldName(key);
        writeValue(gen, obj.getField(key));
      }
      gen.writeEndObject();
    } else if (val instanceof JFunction fn) {
      String params = fn.getParamNames() != null ? String.join(", ", fn.getParamNames()) : "";
      throw new JsonnetException("Couldn't manifest function with params [" + params + "]");
    } else {
      gen.writeNull();
    }
  }

  /**
   * Convert a Jsonnet runtime value to a Jackson JsonNode tree. Useful for interop with libraries
   * that work with Jackson's tree model.
   */
  public static JsonNode toJsonNode(Object val) {
    if (val instanceof JNull || val == null) {
      return NullNode.getInstance();
    } else if (val instanceof Boolean b) {
      return BooleanNode.valueOf(b);
    } else if (val instanceof Double d) {
      if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
        return new LongNode((long) d.doubleValue());
      } else {
        return new DoubleNode(d);
      }
    } else if (val instanceof String s) {
      return new TextNode(s);
    } else if (val instanceof JArray arr) {
      ArrayNode arrayNode = MAPPER.createArrayNode();
      for (int i = 0; i < arr.size(); i++) {
        arrayNode.add(toJsonNode(arr.get(i)));
      }
      return arrayNode;
    } else if (val instanceof JObject obj) {
      ObjectNode objectNode = MAPPER.createObjectNode();
      List<String> keys = new ArrayList<>(obj.getFieldNames());
      keys.sort(JsonnetStrings::compareByCodepoint);
      for (String key : keys) {
        objectNode.set(key, toJsonNode(obj.getField(key)));
      }
      return objectNode;
    }
    return NullNode.getInstance();
  }

  /**
   * Convert a Jackson JsonNode tree to Jsonnet runtime values. Useful for interop with libraries
   * that produce Jackson trees.
   */
  public static Object fromJsonNode(JsonNode node) {
    if (node == null || node.isNull()) {
      return JNull.INSTANCE;
    } else if (node.isBoolean()) {
      return node.booleanValue();
    } else if (node.isNumber()) {
      return node.doubleValue();
    } else if (node.isTextual()) {
      return node.textValue();
    } else if (node.isArray()) {
      List<Object> items = new ArrayList<>(node.size());
      for (JsonNode child : node) {
        items.add(fromJsonNode(child));
      }
      return new JGenericArray(items);
    } else if (node.isObject()) {
      JObject obj = new JObject();
      node.fields()
          .forEachRemaining(
              entry -> {
                String key = entry.getKey();
                Object val2 = fromJsonNode(entry.getValue());
                obj.addField(key, () -> val2);
              });
      return obj;
    }
    return JNull.INSTANCE;
  }

  /**
   * Custom PrettyPrinter that supports Jsonnet's configurable indent string, newline, and key-value
   * separator.
   */
  private static class JsonnetPrettyPrinter extends DefaultPrettyPrinter {
    private final String indent;
    private final String newline;
    private final String kvSep;
    private int depth = 0;

    JsonnetPrettyPrinter(String indent, String newline, String kvSep) {
      this.indent = indent;
      this.newline = newline;
      this.kvSep = kvSep;
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
      return new JsonnetPrettyPrinter(indent, newline, kvSep);
    }

    @Override
    public void writeRootValueSeparator(JsonGenerator g) throws IOException {}

    @Override
    public void writeStartObject(JsonGenerator g) throws IOException {
      g.writeRaw("{");
      depth++;
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
      depth--;
      if (nrOfEntries == 0 && !isCompact()) {
        g.writeRaw(newline);
        g.writeRaw(newline);
        g.writeRaw(indentStr());
      } else if (nrOfEntries > 0) {
        g.writeRaw(newline);
        g.writeRaw(indentStr());
      }
      g.writeRaw("}");
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
      g.writeRaw(",");
      g.writeRaw(newline);
      g.writeRaw(indentStr());
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
      g.writeRaw(kvSep);
    }

    @Override
    public void beforeObjectEntries(JsonGenerator g) throws IOException {
      g.writeRaw(newline);
      g.writeRaw(indentStr());
    }

    @Override
    public void writeStartArray(JsonGenerator g) throws IOException {
      g.writeRaw("[");
      depth++;
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
      depth--;
      if (nrOfValues == 0 && !isCompact()) {
        g.writeRaw(newline);
        g.writeRaw(newline);
        g.writeRaw(indentStr());
      } else if (nrOfValues > 0) {
        g.writeRaw(newline);
        g.writeRaw(indentStr());
      }
      g.writeRaw("]");
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator g) throws IOException {
      g.writeRaw(",");
      g.writeRaw(newline);
      g.writeRaw(indentStr());
    }

    @Override
    public void beforeArrayValues(JsonGenerator g) throws IOException {
      g.writeRaw(newline);
      g.writeRaw(indentStr());
    }

    private boolean isCompact() {
      return indent.isEmpty() && newline.isEmpty();
    }

    private String indentStr() {
      if (indent.isEmpty()) return "";
      return indent.repeat(depth);
    }
  }

  /** Format a number for Jsonnet JSON output. */
  public static String formatNumber(double d) {
    if (d == Math.floor(d) && !Double.isInfinite(d)) {
      if (Math.abs(d) < 1e18) {
        return Long.toString((long) d);
      }
      return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }
    return Double.toString(d);
  }
}

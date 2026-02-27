package com.databricks.jsonnetjvm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for JsonnetJson: number formatting, JSON parsing, and rendering. */
public class JsonnetJsonTest {

  @Nested
  class FormatNumber {
    @Test
    void integers() {
      assertEquals("0", JsonnetJson.formatNumber(0.0));
      assertEquals("1", JsonnetJson.formatNumber(1.0));
      assertEquals("-1", JsonnetJson.formatNumber(-1.0));
      assertEquals("42", JsonnetJson.formatNumber(42.0));
    }

    @Test
    void largeIntegers() {
      assertEquals("1000000", JsonnetJson.formatNumber(1e6));
      assertEquals("1000000000000", JsonnetJson.formatNumber(1e12));
      assertEquals("100000000000000", JsonnetJson.formatNumber(1e14));
    }

    @Test
    void veryLargeIntegers() {
      String result = JsonnetJson.formatNumber(1e18);
      assertFalse(result.contains("E"), "Should not use scientific notation: " + result);
    }

    @Test
    void decimals() {
      assertEquals("3.14", JsonnetJson.formatNumber(3.14));
      assertEquals("0.5", JsonnetJson.formatNumber(0.5));
      assertEquals("-2.5", JsonnetJson.formatNumber(-2.5));
    }

    @Test
    void specialValues() {
      assertEquals("Infinity", JsonnetJson.formatNumber(Double.POSITIVE_INFINITY));
      assertEquals("-Infinity", JsonnetJson.formatNumber(Double.NEGATIVE_INFINITY));
      assertEquals("NaN", JsonnetJson.formatNumber(Double.NaN));
    }
  }

  @Nested
  class ParseJson {
    @Test
    void primitives() {
      assertEquals(JNull.INSTANCE, JsonnetJson.parseJson("null"));
      assertEquals(true, JsonnetJson.parseJson("true"));
      assertEquals(false, JsonnetJson.parseJson("false"));
      assertEquals(42.0, JsonnetJson.parseJson("42"));
      assertEquals(3.14, JsonnetJson.parseJson("3.14"));
      assertEquals("hello", JsonnetJson.parseJson("\"hello\""));
    }

    @Test
    void emptyStructures() {
      Object arr = JsonnetJson.parseJson("[]");
      assertInstanceOf(JArray.class, arr);
      assertEquals(0, ((JArray) arr).size());

      Object obj = JsonnetJson.parseJson("{}");
      assertInstanceOf(JObject.class, obj);
      assertTrue(((JObject) obj).getFieldNames().isEmpty());
    }

    @Test
    void nestedStructures() {
      Object result = JsonnetJson.parseJson("{\"a\": [1, 2], \"b\": {\"c\": true}}");
      assertInstanceOf(JObject.class, result);
      JObject obj = (JObject) result;
      assertInstanceOf(JArray.class, obj.getField("a"));
      assertInstanceOf(JObject.class, obj.getField("b"));
    }

    @Test
    void invalidJson() {
      assertThrows(JsonnetException.class, () -> JsonnetJson.parseJson("{"));
      assertThrows(JsonnetException.class, () -> JsonnetJson.parseJson(""));
      assertThrows(JsonnetException.class, () -> JsonnetJson.parseJson("[1,]"));
    }

    @Test
    void trailingInput() {
      assertThrows(JsonnetException.class, () -> JsonnetJson.parseJson("1 2"));
    }
  }

  @Nested
  class RenderCompact {
    @Test
    void primitives() {
      assertEquals("null", JsonnetJson.renderCompact(JNull.INSTANCE));
      assertEquals("true", JsonnetJson.renderCompact(true));
      assertEquals("false", JsonnetJson.renderCompact(false));
      assertEquals("42", JsonnetJson.renderCompact(42.0));
      assertEquals("\"hello\"", JsonnetJson.renderCompact("hello"));
    }

    @Test
    void escapedStrings() {
      assertEquals("\"a\\\"b\"", JsonnetJson.renderCompact("a\"b"));
      assertEquals("\"a\\nb\"", JsonnetJson.renderCompact("a\nb"));
      assertEquals("\"a\\\\b\"", JsonnetJson.renderCompact("a\\b"));
    }
  }

  @Nested
  class RenderInline {
    @Test
    void emptyArray() {
      JArray arr = new JGenericArray(List.of());
      assertEquals("[ ]", JsonnetJson.renderInline(arr));
    }

    @Test
    void emptyObject() {
      JObject obj = new JObject();
      assertEquals("{ }", JsonnetJson.renderInline(obj));
    }

    @Test
    void simpleArray() {
      JArray arr = new JNumberArray(new double[] {1.0, 2.0, 3.0});
      assertEquals("[1, 2, 3]", JsonnetJson.renderInline(arr));
    }

    @Test
    void simpleObject() {
      JObject obj = new JObject();
      obj.addField("a", () -> 1.0);
      obj.addField("b", () -> "hello");
      assertEquals("{\"a\": 1, \"b\": \"hello\"}", JsonnetJson.renderInline(obj));
    }

    @Test
    void objectFieldsSortedByCodepoint() {
      JObject obj = new JObject();
      obj.addField("z", () -> 1.0);
      obj.addField("a", () -> 2.0);
      String result = JsonnetJson.renderInline(obj);
      assertTrue(result.indexOf("\"a\"") < result.indexOf("\"z\""));
    }

    @Test
    void nullValue() {
      assertEquals("null", JsonnetJson.renderInline(JNull.INSTANCE));
      assertEquals("null", JsonnetJson.renderInline(null));
    }
  }

  @Nested
  class Render {
    @Test
    void customIndent() {
      JObject obj = new JObject();
      obj.addField("a", () -> 1.0);
      String result = JsonnetJson.render(obj, "\t", "\n", ": ");
      assertTrue(result.contains("\t\"a\""), "Should use tab indent: " + result);
    }

    @Test
    void minified() {
      JObject obj = new JObject();
      obj.addField("a", () -> 1.0);
      String result = JsonnetJson.render(obj, "", "", ":");
      assertEquals("{\"a\":1}", result);
    }
  }

  @Nested
  class RoundTrip {
    @Test
    void parseAndRenderPreservesValues() {
      String json = "{\"a\":1,\"b\":\"hello\",\"c\":[true,null,3.14]}";
      Object parsed = JsonnetJson.parseJson(json);
      String rendered = JsonnetJson.renderCompact(parsed);
      Object reparsed = JsonnetJson.parseJson(rendered);

      JObject obj1 = (JObject) parsed;
      JObject obj2 = (JObject) reparsed;
      assertEquals(obj1.getField("a"), obj2.getField("a"));
      assertEquals(obj1.getField("b"), obj2.getField("b"));
    }
  }
}

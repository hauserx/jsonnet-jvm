package com.databricks.jsonnetjvm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for JObject: field management, visibility, merging, layers. */
public class JObjectTest {

  @Nested
  class BasicFields {
    @Test
    void addAndGetField() {
      JObject obj = new JObject();
      obj.addField("x", () -> 42.0);
      assertEquals(42.0, obj.getField("x"));
    }

    @Test
    void missingFieldReturnsNull() {
      JObject obj = new JObject();
      assertNull(obj.getField("missing"));
    }

    @Test
    void multipleFields() {
      JObject obj = new JObject();
      obj.addField("a", () -> 1.0);
      obj.addField("b", () -> "hello");
      obj.addField("c", () -> true);
      assertEquals(1.0, obj.getField("a"));
      assertEquals("hello", obj.getField("b"));
      assertEquals(true, obj.getField("c"));
    }

    @Test
    void getFieldNames() {
      JObject obj = new JObject();
      obj.addField("x", () -> 1.0);
      obj.addField("y", () -> 2.0);
      Set<String> names = obj.getFieldNames();
      assertEquals(Set.of("x", "y"), names);
    }
  }

  @Nested
  class FieldVisibility {
    @Test
    void hiddenFieldNotInGetFieldNames() {
      JObject obj = new JObject();
      obj.addField("visible", () -> 1.0, JObject.Visibility.NORMAL);
      obj.addField("hidden", () -> 2.0, JObject.Visibility.HIDDEN);
      Set<String> names = obj.getFieldNames();
      assertTrue(names.contains("visible"));
      assertFalse(names.contains("hidden"));
    }

    @Test
    void hiddenFieldStillAccessible() {
      JObject obj = new JObject();
      obj.addField("hidden", () -> 42.0, JObject.Visibility.HIDDEN);
      assertEquals(42.0, obj.getField("hidden"));
    }

    @Test
    void forcedFieldAlwaysVisible() {
      JObject obj = new JObject();
      obj.addField("forced", () -> 1.0, JObject.Visibility.FORCED);
      assertTrue(obj.getFieldNames().contains("forced"));
    }

    @Test
    void hasFieldRespectsVisibility() {
      JObject obj = new JObject();
      obj.addField("visible", () -> 1.0, JObject.Visibility.NORMAL);
      obj.addField("hidden", () -> 2.0, JObject.Visibility.HIDDEN);
      assertTrue(obj.hasField("visible"));
      assertFalse(obj.hasField("hidden"));
    }

    @Test
    void hasFieldAllIgnoresVisibility() {
      JObject obj = new JObject();
      obj.addField("hidden", () -> 2.0, JObject.Visibility.HIDDEN);
      assertTrue(obj.hasFieldAll("hidden"));
    }

    @Test
    void setAllFieldsHidden() {
      JObject obj = new JObject();
      obj.addField("a", () -> 1.0);
      obj.addField("b", () -> 2.0);
      obj.setAllFieldsHidden();
      assertTrue(obj.getFieldNames().isEmpty());
      assertTrue(obj.hasFieldAll("a"));
      assertTrue(obj.hasFieldAll("b"));
    }

    @Test
    void getAllFieldNamesIncludesHidden() {
      JObject obj = new JObject();
      obj.addField("visible", () -> 1.0, JObject.Visibility.NORMAL);
      obj.addField("hidden", () -> 2.0, JObject.Visibility.HIDDEN);
      Set<String> all = obj.getAllFieldNames();
      assertTrue(all.contains("visible"));
      assertTrue(all.contains("hidden"));
    }
  }

  @Nested
  class Merge {
    @Test
    void mergeFieldsFromBothObjects() {
      JObject left = new JObject();
      left.addField("a", () -> 1.0);
      JObject right = new JObject();
      right.addField("b", () -> 2.0);
      JObject merged = JObject.merge(left, right);
      assertEquals(1.0, merged.getField("a"));
      assertEquals(2.0, merged.getField("b"));
    }

    @Test
    void mergeRightOverridesLeft() {
      JObject left = new JObject();
      left.addField("x", () -> "old");
      JObject right = new JObject();
      right.addField("x", () -> "new");
      JObject merged = JObject.merge(left, right);
      assertEquals("new", merged.getField("x"));
    }
  }

  @Nested
  class RemoveKey {
    @Test
    void removedFieldNotVisible() {
      JObject obj = new JObject();
      obj.addField("keep", () -> 1.0);
      obj.addField("drop", () -> 2.0);
      JObject removed = obj.removeKey("drop");
      assertTrue(removed.hasFieldAll("keep"));
      assertFalse(removed.hasFieldAll("drop"));
      assertNull(removed.getField("drop"));
    }

    @Test
    void removeDoesNotMutateOriginal() {
      JObject obj = new JObject();
      obj.addField("a", () -> 1.0);
      JObject removed = obj.removeKey("a");
      assertTrue(obj.hasFieldAll("a"));
      assertFalse(removed.hasFieldAll("a"));
    }
  }

  @Nested
  class Locals {
    @Test
    void addAndGetLocal() {
      JObject obj = new JObject();
      obj.addLocal("helper", () -> 99.0);
      assertTrue(obj.hasLocal("helper"));
      assertEquals(99.0, obj.getLocal("helper"));
    }

    @Test
    void missingLocal() {
      JObject obj = new JObject();
      assertFalse(obj.hasLocal("missing"));
      assertNull(obj.getLocal("missing"));
    }
  }
}

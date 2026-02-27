package com.databricks.jsonnetjvm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

/** Unit tests for ArrayBuilder: type promotion and specialized array creation. */
public class ArrayBuilderTest {

  @Test
  void emptyBuild() {
    ArrayBuilder b = new ArrayBuilder(4);
    JArray arr = b.build();
    assertInstanceOf(JGenericArray.class, arr);
    assertEquals(0, arr.size());
  }

  @Test
  void homogeneousDoubles() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add(1.0);
    b.add(2.0);
    b.add(3.0);
    JArray arr = b.build();
    assertInstanceOf(JNumberArray.class, arr);
    assertEquals(3, arr.size());
    assertEquals(1.0, arr.get(0));
    assertEquals(3.0, arr.get(2));
  }

  @Test
  void homogeneousBooleans() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add(true);
    b.add(false);
    b.add(true);
    JArray arr = b.build();
    assertInstanceOf(JBooleanArray.class, arr);
    assertEquals(3, arr.size());
    assertEquals(true, arr.get(0));
    assertEquals(false, arr.get(1));
  }

  @Test
  void homogeneousStrings() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add("a");
    b.add("b");
    JArray arr = b.build();
    assertInstanceOf(JStringArray.class, arr);
    assertEquals(2, arr.size());
    assertEquals("a", arr.get(0));
  }

  @Test
  void mixedDoublesAndStringsPromotesToGeneric() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add(1.0);
    b.add("hello");
    JArray arr = b.build();
    assertInstanceOf(JGenericArray.class, arr);
    assertEquals(2, arr.size());
    assertEquals(1.0, arr.get(0));
    assertEquals("hello", arr.get(1));
  }

  @Test
  void mixedBooleansAndDoublesPromotesToGeneric() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add(true);
    b.add(42.0);
    JArray arr = b.build();
    assertInstanceOf(JGenericArray.class, arr);
    assertEquals(2, arr.size());
    assertEquals(true, arr.get(0));
    assertEquals(42.0, arr.get(1));
  }

  @Test
  void nullPromotesToGeneric() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add(JNull.INSTANCE);
    JArray arr = b.build();
    assertInstanceOf(JGenericArray.class, arr);
    assertEquals(1, arr.size());
  }

  @Test
  void objectPromotesToGeneric() {
    ArrayBuilder b = new ArrayBuilder(4);
    b.add(1.0);
    b.add(new JObject());
    JArray arr = b.build();
    assertInstanceOf(JGenericArray.class, arr);
    assertEquals(2, arr.size());
  }

  @Test
  void growsBeyondInitialCapacity() {
    ArrayBuilder b = new ArrayBuilder(2);
    for (int i = 0; i < 100; i++) {
      b.add((double) i);
    }
    JArray arr = b.build();
    assertEquals(100, arr.size());
    assertEquals(0.0, arr.get(0));
    assertEquals(99.0, arr.get(99));
  }

  @Test
  void stringGrowsBeyondCapacity() {
    ArrayBuilder b = new ArrayBuilder(2);
    for (int i = 0; i < 50; i++) {
      b.add("s" + i);
    }
    JArray arr = b.build();
    assertInstanceOf(JStringArray.class, arr);
    assertEquals(50, arr.size());
    assertEquals("s0", arr.get(0));
    assertEquals("s49", arr.get(49));
  }
}

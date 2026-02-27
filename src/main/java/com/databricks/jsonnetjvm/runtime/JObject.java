package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Jsonnet object with layered inheritance. Each layer corresponds to one object literal. When
 * objects are merged with +, their layer lists are concatenated: (A+B)+C has layers [A,B,C]. Field
 * lookup searches from the topmost layer down. 'super' at layer i sees layers 0..i-1.
 */
public class JObject extends Val {

  // Thread-local bindings for self/super/$ during field evaluation
  private static final ThreadLocal<JObject> currentSelf = new ThreadLocal<>();
  private static final ThreadLocal<Integer> currentSuperMaxLayer =
      ThreadLocal.withInitial(() -> -1);
  private static final ThreadLocal<JObject> currentDollar = new ThreadLocal<>();
  // Stack of enclosing object scopes for resolving object-locals from nested
  // objects
  private static final ThreadLocal<Deque<JObject>> enclosingScopeStack =
      ThreadLocal.withInitial(ArrayDeque::new);

  public static JObject getCurrentSelf() {
    return currentSelf.get();
  }

  public static void setCurrentSelf(JObject self) {
    currentSelf.set(self);
  }

  public static int getCurrentSuperMaxLayer() {
    return currentSuperMaxLayer.get();
  }

  public static void setCurrentSuperMaxLayer(int layer) {
    currentSuperMaxLayer.set(layer);
  }

  public static JObject getCurrentDollar() {
    return currentDollar.get();
  }

  public static void setCurrentDollar(JObject dollar) {
    currentDollar.set(dollar);
  }

  public static void pushEnclosingScope(JObject scope) {
    enclosingScopeStack.get().push(scope);
  }

  public static void popEnclosingScope() {
    enclosingScopeStack.get().pop();
  }

  public static Deque<JObject> getEnclosingScopeStack() {
    return enclosingScopeStack.get();
  }

  /** Field visibility: normal (visible), hidden (::), or force-visible (:::). */
  public enum Visibility {
    NORMAL, // :
    HIDDEN, // ::
    FORCED // :::
  }

  /** A single layer of an object — the fields and locals defined in one object literal. */
  static class Layer {
    final Map<String, Supplier<Object>> fields = new LinkedHashMap<>();
    final Map<String, Visibility> fieldVisibility = new LinkedHashMap<>();
    final Map<String, Supplier<Object>> locals = new LinkedHashMap<>();
    final List<Runnable> asserts = new ArrayList<>();
  }

  // Layers: index 0 = bottommost, last = topmost
  private final List<Layer> layers;
  // Captured $ from creation context
  private JObject capturedDollar;
  // Cached sorted field names
  private Set<String> cachedFieldNames;
  // Guard against re-entrant assert evaluation
  private boolean assertsRunning;
  // Keys removed via objectRemoveKey — layers are kept intact for super
  private Set<String> removedKeys;

  public JObject() {
    this.layers = new ArrayList<>();
    this.layers.add(new Layer());
  }

  /** Constructor for merge/removeKey: reuses existing layers. */
  private JObject(List<Layer> layers) {
    this.layers = layers;
  }

  /**
   * Create a new object that hides the given key from the visible output but preserves the full
   * layer structure so super references still work.
   */
  @TruffleBoundary
  public JObject removeKey(String key) {
    JObject result = new JObject(new ArrayList<>(this.layers));
    result.capturedDollar = this.capturedDollar;
    result.removedKeys = new HashSet<>();
    if (this.removedKeys != null) result.removedKeys.addAll(this.removedKeys);
    result.removedKeys.add(key);
    return result;
  }

  public void addField(String name, Supplier<Object> body) {
    addField(name, body, Visibility.NORMAL);
  }

  public void addField(String name, Supplier<Object> body, Visibility visibility) {
    Layer layer = layers.get(layers.size() - 1);
    layer.fields.put(name, body);
    layer.fieldVisibility.put(name, visibility);
    cachedFieldNames = null;
  }

  public void addAssert(Runnable assertion) {
    layers.get(layers.size() - 1).asserts.add(assertion);
  }

  /** Mark all existing fields as hidden. Used for the std library object. */
  public void setAllFieldsHidden() {
    for (Layer layer : layers) {
      for (String name : layer.fields.keySet()) {
        layer.fieldVisibility.put(name, Visibility.HIDDEN);
      }
    }
    cachedFieldNames = null;
  }

  public void addLocal(String name, Supplier<Object> body) {
    layers.get(layers.size() - 1).locals.put(name, body);
  }

  public Map<String, Supplier<Object>> getLocals() {
    // Return accumulated locals from all layers (topmost wins)
    Map<String, Supplier<Object>> all = new LinkedHashMap<>();
    for (Layer layer : layers) {
      all.putAll(layer.locals);
    }
    return all;
  }

  public void setCapturedDollar(JObject dollar) {
    this.capturedDollar = dollar;
  }

  public JObject getCapturedDollar() {
    return capturedDollar;
  }

  /** Merge two objects: concatenate layer lists. */
  @TruffleBoundary
  public static JObject merge(JObject left, JObject right) {
    List<Layer> merged = new ArrayList<>(left.layers.size() + right.layers.size());
    merged.addAll(left.layers);
    merged.addAll(right.layers);
    return new JObject(merged);
  }

  /** Get a field value with self=this. Runs object asserts on first access. */
  @TruffleBoundary
  public Object getField(String name) {
    if (removedKeys != null && removedKeys.contains(name)) return null;
    if (!assertsRunning) {
      assertsRunning = true;
      try {
        runAsserts(this);
      } finally {
        assertsRunning = false;
      }
    }
    return evaluateField(name, this, layers.size() - 1);
  }

  /** Run all object asserts across all layers with the given self binding. */
  @TruffleBoundary
  private void runAsserts(JObject self) {
    JObject prevSelf = currentSelf.get();
    int prevSuperMax = currentSuperMaxLayer.get();
    JObject prevDollar = currentDollar.get();
    Deque<JObject> stack = enclosingScopeStack.get();
    try {
      currentSelf.set(self);
      if (this.capturedDollar != null) {
        currentDollar.set(this.capturedDollar);
      } else if (prevDollar == null) {
        currentDollar.set(self);
      }
      stack.push(self);
      for (int i = 0; i < layers.size(); i++) {
        currentSuperMaxLayer.set(i - 1);
        for (Runnable assertion : layers.get(i).asserts) {
          assertion.run();
        }
      }
    } finally {
      stack.pop();
      currentSelf.set(prevSelf);
      currentSuperMaxLayer.set(prevSuperMax);
      currentDollar.set(prevDollar);
    }
  }

  /**
   * Evaluate a field searching from maxLayer down, with explicit self binding. Used for super
   * access: super.field evaluates with a lower maxLayer but same self.
   */
  @TruffleBoundary
  public Object evaluateField(String name, JObject self, int maxLayer) {
    for (int i = maxLayer; i >= 0; i--) {
      Supplier<Object> body = layers.get(i).fields.get(name);
      if (body != null) {
        JObject prevSelf = currentSelf.get();
        int prevSuperMax = currentSuperMaxLayer.get();
        JObject prevDollar = currentDollar.get();
        Deque<JObject> stack = enclosingScopeStack.get();
        try {
          currentSelf.set(self);
          currentSuperMaxLayer.set(i - 1);
          if (this.capturedDollar != null) {
            currentDollar.set(this.capturedDollar);
          } else if (prevDollar == null) {
            currentDollar.set(self);
          }
          // Push self onto enclosing scope stack so that object-locals
          // defined on this object can be found during field evaluation
          stack.push(self);
          return body.get();
        } finally {
          stack.pop();
          currentSelf.set(prevSelf);
          currentSuperMaxLayer.set(prevSuperMax);
          currentDollar.set(prevDollar);
        }
      }
    }
    return null;
  }

  /**
   * Evaluate an object-local binding. Searches all layers for the local name. Called during field
   * evaluation when self/super/$ are already set in thread-locals.
   */
  @TruffleBoundary
  public Object getLocal(String name) {
    // Search from topmost layer down
    for (int i = layers.size() - 1; i >= 0; i--) {
      Supplier<Object> body = layers.get(i).locals.get(name);
      if (body != null) {
        return body.get();
      }
    }
    return null;
  }

  /**
   * Evaluate an object-local binding with explicit self binding. Used when resolving object-locals
   * from nested scopes where currentSelf may have changed to an inner object. The self parameter
   * should be the enclosing object that owns the local.
   */
  @TruffleBoundary
  public Object evaluateLocal(String name, JObject self) {
    for (int i = layers.size() - 1; i >= 0; i--) {
      Supplier<Object> body = layers.get(i).locals.get(name);
      if (body != null) {
        JObject prevSelf = currentSelf.get();
        int prevSuperMax = currentSuperMaxLayer.get();
        try {
          currentSelf.set(self);
          currentSuperMaxLayer.set(layers.size() - 2);
          return body.get();
        } finally {
          currentSelf.set(prevSelf);
          currentSuperMaxLayer.set(prevSuperMax);
        }
      }
    }
    return null;
  }

  public boolean hasLocal(String name) {
    for (int i = layers.size() - 1; i >= 0; i--) {
      if (layers.get(i).locals.containsKey(name)) return true;
    }
    return false;
  }

  /** Check if a visible field exists (excludes hidden and removed). */
  @TruffleBoundary
  public boolean hasField(String name) {
    if (removedKeys != null && removedKeys.contains(name)) return false;
    boolean found = false;
    for (int i = layers.size() - 1; i >= 0; i--) {
      if (layers.get(i).fields.containsKey(name)) {
        found = true;
        break;
      }
    }
    if (!found) return false;
    return getFieldVisibility(name) != Visibility.HIDDEN;
  }

  /** Check if a field exists in layers up to maxLayer (for 'in super'). */
  @TruffleBoundary
  public boolean hasFieldInLayers(String name, int maxLayer) {
    if (removedKeys != null && removedKeys.contains(name)) return false;
    for (int i = Math.min(maxLayer, layers.size() - 1); i >= 0; i--) {
      if (layers.get(i).fields.containsKey(name)) return true;
    }
    return false;
  }

  /** Check if any field exists (including hidden, excluding removed). */
  @TruffleBoundary
  public boolean hasFieldAll(String name) {
    if (removedKeys != null && removedKeys.contains(name)) return false;
    for (int i = layers.size() - 1; i >= 0; i--) {
      if (layers.get(i).fields.containsKey(name)) return true;
    }
    return false;
  }

  /**
   * Get the effective visibility for a field. NORMAL (:) is transparent and inherits from the base.
   * HIDDEN (::) and FORCED (:::) explicitly set visibility; the latest non-NORMAL wins.
   */
  @TruffleBoundary
  public Visibility getFieldVisibility(String name) {
    Visibility result = null;
    for (int i = 0; i < layers.size(); i++) {
      Visibility v = layers.get(i).fieldVisibility.get(name);
      if (v != null) {
        if (result == null) {
          result = v;
        } else if (v != Visibility.NORMAL) {
          result = v;
        }
      }
    }
    return result != null ? result : Visibility.NORMAL;
  }

  /**
   * Get the raw field supplier without forcing it. Searches from topmost layer down. Returns null
   * if the field does not exist.
   */
  @TruffleBoundary
  public Supplier<Object> getFieldSupplier(String name) {
    if (removedKeys != null && removedKeys.contains(name)) return null;
    for (int i = layers.size() - 1; i >= 0; i--) {
      Supplier<Object> body = layers.get(i).fields.get(name);
      if (body != null) {
        final JObject self = this;
        final int layerIdx = i;
        return () -> evaluateField(name, self, layerIdx);
      }
    }
    return null;
  }

  /** Get visible field names (excludes hidden and removed fields, includes forced fields). */
  @TruffleBoundary
  public Set<String> getFieldNames() {
    if (cachedFieldNames != null) return cachedFieldNames;
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (Layer layer : layers) {
      names.addAll(layer.fields.keySet());
    }
    names.removeIf(name -> getFieldVisibility(name) == Visibility.HIDDEN);
    if (removedKeys != null) names.removeAll(removedKeys);
    cachedFieldNames = Collections.unmodifiableSet(names);
    return cachedFieldNames;
  }

  /** Get ALL field names including hidden ones but excluding removed. */
  @TruffleBoundary
  public Set<String> getAllFieldNames() {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (Layer layer : layers) {
      names.addAll(layer.fields.keySet());
    }
    if (removedKeys != null) names.removeAll(removedKeys);
    return Collections.unmodifiableSet(names);
  }

  @Override
  @TruffleBoundary
  public String toJson() {
    // Ensure object asserts are checked when manifesting
    if (!assertsRunning) {
      assertsRunning = true;
      try {
        runAsserts(this);
      } finally {
        assertsRunning = false;
      }
    }
    return JsonnetJson.renderInline(this);
  }
}

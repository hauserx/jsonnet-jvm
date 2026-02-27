package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

public class JsonnetObjectLiteralNode extends JsonnetExpressionNode {
  // Static field names (null if computed)
  private final String[] staticFieldNames;
  // Computed field name expressions (null if static)
  @Children private final JsonnetExpressionNode[] fieldNameNodes;
  @Children private final JsonnetToValNode[] toValNodes;
  // Field visibility per field
  private final JObject.Visibility[] fieldVisibilities;
  // Frame slots for computed +: fields (-1 if not applicable)
  private final int[] computedPlusKeySlots;

  // Object-local binding assignments (evaluated before fields)
  @Children private final JsonnetExpressionNode[] localAssignments;
  private final String[] localNames;
  @Children private final JsonnetExpressionNode[] localValueNodes;

  // Object asserts (evaluated lazily when fields are accessed)
  @Children private final JsonnetExpressionNode[] assertConditions;
  @Children private final JsonnetExpressionNode[] assertMessages;

  public JsonnetObjectLiteralNode(
      String[] staticFieldNames,
      JsonnetExpressionNode[] fieldNameNodes,
      JsonnetExpressionNode[] valueNodes,
      JObject.Visibility[] fieldVisibilities,
      int[] computedPlusKeySlots,
      JsonnetExpressionNode[] localAssignments,
      String[] localNames,
      JsonnetExpressionNode[] localValueNodes,
      JsonnetExpressionNode[] assertConditions,
      JsonnetExpressionNode[] assertMessages) {
    this.staticFieldNames = staticFieldNames;
    this.fieldNameNodes = fieldNameNodes;
    this.toValNodes = new JsonnetToValNode[valueNodes.length];
    for (int i = 0; i < valueNodes.length; i++) {
      this.toValNodes[i] = JsonnetToValNodeGen.create(valueNodes[i]);
    }
    this.fieldVisibilities = fieldVisibilities;
    this.computedPlusKeySlots = computedPlusKeySlots;
    this.localAssignments = localAssignments;
    this.localNames = localNames;
    this.localValueNodes = localValueNodes;
    this.assertConditions = assertConditions;
    this.assertMessages = assertMessages;
  }

  /** Convenience constructor for simple objects with only static field names and no locals. */
  public JsonnetObjectLiteralNode(String[] fieldNames, JsonnetExpressionNode[] valueNodes) {
    this(
        fieldNames,
        new JsonnetExpressionNode[fieldNames.length], // all null = all static
        valueNodes,
        new JObject.Visibility[0], // empty = all NORMAL
        new int[0],
        new JsonnetExpressionNode[0],
        new String[0],
        new JsonnetExpressionNode[0],
        new JsonnetExpressionNode[0],
        new JsonnetExpressionNode[0]);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return createAndPopulate(frame.materialize());
  }

  @TruffleBoundary
  private JObject createAndPopulate(MaterializedFrame frame) {
    // Evaluate local assignments (writes thunks to frame slots)
    for (JsonnetExpressionNode localAssign : localAssignments) {
      localAssign.executeGeneric(frame);
    }

    JObject obj = new JObject();

    // Capture enclosing self for object-local resolution from nested scopes.
    final JObject capturedEnclosing = JObject.getCurrentSelf();

    // Add object-local bindings
    for (int i = 0; i < localNames.length; i++) {
      final int idx = i;
      obj.addLocal(localNames[i], () -> localValueNodes[idx].executeGeneric(frame));
    }

    // Add fields with enclosing scope push/pop and visibility
    for (int i = 0; i < toValNodes.length; i++) {
      String fieldName;
      if (staticFieldNames[i] != null) {
        fieldName = staticFieldNames[i];
      } else {
        // Computed field name — null keys mean skip the field
        Object nameVal = fieldNameNodes[i].executeGeneric(frame);
        if (nameVal == null || nameVal instanceof JNull) {
          continue;
        }
        if (!(nameVal instanceof String)) {
          throw new JsonnetException(
              "Field name must be a string, got " + JsonnetException.typeName(nameVal), this);
        }
        fieldName = (String) nameVal;
        // Store computed key in frame for +: value expressions to read
        if (i < computedPlusKeySlots.length && computedPlusKeySlots[i] >= 0) {
          frame.setObject(computedPlusKeySlots[i], fieldName);
        }
      }
      if (obj.hasFieldAll(fieldName)) {
        throw new JsonnetException(
            "Duplicate key " + fieldName + " in evaluated object literal.", this);
      }
      final int index = i;
      JObject.Visibility vis =
          (fieldVisibilities.length > i) ? fieldVisibilities[i] : JObject.Visibility.NORMAL;

      if (capturedEnclosing != null) {
        obj.addField(
            fieldName,
            () -> {
              JObject.pushEnclosingScope(capturedEnclosing);
              try {
                return toValNodes[index].executeGeneric(frame);
              } finally {
                JObject.popEnclosingScope();
              }
            },
            vis);
      } else {
        obj.addField(fieldName, () -> toValNodes[index].executeGeneric(frame), vis);
      }
    }

    // Add object asserts as suppliers (evaluated lazily when fields are accessed)
    if (assertConditions.length > 0) {
      for (int i = 0; i < assertConditions.length; i++) {
        final int idx = i;
        final JsonnetExpressionNode condNode = assertConditions[idx];
        obj.addAssert(
            () -> {
              Object cond = condNode.executeGeneric(frame);
              if (cond instanceof Boolean && !(Boolean) cond) {
                if (assertMessages[idx] != null) {
                  Object msg = assertMessages[idx].executeGeneric(frame);
                  throw new JsonnetException("Assertion failed: " + msg, condNode);
                }
                throw new JsonnetException("Assertion failed", condNode);
              }
            });
      }
    }

    // Capture $ from current context
    JObject dollar = JObject.getCurrentDollar();
    if (dollar != null) {
      obj.setCapturedDollar(dollar);
    }

    return obj;
  }
}

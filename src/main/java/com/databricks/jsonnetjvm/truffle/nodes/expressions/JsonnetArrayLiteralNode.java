package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import java.util.ArrayList;
import java.util.List;

public class JsonnetArrayLiteralNode extends JsonnetExpressionNode {
  @Children private final JsonnetExpressionNode[] elementNodes;

  public JsonnetArrayLiteralNode(JsonnetExpressionNode[] elementNodes) {
    this.elementNodes = elementNodes;
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    MaterializedFrame mf = frame.materialize();
    List<Object> elements = new ArrayList<>(elementNodes.length);
    for (JsonnetExpressionNode node : elementNodes) {
      elements.add(JsonnetThunk.withSelfCapture(() -> node.executeGeneric(mf)));
    }
    return new JGenericArray(elements);
  }
}

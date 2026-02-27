package com.databricks.jsonnetjvm.truffle.nodes.expressions;

import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class JsonnetBinaryNode extends JsonnetExpressionNode {}

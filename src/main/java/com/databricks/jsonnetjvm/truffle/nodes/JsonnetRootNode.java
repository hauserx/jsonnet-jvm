package com.databricks.jsonnetjvm.truffle.nodes;

import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

public class JsonnetRootNode extends RootNode {

  @Child private JsonnetExpressionNode body;
  private final String name;
  private final Source source;

  public JsonnetRootNode(
      JsonnetLanguage language, FrameDescriptor frameDescriptor, JsonnetExpressionNode body) {
    this(language, frameDescriptor, body, "(anonymous)", null);
  }

  public JsonnetRootNode(
      JsonnetLanguage language,
      FrameDescriptor frameDescriptor,
      JsonnetExpressionNode body,
      String name,
      Source source) {
    super(language, frameDescriptor);
    this.body = body;
    this.name = name;
    this.source = source;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return body.executeGeneric(frame);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SourceSection getSourceSection() {
    if (source != null) {
      return source.createSection(0, source.getLength());
    }
    return null;
  }
}

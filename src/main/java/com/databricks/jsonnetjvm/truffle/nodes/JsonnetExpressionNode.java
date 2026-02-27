package com.databricks.jsonnetjvm.truffle.nodes;

import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.JsonnetTypes;
import com.databricks.jsonnetjvm.truffle.JsonnetTypesGen;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

@TypeSystemReference(JsonnetTypes.class)
public abstract class JsonnetExpressionNode extends Node {

  private int sourceCharIndex = -1;
  private int sourceLength;

  public void setSourceSection(int charIndex, int length) {
    this.sourceCharIndex = charIndex;
    this.sourceLength = length;
  }

  @Override
  @TruffleBoundary
  public SourceSection getSourceSection() {
    if (sourceCharIndex < 0) {
      return null;
    }
    RootNode rootNode = getRootNode();
    if (rootNode == null) {
      return null;
    }
    SourceSection rootSection = rootNode.getSourceSection();
    if (rootSection == null) {
      return null;
    }
    Source source = rootSection.getSource();
    return source.createSection(sourceCharIndex, sourceLength);
  }

  public boolean hasSourceSection() {
    return sourceCharIndex >= 0;
  }

  public abstract Object executeGeneric(VirtualFrame frame);

  public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
    return JsonnetTypesGen.expectBoolean(executeGeneric(frame));
  }

  public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
    return JsonnetTypesGen.expectDouble(executeGeneric(frame));
  }

  public String executeString(VirtualFrame frame) throws UnexpectedResultException {
    return JsonnetTypesGen.expectString(executeGeneric(frame));
  }

  public RootNode asRootNode(
      JsonnetLanguage language, com.oracle.truffle.api.frame.FrameDescriptor frameDescriptor) {
    return new JsonnetRootNode(language, frameDescriptor, this);
  }

  public RootNode asRootNode(
      JsonnetLanguage language,
      com.oracle.truffle.api.frame.FrameDescriptor frameDescriptor,
      String name,
      Source source) {
    return new JsonnetRootNode(language, frameDescriptor, this, name, source);
  }
}

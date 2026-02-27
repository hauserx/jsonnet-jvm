package com.databricks.jsonnetjvm.truffle.parser;

import com.databricks.jsonnetjvm.JsonnetBaseVisitor;
import com.databricks.jsonnetjvm.JsonnetParser;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetInternalError;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetRootNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetAddNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetArgReadNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetArrayComprehensionNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetArrayLiteralNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetAssertNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBitwiseAndNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBitwiseOrNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBitwiseXorNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetCallNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetComputedFieldObjectNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetDivideNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetDollarNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetEqualNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetErrorNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetFieldAccessNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetFieldPlusNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetFunctionLiteralNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetGreaterEqualNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetGreaterThanNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetIfElseNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetImportNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetImportStrNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetInNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetInSuperNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetIndexNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetLessEqualNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetLessThanNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetLocalBlockNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetLogicalAndNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetLogicalOrNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetModuloNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetMultiplyNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetNotEqualNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetObjectComprehensionNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetObjectLiteralNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetObjectLocalReadNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetParamDefaultNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetReadClosureVariableNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetReadLocalVariableNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetSelfNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetShiftLeftNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetShiftRightNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetSliceNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetSubtractNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetSuperFieldNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetSuperIndexNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetUnaryBitNotNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetUnaryMinusNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetUnaryNotNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetUnaryPlusNodeGen;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetWriteLocalVariableNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetWriteThunkNode;
import com.databricks.jsonnetjvm.truffle.nodes.literals.JsonnetBooleanLiteralNode;
import com.databricks.jsonnetjvm.truffle.nodes.literals.JsonnetNullLiteralNode;
import com.databricks.jsonnetjvm.truffle.nodes.literals.JsonnetNumberLiteralNode;
import com.databricks.jsonnetjvm.truffle.nodes.literals.JsonnetStringLiteralNode;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public class JsonnetTruffleASTBuilder extends JsonnetBaseVisitor<JsonnetExpressionNode> {

  private final JsonnetLanguage language;
  private final FrameDescriptor.Builder frameDescriptorBuilder = FrameDescriptor.newBuilder();
  private final Map<String, Integer> variableSlots = new HashMap<>();
  private final int stdSlot;
  private final String sourceName;
  private final Source source; // Truffle Source for source sections (may be null)

  private final Map<String, Integer> closureVariables = new HashMap<>();
  private int closureFrameSlot = -1;

  private final Set<String> objectLocalNames = new HashSet<>();

  public JsonnetTruffleASTBuilder(JsonnetLanguage language) {
    this(language, "<stdin>", null);
  }

  public JsonnetTruffleASTBuilder(JsonnetLanguage language, String sourceName) {
    this(language, sourceName, null);
  }

  public JsonnetTruffleASTBuilder(JsonnetLanguage language, String sourceName, Source source) {
    this.language = language;
    this.sourceName = sourceName;
    this.source = source;
    this.stdSlot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, "std", null);
    variableSlots.put("std", stdSlot);
  }

  private JsonnetTruffleASTBuilder(
      JsonnetLanguage language, int stdSlot, String sourceName, Source source) {
    this.language = language;
    this.stdSlot = stdSlot;
    this.sourceName = sourceName;
    this.source = source;
  }

  /** Assign source section from an ANTLR context to a Truffle node. */
  private <T extends JsonnetExpressionNode> T withSourceSection(T node, ParserRuleContext ctx) {
    if (source != null && ctx != null && ctx.getStart() != null) {
      Token start = ctx.getStart();
      Token stop = ctx.getStop();
      int startIndex = start.getStartIndex();
      int stopIndex = stop != null ? stop.getStopIndex() : start.getStopIndex();
      if (startIndex >= 0 && stopIndex >= startIndex) {
        node.setSourceSection(startIndex, stopIndex - startIndex + 1);
      }
    }
    return node;
  }

  public FrameDescriptor getFrameDescriptor() {
    return frameDescriptorBuilder.build();
  }

  public int getStdSlot() {
    return stdSlot;
  }

  @Override
  public JsonnetExpressionNode visit(ParseTree tree) {
    JsonnetExpressionNode node = super.visit(tree);
    if (node != null
        && !node.hasSourceSection()
        && source != null
        && tree instanceof ParserRuleContext ctx) {
      withSourceSection(node, ctx);
    }
    return node;
  }

  @Override
  public JsonnetExpressionNode visitJsonnet(JsonnetParser.JsonnetContext ctx) {
    return visit(ctx.expr());
  }

  // --- Literals ---

  @Override
  public JsonnetExpressionNode visitNumberLit(JsonnetParser.NumberLitContext ctx) {
    double val = Double.parseDouble(ctx.getText().replace("_", ""));
    if (Double.isInfinite(val) || Double.isNaN(val)) {
      throw new JsonnetStaticError("overflow");
    }
    return new JsonnetNumberLiteralNode(val);
  }

  @Override
  public JsonnetExpressionNode visitStringLit(JsonnetParser.StringLitContext ctx) {
    String text = ctx.getText();
    String value = processStringLiteral(text);
    return new JsonnetStringLiteralNode(value);
  }

  /** Process a Jsonnet string literal token into its actual string value. */
  static String processStringLiteral(String text) {
    if (text.startsWith("|||")) {
      return processTextBlock(text);
    } else if (text.startsWith("@\"")) {
      // Verbatim double-quoted: @"..." — doubled quotes "" become "
      String inner = text.substring(2, text.length() - 1);
      return inner.replace("\"\"", "\"");
    } else if (text.startsWith("@'")) {
      // Verbatim single-quoted: @'...' — doubled quotes '' become '
      String inner = text.substring(2, text.length() - 1);
      return inner.replace("''", "'");
    } else if (text.startsWith("\"") || text.startsWith("'")) {
      // Regular quoted string — process escape sequences
      String inner = text.substring(1, text.length() - 1);
      return processEscapes(inner);
    }
    throw new JsonnetStaticError("Unrecognized string literal: " + text);
  }

  /** Process backslash escape sequences in a regular string. */
  private static String processEscapes(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' && i + 1 < s.length()) {
        char next = s.charAt(i + 1);
        switch (next) {
          case '"':
            sb.append('"');
            i++;
            break;
          case '\'':
            sb.append('\'');
            i++;
            break;
          case '\\':
            sb.append('\\');
            i++;
            break;
          case '/':
            sb.append('/');
            i++;
            break;
          case 'b':
            sb.append('\b');
            i++;
            break;
          case 'f':
            sb.append('\f');
            i++;
            break;
          case 'n':
            sb.append('\n');
            i++;
            break;
          case 'r':
            sb.append('\r');
            i++;
            break;
          case 't':
            sb.append('\t');
            i++;
            break;
          case 'u':
            if (i + 5 < s.length()) {
              String hex = s.substring(i + 2, i + 6);
              int codePoint = Integer.parseInt(hex, 16);
              if (Character.isHighSurrogate((char) codePoint)) {
                if (i + 11 < s.length() && s.charAt(i + 6) == '\\' && s.charAt(i + 7) == 'u') {
                  String hex2 = s.substring(i + 8, i + 12);
                  int codePoint2 = Integer.parseInt(hex2, 16);
                  if (Character.isLowSurrogate((char) codePoint2)) {
                    int fullCodePoint = Character.toCodePoint((char) codePoint, (char) codePoint2);
                    sb.append(Character.toChars(fullCodePoint));
                    i += 11;
                  } else {
                    throw new JsonnetStaticError(
                        "Expected a low surrogate after high surrogate \\u" + hex);
                  }
                } else {
                  throw new JsonnetStaticError(
                      "Expected a low surrogate after high surrogate \\u" + hex);
                }
              } else if (Character.isLowSurrogate((char) codePoint)) {
                throw new JsonnetStaticError(
                    "Unexpected low surrogate \\u" + hex + " without preceding high surrogate");
              } else {
                sb.append((char) codePoint);
                i += 5;
              }
            } else {
              sb.append(c);
            }
            break;
          default:
            sb.append(c);
            break;
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /** Process a text block literal (|||...|||). */
  private static String processTextBlock(String text) {
    // Strip opening and closing ||| delimiters
    String inner = text.substring(3, text.length() - 3);

    // Check for chomp mode (|||- strips trailing newline)
    boolean chomp = false;
    if (inner.startsWith("-")) {
      chomp = true;
      inner = inner.substring(1);
    }

    // Detect line ending style (preserve original)
    String lineEnding = inner.contains("\r\n") ? "\r\n" : "\n";

    // Normalize to \n for splitting, then restore when joining
    String normalized = inner.replace("\r\n", "\n").replace("\r", "\n");

    // Text blocks require a newline after the opening |||
    if (!normalized.contains("\n")) {
      throw new JsonnetStaticError("|||-blocks require multiple lines");
    }

    // Split into lines
    String[] lines = normalized.split("\n", -1);

    // First line (after opening |||) should be empty/whitespace — skip it
    // Last line (before closing |||) is indentation reference — skip it
    if (lines.length < 2) {
      return chomp ? "" : lineEnding;
    }

    // Extract content lines (between first and last)
    List<String> contentLines = new ArrayList<>();
    for (int i = 1; i < lines.length - 1; i++) {
      contentLines.add(lines[i]);
    }

    if (contentLines.isEmpty()) {
      return chomp ? "" : lineEnding;
    }

    // Find the indentation of the first non-empty content line as reference
    int refIndent = 0;
    char refChar = ' ';
    for (String line : contentLines) {
      if (!line.trim().isEmpty()) {
        while (refIndent < line.length()
            && (line.charAt(refIndent) == ' ' || line.charAt(refIndent) == '\t')) {
          refChar = line.charAt(refIndent);
          refIndent++;
        }
        break;
      }
    }

    // Validate indentation of all non-empty content lines
    for (String line : contentLines) {
      if (line.trim().isEmpty()) continue;
      int indent = 0;
      while (indent < line.length()
          && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
        indent++;
      }
      if (indent < refIndent) {
        String found =
            indent
                + (line.charAt(0) == '\t'
                    ? " tab" + (indent != 1 ? "s" : "")
                    : " space" + (indent != 1 ? "s" : ""));
        String expected =
            refIndent
                + " "
                + (refChar == '\t'
                    ? "tab" + (refIndent != 1 ? "s" : "")
                    : "space" + (refIndent != 1 ? "s" : ""));
        throw new JsonnetStaticError(
            "text block indentation mismatch: expected at least " + expected + ", found " + found);
      }
      if (indent > 0 && line.charAt(0) != refChar) {
        String found =
            indent
                + (line.charAt(0) == '\t'
                    ? " tab" + (indent != 1 ? "s" : "")
                    : " space" + (indent != 1 ? "s" : ""));
        String expected =
            refIndent
                + " "
                + (refChar == '\t'
                    ? "tab" + (refIndent != 1 ? "s" : "")
                    : "space" + (refIndent != 1 ? "s" : ""));
        throw new JsonnetStaticError(
            "text block indentation mismatch: expected at least " + expected + ", found " + found);
      }
    }

    int minIndent = refIndent;

    // Strip common indentation and join with original line endings
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < contentLines.size(); i++) {
      String line = contentLines.get(i);
      if (line.trim().isEmpty()) {
        // Blank line — keep as empty
        sb.append("");
      } else {
        sb.append(line.substring(minIndent));
      }
      if (i < contentLines.size() - 1) {
        sb.append(lineEnding);
      }
    }

    if (!chomp) {
      sb.append(lineEnding);
    }

    return sb.toString();
  }

  @Override
  public JsonnetExpressionNode visitTrue(JsonnetParser.TrueContext ctx) {
    return new JsonnetBooleanLiteralNode(true);
  }

  @Override
  public JsonnetExpressionNode visitFalse(JsonnetParser.FalseContext ctx) {
    return new JsonnetBooleanLiteralNode(false);
  }

  @Override
  public JsonnetExpressionNode visitNull(JsonnetParser.NullContext ctx) {
    return new JsonnetNullLiteralNode();
  }

  // --- Self / Super / Dollar ---

  @Override
  public JsonnetExpressionNode visitSelf(JsonnetParser.SelfContext ctx) {
    return new JsonnetSelfNode();
  }

  @Override
  public JsonnetExpressionNode visitDollar(JsonnetParser.DollarContext ctx) {
    return new JsonnetDollarNode();
  }

  @Override
  public JsonnetExpressionNode visitSuperField(JsonnetParser.SuperFieldContext ctx) {
    String fieldName = ctx.id().getText();
    return new JsonnetSuperFieldNode(fieldName);
  }

  @Override
  public JsonnetExpressionNode visitSuperIndex(JsonnetParser.SuperIndexContext ctx) {
    JsonnetExpressionNode indexExpr = visit(ctx.expr());
    return new JsonnetSuperIndexNode(indexExpr);
  }

  @Override
  public JsonnetExpressionNode visitInSuper(JsonnetParser.InSuperContext ctx) {
    JsonnetExpressionNode keyExpr = visit(ctx.expr());
    return new JsonnetInSuperNode(keyExpr);
  }

  // --- Variables & Scope ---

  @Override
  public JsonnetExpressionNode visitVar(JsonnetParser.VarContext ctx) {
    String name = ctx.getText();
    // Check object-local names first (resolved via currentSelf at runtime)
    if (objectLocalNames.contains(name)) {
      return new JsonnetObjectLocalReadNode(name);
    }

    // Check local variables
    Integer slot = variableSlots.get(name);
    if (slot != null) {
      return new JsonnetReadLocalVariableNode(slot, name);
    }
    // Check closure variables (from outer scope)
    Integer closureVarSlot = closureVariables.get(name);
    if (closureVarSlot != null && closureFrameSlot >= 0) {
      return new JsonnetReadClosureVariableNode(closureFrameSlot, closureVarSlot, name);
    }
    throw new JsonnetStaticError("Unknown variable: " + name);
  }

  @Override
  public JsonnetExpressionNode visitLocalVar(JsonnetParser.LocalVarContext ctx) {
    Map<String, Integer> savedSlots = new HashMap<>(variableSlots);

    // Allocate ALL slots first so forward references work (e.g. mutual recursion)
    int[] slots = new int[ctx.bind().size()];
    for (int i = 0; i < ctx.bind().size(); i++) {
      JsonnetParser.BindContext bind = ctx.bind(i);
      String name = bind.id().getText();
      slots[i] = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, name, null);
      variableSlots.put(name, slots[i]);
    }

    // Now visit expressions with all slots visible, using thunks for lazy
    // evaluation
    JsonnetExpressionNode[] assignments = new JsonnetExpressionNode[ctx.bind().size()];
    for (int i = 0; i < ctx.bind().size(); i++) {
      JsonnetParser.BindContext bind = ctx.bind(i);
      if (isFunctionBind(bind)) {
        assignments[i] = new JsonnetWriteThunkNode(slots[i], buildFunction(bind));
      } else {
        assignments[i] = new JsonnetWriteThunkNode(slots[i], visit(bind.expr()));
      }
    }
    JsonnetExpressionNode body = visit(ctx.expr());

    variableSlots.clear();
    variableSlots.putAll(savedSlots);

    return new JsonnetLocalBlockNode(assignments, body);
  }

  private JsonnetExpressionNode buildFunctionBody(
      JsonnetParser.ParamsContext params, JsonnetParser.ExprContext bodyExpr, String funcName) {
    JsonnetTruffleASTBuilder bodyBuilder =
        new JsonnetTruffleASTBuilder(language, -1, sourceName, source);

    // Slot 0: std
    int bodyStdSlot = bodyBuilder.frameDescriptorBuilder.addSlot(FrameSlotKind.Object, "std", null);
    bodyBuilder.variableSlots.put("std", bodyStdSlot);

    // Slot 1: closure frame reference
    int closureSlot =
        bodyBuilder.frameDescriptorBuilder.addSlot(FrameSlotKind.Object, "$closure", null);
    bodyBuilder.closureFrameSlot = closureSlot;

    // Record outer variables as closure variables
    for (Map.Entry<String, Integer> entry : this.variableSlots.entrySet()) {
      if (!entry.getKey().equals("std")) {
        bodyBuilder.closureVariables.put(entry.getKey(), entry.getValue());
      }
    }
    // Also inherit closure variables from parent (for nested closures)
    for (Map.Entry<String, Integer> entry : this.closureVariables.entrySet()) {
      if (!bodyBuilder.closureVariables.containsKey(entry.getKey())) {
        bodyBuilder.closureVariables.put(entry.getKey(), entry.getValue());
      }
    }

    // Inherit object-local names (functions inside objects can reference
    // object-locals)
    bodyBuilder.objectLocalNames.addAll(this.objectLocalNames);

    List<JsonnetExpressionNode> argAssignments = new ArrayList<>();
    String[] paramNames = null;

    if (params != null) {
      // First pass: allocate ALL parameter slots so forward references work
      // (e.g. function(a=[1, b[1]], b=[a[0], 2]) where a references b)
      int[] paramSlots = new int[params.param().size()];
      paramNames = new String[params.param().size()];
      for (int i = 0; i < params.param().size(); i++) {
        String paramName = params.param().get(i).id().getText();
        paramNames[i] = paramName;
        paramSlots[i] =
            bodyBuilder.frameDescriptorBuilder.addSlot(FrameSlotKind.Object, paramName, null);
        bodyBuilder.variableSlots.put(paramName, paramSlots[i]);
      }

      // Second pass: visit default expressions with all params in scope
      for (int i = 0; i < params.param().size(); i++) {
        JsonnetParser.ParamContext param = params.param().get(i);
        String paramName = paramNames[i];
        boolean hasDefault = param.expr() != null;
        JsonnetExpressionNode paramValueNode;

        if (hasDefault) {
          JsonnetExpressionNode defaultValueNode = bodyBuilder.visit(param.expr());
          paramValueNode = new JsonnetParamDefaultNode(i, paramName, defaultValueNode);
        } else {
          paramValueNode = new JsonnetArgReadNode(i, paramName);
        }

        argAssignments.add(new JsonnetWriteLocalVariableNode(paramSlots[i], paramValueNode));
      }
    }

    JsonnetExpressionNode body = bodyBuilder.visit(bodyExpr);

    // Build prologue: std, closure frame, then params
    List<JsonnetExpressionNode> allAssignments = new ArrayList<>();
    allAssignments.add(new JsonnetWriteLocalVariableNode(bodyStdSlot, new StdObjectNode(language)));
    // Write closure frame from arguments[0]
    allAssignments.add(
        new JsonnetWriteLocalVariableNode(
            closureSlot,
            new JsonnetExpressionNode() {
              @Override
              public Object executeGeneric(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                return args.length > 0 ? args[0] : null;
              }
            }));
    allAssignments.addAll(argAssignments);

    JsonnetExpressionNode fullBody =
        new JsonnetLocalBlockNode(allAssignments.toArray(new JsonnetExpressionNode[0]), body);

    JsonnetRootNode rootNode =
        new JsonnetRootNode(language, bodyBuilder.getFrameDescriptor(), fullBody, funcName, source);

    int paramCount = params != null ? params.param().size() : 0;
    return new JsonnetFunctionLiteralNode(
        funcName, rootNode.getCallTarget(), paramCount, paramNames);
  }

  /**
   * Distinguishes {@code local f() = ...} (function bind, has parens) from {@code local x = ...}
   * (value bind). Needed because zero-arg functions have {@code bind.params() == null} just like
   * value bindings.
   */
  private static boolean isFunctionBind(JsonnetParser.BindContext bind) {
    return bind.getChildCount() > 3 && "(".equals(bind.getChild(1).getText());
  }

  private static boolean isFunctionField(JsonnetParser.FieldContext field) {
    for (int i = 0; i < field.getChildCount(); i++) {
      String text = field.getChild(i).getText();
      if ("(".equals(text)) return true;
      if (":".equals(text) || "+".equals(text)) return false;
    }
    return false;
  }

  private JsonnetExpressionNode buildFunction(JsonnetParser.BindContext bind) {
    return buildFunctionBody(bind.params(), bind.expr(), bind.id().getText());
  }

  // --- Field Access ---

  @Override
  public JsonnetExpressionNode visitFieldAccess(JsonnetParser.FieldAccessContext ctx) {
    JsonnetExpressionNode object = visit(ctx.expr());
    String fieldName = ctx.id().getText();
    return new JsonnetFieldAccessNode(object, fieldName);
  }

  // --- Index / Slice ---

  @Override
  public JsonnetExpressionNode visitIndexOrSlice(JsonnetParser.IndexOrSliceContext ctx) {
    JsonnetExpressionNode target = visit(ctx.expr(0));

    // Check if this is a slice (has a colon)
    boolean hasColon = false;
    for (int i = 0; i < ctx.getChildCount(); i++) {
      if (":".equals(ctx.getChild(i).getText())) {
        hasColon = true;
        break;
      }
    }

    if (!hasColon) {
      // Simple index: expr[expr]
      JsonnetExpressionNode index = visit(ctx.expr(1));
      return new JsonnetIndexNode(target, index);
    }

    // Slice: expr[start?:end?(:step?)?]
    // Parse the children between [ and ] to find start, end, step
    // Children pattern: target '[' start? ':' end? (':' step?)? ']'
    JsonnetExpressionNode startNode = null;
    JsonnetExpressionNode endNode = null;
    JsonnetExpressionNode stepNode = null;

    int colonCount = 0;
    int exprIndex = 1; // expr(0) is target, remaining are slice components

    // Walk children between '[' and ']' to map expressions to positions
    boolean inBracket = false;
    boolean expectExprBeforeColon = true;
    int position = 0; // 0=start, 1=end, 2=step

    for (int i = 0; i < ctx.getChildCount(); i++) {
      String childText = ctx.getChild(i).getText();
      if ("[".equals(childText)) {
        inBracket = true;
        continue;
      }
      if ("]".equals(childText)) {
        break;
      }
      if (!inBracket) continue;

      if (":".equals(childText)) {
        position++;
        continue;
      }

      // This child is an expression
      if (exprIndex < ctx.expr().size()) {
        JsonnetExpressionNode exprNode = visit(ctx.expr(exprIndex));
        exprIndex++;
        switch (position) {
          case 0:
            startNode = exprNode;
            break;
          case 1:
            endNode = exprNode;
            break;
          case 2:
            stepNode = exprNode;
            break;
        }
      }
    }

    return new JsonnetSliceNode(target, startNode, endNode, stepNode);
  }

  // --- Function Calls ---

  @Override
  public JsonnetExpressionNode visitCall(JsonnetParser.CallContext ctx) {
    // Visit args first to preserve AST build order (undefined-variable errors in
    // args
    // should be reported before errors in the function expression)
    JsonnetExpressionNode[] argExprs;
    String[] argNames;

    if (ctx.args() != null) {
      List<JsonnetParser.ExprContext> allExprs = ctx.args().expr();
      List<JsonnetParser.IdContext> namedIds = ctx.args().id();
      int numNamed = (namedIds != null) ? namedIds.size() : 0;
      int numPositional = allExprs.size() - numNamed;

      argExprs = new JsonnetExpressionNode[allExprs.size()];
      for (int i = 0; i < allExprs.size(); i++) {
        argExprs[i] = visit(allExprs.get(i));
      }

      argNames = new String[allExprs.size()];
      for (int i = 0; i < numNamed; i++) {
        argNames[numPositional + i] = namedIds.get(i).getText();
      }
    } else {
      argExprs = new JsonnetExpressionNode[0];
      argNames = new String[0];
    }

    JsonnetExpressionNode funcNode = visit(ctx.expr());
    boolean tailstrict = ctx.TAILSTRICT() != null;
    return new JsonnetCallNode(funcNode, argExprs, argNames, tailstrict);
  }

  // --- Arrays ---

  @Override
  public JsonnetExpressionNode visitArray(JsonnetParser.ArrayContext ctx) {
    List<JsonnetExpressionNode> elementNodes = new ArrayList<>();
    if (ctx.expr() != null) {
      for (JsonnetParser.ExprContext expr : ctx.expr()) {
        elementNodes.add(visit(expr));
      }
    }
    return new JsonnetArrayLiteralNode(elementNodes.toArray(new JsonnetExpressionNode[0]));
  }

  @Override
  public JsonnetExpressionNode visitArrayComp(JsonnetParser.ArrayCompContext ctx) {
    Map<String, Integer> savedSlots = new HashMap<>(variableSlots);
    List<Integer> slots = new ArrayList<>();
    List<JsonnetExpressionNode> listNodes = new ArrayList<>();
    List<JsonnetExpressionNode> filterNodes = new ArrayList<>();

    // Visit source expression BEFORE adding comprehension variable to scope
    // so that `local x = 10; [x for x in [x, x, x]]` uses outer x for source
    listNodes.add(visit(ctx.expr(1)));

    String id = ctx.id().getText();
    int slot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, id, null);
    variableSlots.put(id, slot);
    slots.add(slot);

    for (JsonnetParser.CompspecContext comp : ctx.compspec()) {
      if (comp.FOR() != null) {
        // Visit source for nested for-clause before adding its variable
        listNodes.add(visit(comp.expr()));
        String innerId = comp.id().getText();
        int innerSlot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, innerId, null);
        variableSlots.put(innerId, innerSlot);
        slots.add(innerSlot);
      } else if (comp.IF() != null) {
        filterNodes.add(visit(comp.expr()));
      }
    }

    JsonnetExpressionNode resultExpr = visit(ctx.expr(0));
    JsonnetExpressionNode filterExpr = filterNodes.isEmpty() ? null : filterNodes.get(0);

    variableSlots.clear();
    variableSlots.putAll(savedSlots);

    return new JsonnetArrayComprehensionNode(
        resultExpr,
        slots.stream().mapToInt(i -> i).toArray(),
        listNodes.toArray(new JsonnetExpressionNode[0]),
        filterExpr);
  }

  // --- Objects ---

  @Override
  public JsonnetExpressionNode visitObject(JsonnetParser.ObjectContext ctx) {
    if (ctx.objinside() == null) {
      return new JsonnetObjectLiteralNode(new String[0], new JsonnetExpressionNode[0]);
    }

    JsonnetParser.ObjinsideContext inside = ctx.objinside();

    if (inside.member() == null || inside.member().isEmpty()) {
      if (inside.id() != null) {
        return visitObjInsideComprehension(inside);
      }
      return visitComputedFieldObject(inside);
    }

    return visitObjInsideMembers(inside);
  }

  /** Parse the h rule to determine field visibility. */
  private static JObject.Visibility parseVisibility(JsonnetParser.HContext h) {
    if (h == null) return JObject.Visibility.NORMAL;
    int colonCount = h.getChildCount();
    return switch (colonCount) {
      case 3 -> JObject.Visibility.FORCED;
      case 2 -> JObject.Visibility.HIDDEN;
      default -> JObject.Visibility.NORMAL;
    };
  }

  private JsonnetExpressionNode visitObjInsideMembers(JsonnetParser.ObjinsideContext inside) {
    List<String> staticNames = new ArrayList<>();
    List<JsonnetExpressionNode> fieldNameNodes = new ArrayList<>();
    List<JsonnetExpressionNode> valueNodes = new ArrayList<>();
    List<JObject.Visibility> visibilities = new ArrayList<>();
    List<Integer> computedPlusKeySlots = new ArrayList<>();
    List<String> localNames = new ArrayList<>();
    List<JsonnetExpressionNode> localValueNodes = new ArrayList<>();
    List<JsonnetExpressionNode> localAssignments = new ArrayList<>();

    // Save and set up object-local scope
    Set<String> savedObjectLocalNames = new HashSet<>(objectLocalNames);

    // First pass: collect object-local names so field bodies can reference them
    Set<String> thisObjectLocals = new HashSet<>();
    for (JsonnetParser.MemberContext member : inside.member()) {
      if (member.objlocal() != null) {
        JsonnetParser.BindContext bind = member.objlocal().bind();
        String name = bind.id().getText();
        if (thisObjectLocals.contains(name)) {
          throw new JsonnetStaticError("Expected no duplicate local: " + name);
        }
        thisObjectLocals.add(name);
        objectLocalNames.add(name);
      }
    }

    // Collect assert expressions
    List<JsonnetExpressionNode> assertConditions = new ArrayList<>();
    List<JsonnetExpressionNode> assertMessages = new ArrayList<>();

    Set<String> seenStaticFieldNames = new HashSet<>();

    // Second pass: process fields, locals, and asserts
    for (JsonnetParser.MemberContext member : inside.member()) {
      if (member.field() != null) {
        JsonnetParser.FieldContext field = member.field();
        JsonnetParser.FieldnameContext fieldname = field.fieldname();

        String fieldKey = null;
        if (fieldname.expr() != null) {
          // Computed field keys don't see the current object's locals —
          // only the enclosing scope is visible for key expressions
          Set<String> withLocals = new HashSet<>(objectLocalNames);
          objectLocalNames.clear();
          objectLocalNames.addAll(savedObjectLocalNames);
          staticNames.add(null);
          fieldNameNodes.add(visit(fieldname.expr()));
          objectLocalNames.clear();
          objectLocalNames.addAll(withLocals);
        } else {
          fieldKey = fieldname.getText();
          if (fieldKey.startsWith("\"") || fieldKey.startsWith("'") || fieldKey.startsWith("@")) {
            fieldKey = processStringLiteral(fieldKey);
          }
          if (!seenStaticFieldNames.add(fieldKey)) {
            throw new JsonnetStaticError("Expected no duplicate field: " + fieldKey);
          }
          staticNames.add(fieldKey);
          fieldNameNodes.add(null);
        }

        // Parse field visibility from h rule
        visibilities.add(parseVisibility(field.h()));

        // Check for +: (field override — compiles as super.field + expr)
        boolean hasPlus = false;
        for (int i = 0; i < field.getChildCount(); i++) {
          if ("+".equals(field.getChild(i).getText())) {
            hasPlus = true;
            break;
          }
        }

        // Handle function-style field: fieldname(params) : expr
        // Note: params? can be null for zero-arg methods like foo():: expr
        if (isFunctionField(field)) {
          valueNodes.add(buildFunctionBody(field.params(), field.expr(), "field_func"));
          computedPlusKeySlots.add(-1);
        } else if (hasPlus) {
          // +: means value = if field in super then super.field + val else val
          JsonnetExpressionNode valExpr = visit(field.expr());
          if (fieldKey != null) {
            valueNodes.add(new JsonnetFieldPlusNode(fieldKey, valExpr));
            computedPlusKeySlots.add(-1);
          } else {
            int slot =
                frameDescriptorBuilder.addSlot(
                    FrameSlotKind.Object, "$computedPlusKey$" + staticNames.size(), null);
            computedPlusKeySlots.add(slot);
            valueNodes.add(new JsonnetFieldPlusNode(slot, valExpr));
          }
        } else {
          computedPlusKeySlots.add(-1);
          valueNodes.add(visit(field.expr()));
        }
      } else if (member.objlocal() != null) {
        JsonnetParser.BindContext bind = member.objlocal().bind();
        String name = bind.id().getText();
        localNames.add(name);

        if (isFunctionBind(bind)) {
          localValueNodes.add(buildFunctionBody(bind.params(), bind.expr(), name));
        } else {
          localValueNodes.add(visit(bind.expr()));
        }
      } else if (member.assertStmt() != null) {
        // Object assert: assert expr (: msg)?
        JsonnetParser.AssertStmtContext assertCtx = member.assertStmt();
        assertConditions.add(visit(assertCtx.expr(0)));
        if (assertCtx.expr().size() > 1) {
          assertMessages.add(visit(assertCtx.expr(1)));
        } else {
          assertMessages.add(null);
        }
      }
    }

    objectLocalNames.clear();
    objectLocalNames.addAll(savedObjectLocalNames);

    return new JsonnetObjectLiteralNode(
        staticNames.toArray(new String[0]),
        fieldNameNodes.toArray(new JsonnetExpressionNode[0]),
        valueNodes.toArray(new JsonnetExpressionNode[0]),
        visibilities.toArray(new JObject.Visibility[0]),
        computedPlusKeySlots.stream().mapToInt(Integer::intValue).toArray(),
        localAssignments.toArray(new JsonnetExpressionNode[0]),
        localNames.toArray(new String[0]),
        localValueNodes.toArray(new JsonnetExpressionNode[0]),
        assertConditions.toArray(new JsonnetExpressionNode[0]),
        assertMessages.toArray(new JsonnetExpressionNode[0]));
  }

  private JsonnetExpressionNode visitObjInsideComprehension(JsonnetParser.ObjinsideContext inside) {
    Map<String, Integer> savedSlots = new HashMap<>(variableSlots);
    Set<String> savedObjectLocalNames = new HashSet<>(objectLocalNames);

    // Register object-local names first (so they can reference each other),
    // but defer visiting their value expressions until loop variables are in scope.
    List<String> localNames = new ArrayList<>();
    if (inside.objlocal() != null) {
      for (JsonnetParser.ObjlocalContext objlocal : inside.objlocal()) {
        String name = objlocal.bind().id().getText();
        localNames.add(name);
        objectLocalNames.add(name);
      }
    }

    // Collect all loop variables and list expressions (primary for + compspec fors)
    List<Integer> slots = new ArrayList<>();
    List<JsonnetExpressionNode> listNodes = new ArrayList<>();

    // Visit primary list expression BEFORE adding loop variable to scope
    listNodes.add(visit(inside.expr(2)));

    String loopVar = inside.id().getText();
    int loopSlot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, loopVar, null);
    variableSlots.put(loopVar, loopSlot);
    slots.add(loopSlot);

    // Handle additional compspecs (for/if clauses)
    List<JsonnetExpressionNode> filterNodes = new ArrayList<>();
    if (inside.compspec() != null) {
      for (JsonnetParser.CompspecContext comp : inside.compspec()) {
        if (comp.FOR() != null) {
          listNodes.add(visit(comp.expr()));
          String innerId = comp.id().getText();
          int innerSlot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, innerId, null);
          variableSlots.put(innerId, innerSlot);
          slots.add(innerSlot);
        } else if (comp.IF() != null) {
          filterNodes.add(visit(comp.expr()));
        }
      }
    }

    // NOW visit objlocal value expressions — loop variables are in scope
    List<JsonnetExpressionNode> localValueNodes = new ArrayList<>();
    if (inside.objlocal() != null) {
      for (JsonnetParser.ObjlocalContext objlocal : inside.objlocal()) {
        JsonnetParser.BindContext bind = objlocal.bind();
        JsonnetExpressionNode valueNode;
        if (isFunctionBind(bind)) {
          valueNode = buildFunctionBody(bind.params(), bind.expr(), bind.id().getText());
        } else {
          valueNode = visit(bind.expr());
        }
        localValueNodes.add(valueNode);
      }
    }

    // Detect +: in comprehension (the '+' token appears before ':')
    boolean hasPlus = false;
    for (int i = 0; i < inside.getChildCount(); i++) {
      String text = inside.getChild(i).getText();
      if ("+".equals(text)) {
        hasPlus = true;
        break;
      }
      if (":".equals(text)) break;
    }

    // Key expression does NOT see current object's locals
    Set<String> withLocals = new HashSet<>(objectLocalNames);
    objectLocalNames.clear();
    objectLocalNames.addAll(savedObjectLocalNames);
    JsonnetExpressionNode keyExpr = visit(inside.expr(0));
    objectLocalNames.clear();
    objectLocalNames.addAll(withLocals);

    // Value expression DOES see object-locals (they shadow loop variables
    // because visitVar checks objectLocalNames before variableSlots)
    JsonnetExpressionNode valueExpr = visit(inside.expr(1));

    JsonnetExpressionNode filterExpr = filterNodes.isEmpty() ? null : filterNodes.get(0);

    variableSlots.clear();
    variableSlots.putAll(savedSlots);
    objectLocalNames.clear();
    objectLocalNames.addAll(savedObjectLocalNames);

    return new JsonnetObjectComprehensionNode(
        keyExpr,
        valueExpr,
        slots.stream().mapToInt(i -> i).toArray(),
        listNodes.toArray(new JsonnetExpressionNode[0]),
        filterExpr,
        hasPlus,
        localNames.toArray(new String[0]),
        localValueNodes.toArray(new JsonnetExpressionNode[0]));
  }

  /**
   * Handle alt 2 of objinside WITHOUT a for clause: { (objlocal,)* [keyExpr]: valExpr } This is a
   * single computed field with optional pre-field locals.
   */
  private JsonnetExpressionNode visitComputedFieldObject(JsonnetParser.ObjinsideContext inside) {
    Map<String, Integer> savedSlots = new HashMap<>(variableSlots);
    Set<String> savedObjectLocalNames = new HashSet<>(objectLocalNames);

    List<String> localNames = new ArrayList<>();
    List<JsonnetExpressionNode> localValueNodes = new ArrayList<>();

    if (inside.objlocal() != null) {
      Set<String> localNameSet = new HashSet<>();
      for (JsonnetParser.ObjlocalContext objlocal : inside.objlocal()) {
        String name = objlocal.bind().id().getText();
        localNameSet.add(name);
        objectLocalNames.add(name);
      }

      for (JsonnetParser.ObjlocalContext objlocal : inside.objlocal()) {
        JsonnetParser.BindContext bind = objlocal.bind();
        String name = bind.id().getText();
        localNames.add(name);
        JsonnetExpressionNode valueNode;
        if (isFunctionBind(bind)) {
          valueNode = buildFunctionBody(bind.params(), bind.expr(), name);
        } else {
          valueNode = visit(bind.expr());
        }
        localValueNodes.add(valueNode);
      }
    }

    // Computed keys don't see current object's locals — only enclosing scope
    Set<String> fullObjectLocalNames = new HashSet<>(objectLocalNames);
    objectLocalNames.clear();
    objectLocalNames.addAll(savedObjectLocalNames);
    JsonnetExpressionNode keyExpr = visit(inside.expr(0));
    objectLocalNames.clear();
    objectLocalNames.addAll(fullObjectLocalNames);

    JsonnetExpressionNode valueExpr = visit(inside.expr(1));

    variableSlots.clear();
    variableSlots.putAll(savedSlots);
    objectLocalNames.clear();
    objectLocalNames.addAll(savedObjectLocalNames);

    return new JsonnetComputedFieldObjectNode(
        keyExpr,
        valueExpr,
        localNames.toArray(new String[0]),
        localValueNodes.toArray(new JsonnetExpressionNode[0]));
  }

  // --- Object Apply ---

  @Override
  public JsonnetExpressionNode visitApply(JsonnetParser.ApplyContext ctx) {
    JsonnetExpressionNode target = visit(ctx.expr());
    JsonnetExpressionNode objectLiteral;

    if (ctx.objinside() == null) {
      objectLiteral = new JsonnetObjectLiteralNode(new String[0], new JsonnetExpressionNode[0]);
    } else {
      JsonnetParser.ObjinsideContext inside = ctx.objinside();
      if (inside.member() == null || inside.member().isEmpty()) {
        if (inside.id() != null) {
          objectLiteral = visitObjInsideComprehension(inside);
        } else {
          objectLiteral = visitComputedFieldObject(inside);
        }
      } else {
        objectLiteral = visitObjInsideMembers(inside);
      }
    }

    return JsonnetAddNodeGen.create(target, objectLiteral);
  }

  // --- Control Flow ---

  @Override
  public JsonnetExpressionNode visitAssert(JsonnetParser.AssertContext ctx) {
    // assert expr (: msg)? ; body
    // Evaluates condition, throws if false, then evaluates and returns body
    JsonnetExpressionNode condition = visit(ctx.expr(0));
    JsonnetExpressionNode body;
    JsonnetExpressionNode message;
    if (ctx.expr().size() == 3) {
      // assert cond : msg ; body
      message = visit(ctx.expr(1));
      body = visit(ctx.expr(2));
    } else {
      // assert cond ; body
      message = null;
      body = visit(ctx.expr(1));
    }
    return new JsonnetAssertNode(condition, message, body);
  }

  @Override
  public JsonnetExpressionNode visitIfElse(JsonnetParser.IfElseContext ctx) {
    JsonnetExpressionNode condition = visit(ctx.expr(0));
    JsonnetExpressionNode thenExpr = visit(ctx.expr(1));
    JsonnetExpressionNode elseExpr =
        ctx.expr().size() > 2 ? visit(ctx.expr(2)) : new JsonnetNullLiteralNode();
    return new JsonnetIfElseNode(condition, thenExpr, elseExpr);
  }

  // --- Error ---

  @Override
  public JsonnetExpressionNode visitError(JsonnetParser.ErrorContext ctx) {
    JsonnetExpressionNode message = visit(ctx.expr());
    return new JsonnetErrorNode(message);
  }

  // --- Binary Operators ---

  @Override
  public JsonnetExpressionNode visitAdditive(JsonnetParser.AdditiveContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    String op = ctx.getChild(1).getText();
    return switch (op) {
      case "+" -> JsonnetAddNodeGen.create(left, right);
      case "-" -> JsonnetSubtractNodeGen.create(left, right);
      default -> throw new JsonnetInternalError("Additive operator " + op + " not implemented");
    };
  }

  @Override
  public JsonnetExpressionNode visitMultiplicative(JsonnetParser.MultiplicativeContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    String op = ctx.getChild(1).getText();
    return switch (op) {
      case "*" -> JsonnetMultiplyNodeGen.create(left, right);
      case "/" -> JsonnetDivideNodeGen.create(left, right);
      case "%" -> JsonnetModuloNodeGen.create(left, right);
      default ->
          throw new JsonnetInternalError("Multiplicative operator " + op + " not implemented");
    };
  }

  @Override
  public JsonnetExpressionNode visitRelational(JsonnetParser.RelationalContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    String op = ctx.getChild(1).getText();
    return switch (op) {
      case "<" -> JsonnetLessThanNodeGen.create(left, right);
      case ">" -> JsonnetGreaterThanNodeGen.create(left, right);
      case "<=" -> JsonnetLessEqualNodeGen.create(left, right);
      case ">=" -> JsonnetGreaterEqualNodeGen.create(left, right);
      case "in" -> JsonnetInNodeGen.create(left, right);
      default -> throw new JsonnetInternalError("Relational operator " + op + " not implemented");
    };
  }

  @Override
  public JsonnetExpressionNode visitEquality(JsonnetParser.EqualityContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    String op = ctx.getChild(1).getText();
    return switch (op) {
      case "==" -> JsonnetEqualNodeGen.create(left, right);
      case "!=" -> JsonnetNotEqualNodeGen.create(left, right);
      default -> throw new JsonnetInternalError("Equality operator " + op + " not implemented");
    };
  }

  @Override
  public JsonnetExpressionNode visitLogicalAnd(JsonnetParser.LogicalAndContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    return new JsonnetLogicalAndNode(left, right);
  }

  @Override
  public JsonnetExpressionNode visitLogicalOr(JsonnetParser.LogicalOrContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    return new JsonnetLogicalOrNode(left, right);
  }

  @Override
  public JsonnetExpressionNode visitShift(JsonnetParser.ShiftContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    String op = ctx.getChild(1).getText();
    return switch (op) {
      case "<<" -> JsonnetShiftLeftNodeGen.create(left, right);
      case ">>" -> JsonnetShiftRightNodeGen.create(left, right);
      default -> throw new JsonnetInternalError("Shift operator " + op + " not implemented");
    };
  }

  @Override
  public JsonnetExpressionNode visitBitwiseAnd(JsonnetParser.BitwiseAndContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    return JsonnetBitwiseAndNodeGen.create(left, right);
  }

  @Override
  public JsonnetExpressionNode visitBitwiseXor(JsonnetParser.BitwiseXorContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    return JsonnetBitwiseXorNodeGen.create(left, right);
  }

  @Override
  public JsonnetExpressionNode visitBitwiseOr(JsonnetParser.BitwiseOrContext ctx) {
    JsonnetExpressionNode left = visit(ctx.expr(0));
    JsonnetExpressionNode right = visit(ctx.expr(1));
    return JsonnetBitwiseOrNodeGen.create(left, right);
  }

  // --- Unary Operators ---

  @Override
  public JsonnetExpressionNode visitUnaryMinus(JsonnetParser.UnaryMinusContext ctx) {
    JsonnetExpressionNode operand = visit(ctx.expr());
    return JsonnetUnaryMinusNodeGen.create(operand);
  }

  @Override
  public JsonnetExpressionNode visitUnaryPlus(JsonnetParser.UnaryPlusContext ctx) {
    JsonnetExpressionNode operand = visit(ctx.expr());
    return JsonnetUnaryPlusNodeGen.create(operand);
  }

  @Override
  public JsonnetExpressionNode visitUnaryNot(JsonnetParser.UnaryNotContext ctx) {
    JsonnetExpressionNode operand = visit(ctx.expr());
    return JsonnetUnaryNotNodeGen.create(operand);
  }

  @Override
  public JsonnetExpressionNode visitUnaryBitNot(JsonnetParser.UnaryBitNotContext ctx) {
    JsonnetExpressionNode operand = visit(ctx.expr());
    return JsonnetUnaryBitNotNodeGen.create(operand);
  }

  // --- Parentheses ---

  @Override
  public JsonnetExpressionNode visitParen(JsonnetParser.ParenContext ctx) {
    return visit(ctx.expr());
  }

  // --- Imports ---

  @Override
  public JsonnetExpressionNode visitImport(JsonnetParser.ImportContext ctx) {
    String importPath = processStringLiteral(ctx.STRING().getText());
    return new JsonnetImportNode(importPath, sourceName, language);
  }

  @Override
  public JsonnetExpressionNode visitImportStr(JsonnetParser.ImportStrContext ctx) {
    String importPath = processStringLiteral(ctx.STRING().getText());
    return new JsonnetImportStrNode(importPath, sourceName);
  }

  // --- Anonymous Functions ---

  @Override
  public JsonnetExpressionNode visitFunction(JsonnetParser.FunctionContext ctx) {
    return buildFunctionBody(ctx.params(), ctx.expr(), "anonymous");
  }

  // --- Helper: StdObjectNode ---

  /**
   * A node that returns the std library object when executed. Used to inject std into function body
   * frames.
   */
  static class StdObjectNode extends JsonnetExpressionNode {
    private final JsonnetLanguage language;

    StdObjectNode(JsonnetLanguage language) {
      this.language = language;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
      return language.getStdLibrary();
    }
  }
}

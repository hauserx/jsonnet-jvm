package jsonnetjvm.truffle.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import jsonnetjvm.JsonnetBaseVisitor;
import jsonnetjvm.JsonnetParser;
import jsonnetjvm.runtime.JFunction;
import jsonnetjvm.truffle.JsonnetLanguage;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import jsonnetjvm.truffle.nodes.JsonnetRootNode;
import jsonnetjvm.truffle.nodes.expressions.*;
import jsonnetjvm.truffle.nodes.literals.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonnetTruffleASTBuilder extends JsonnetBaseVisitor<JsonnetExpressionNode> {

    private final JsonnetLanguage language;
    private final FrameDescriptor.Builder frameDescriptorBuilder = FrameDescriptor.newBuilder();
    private final Map<String, Integer> variableSlots = new HashMap<>();

    public JsonnetTruffleASTBuilder(JsonnetLanguage language) {
        this.language = language;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptorBuilder.build();
    }

    @Override
    public JsonnetExpressionNode visitJsonnet(JsonnetParser.JsonnetContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public JsonnetExpressionNode visitNumberLit(JsonnetParser.NumberLitContext ctx) {
        return new JsonnetNumberLiteralNode(Double.parseDouble(ctx.getText()));
    }

    @Override
    public JsonnetExpressionNode visitStringLit(JsonnetParser.StringLitContext ctx) {
        String text = ctx.getText();
        String value = text.substring(1, text.length() - 1);
        return new JsonnetStringLiteralNode(value);
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

    @Override
    public JsonnetExpressionNode visitLocalVar(JsonnetParser.LocalVarContext ctx) {
        Map<String, Integer> savedSlots = new HashMap<>(variableSlots);

        JsonnetExpressionNode[] assignments = new JsonnetExpressionNode[ctx.bind().size()];
        for (int i = 0; i < ctx.bind().size(); i++) {
            JsonnetParser.BindContext bind = ctx.bind(i);
            String name = bind.id().getText();
            int slot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, name, null);
            variableSlots.put(name, slot);

            if (bind.params() != null) {
                assignments[i] = new JsonnetWriteLocalVariableNode(slot, buildFunction(bind));
            } else {
                assignments[i] = new JsonnetWriteLocalVariableNode(slot, visit(bind.expr()));
            }
        }
        JsonnetExpressionNode body = visit(ctx.expr());

        variableSlots.clear();
        variableSlots.putAll(savedSlots);

        return new JsonnetLocalBlockNode(assignments, body);
    }

    private JsonnetExpressionNode buildFunction(JsonnetParser.BindContext bind) {
        String funcName = bind.id().getText();
        JsonnetTruffleASTBuilder bodyBuilder = new JsonnetTruffleASTBuilder(language);

        List<JsonnetExpressionNode> argAssignments = new ArrayList<>();
        if (bind.params() != null) {
            int argIndex = 0;
            for (JsonnetParser.ParamContext param : bind.params().param()) {
                String paramName = param.id().getText();
                int slot = bodyBuilder.frameDescriptorBuilder.addSlot(FrameSlotKind.Object, paramName, null);
                bodyBuilder.variableSlots.put(paramName, slot);

                final int currentArgIndex = argIndex;
                JsonnetExpressionNode defaultValueNode = param.expr() != null
                        ? bodyBuilder.visit(param.expr())
                        : new JsonnetNullLiteralNode();

                JsonnetExpressionNode paramValueNode = new JsonnetIfElseNode(new JsonnetExpressionNode() {
                    @Override
                    public Object executeGeneric(com.oracle.truffle.api.frame.VirtualFrame frame) {
                        return frame.getArguments().length > currentArgIndex;
                    }
                    @Override
                    public boolean executeBoolean(com.oracle.truffle.api.frame.VirtualFrame frame) {
                        return frame.getArguments().length > currentArgIndex;
                    }
                }, new JsonnetArgReadNode(currentArgIndex), defaultValueNode);

                argAssignments.add(new JsonnetWriteLocalVariableNode(slot, paramValueNode));
                argIndex++;
            }
        }

        bodyBuilder.variableSlots.putAll(this.variableSlots);

        JsonnetExpressionNode body = bodyBuilder.visit(bind.expr());
        JsonnetExpressionNode fullBody = new JsonnetLocalBlockNode(argAssignments.toArray(new JsonnetExpressionNode[0]),
                body);

        JsonnetRootNode rootNode = new JsonnetRootNode(language, bodyBuilder.getFrameDescriptor(), fullBody);

        return new JsonnetFunctionLiteralNode(funcName, rootNode.getCallTarget());
    }

    @Override
    public JsonnetExpressionNode visitVar(JsonnetParser.VarContext ctx) {
        String name = ctx.getText();
        if ("std".equals(name)) {
            return null;
        }
        Integer slot = variableSlots.get(name);
        if (slot == null) {
            throw new RuntimeException("Undefined variable: " + name);
        }
        return new JsonnetReadLocalVariableNode(slot, name);
    }

    @Override
    public JsonnetExpressionNode visitRelational(JsonnetParser.RelationalContext ctx) {
        JsonnetExpressionNode left = visit(ctx.expr(0));
        JsonnetExpressionNode right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        if ("<".equals(op)) {
            return JsonnetLessThanNodeGen.create(left, right);
        }
        throw new UnsupportedOperationException("Relational operator " + op + " not implemented");
    }

    @Override
    public JsonnetExpressionNode visitParen(JsonnetParser.ParenContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public JsonnetExpressionNode visitCall(JsonnetParser.CallContext ctx) {
        List<JsonnetExpressionNode> args = new ArrayList<>();
        if (ctx.args() != null) {
            for (JsonnetParser.ExprContext arg : ctx.args().expr()) {
                args.add(visit(arg));
            }
        }

        String callTarget = ctx.expr().getText();
        if ("std.range".equals(callTarget)) {
            return JsonnetStdRangeNodeGen.create(args.get(0), args.get(1));
        } else if ("std.length".equals(callTarget)) {
            return JsonnetStdLengthNodeGen.create(args.get(0));
        }

        JsonnetExpressionNode funcNode = visit(ctx.expr());
        if (funcNode == null) {
            throw new RuntimeException("Cannot call undefined or null: " + callTarget);
        }

        return new JsonnetCallNode(funcNode, args.toArray(new JsonnetExpressionNode[0]));
    }

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

        String id = ctx.id().getText();
        int slot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, id, null);
        variableSlots.put(id, slot);
        slots.add(slot);
        listNodes.add(visit(ctx.expr(1)));

        for (JsonnetParser.CompspecContext comp : ctx.compspec()) {
            if (comp.FOR() != null) {
                String innerId = comp.id().getText();
                int innerSlot = frameDescriptorBuilder.addSlot(FrameSlotKind.Object, innerId, null);
                variableSlots.put(innerId, innerSlot);
                slots.add(innerSlot);
                listNodes.add(visit(comp.expr()));
            }
        }

        JsonnetExpressionNode resultExpr = visit(ctx.expr(0));
        variableSlots.clear();
        variableSlots.putAll(savedSlots);

        return new JsonnetArrayComprehensionNode(resultExpr, slots.stream().mapToInt(i -> i).toArray(),
                listNodes.toArray(new JsonnetExpressionNode[0]));
    }

    @Override
    public JsonnetExpressionNode visitIfElse(JsonnetParser.IfElseContext ctx) {
        JsonnetExpressionNode condition = visit(ctx.expr(0));
        JsonnetExpressionNode thenExpr = visit(ctx.expr(1));
        JsonnetExpressionNode elseExpr = ctx.expr().size() > 2 ? visit(ctx.expr(2)) : new JsonnetNullLiteralNode();
        return new JsonnetIfElseNode(condition, thenExpr, elseExpr);
    }

    @Override
    public JsonnetExpressionNode visitObject(JsonnetParser.ObjectContext ctx) {
        if (ctx.objinside() == null) {
            return new JsonnetObjectLiteralNode(new String[0], new JsonnetExpressionNode[0]);
        }

        JsonnetParser.ObjinsideContext inside = ctx.objinside();
        List<String> fieldNames = new ArrayList<>();
        List<JsonnetExpressionNode> valueNodes = new ArrayList<>();

        if (inside.member() != null) {
            for (JsonnetParser.MemberContext member : inside.member()) {
                if (member.field() != null) {
                    JsonnetParser.FieldContext field = member.field();
                    String key = field.fieldname().getText();
                    if (key.startsWith("\"") || key.startsWith("'")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    fieldNames.add(key);
                    valueNodes.add(visit(field.expr()));
                }
            }
        }

        return new JsonnetObjectLiteralNode(fieldNames.toArray(new String[0]),
                valueNodes.toArray(new JsonnetExpressionNode[0]));
    }

    @Override
    public JsonnetExpressionNode visitAdditive(JsonnetParser.AdditiveContext ctx) {
        JsonnetExpressionNode left = visit(ctx.expr(0));
        JsonnetExpressionNode right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        if ("+".equals(op)) {
            return JsonnetAddNodeGen.create(left, right);
        }
        throw new UnsupportedOperationException("Additive operator " + op + " not implemented");
    }
}
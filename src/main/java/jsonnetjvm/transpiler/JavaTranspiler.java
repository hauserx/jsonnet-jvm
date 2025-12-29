package jsonnetjvm.transpiler;

import jsonnetjvm.JsonnetBaseVisitor;
import jsonnetjvm.JsonnetParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.stream.Collectors;

public class JavaTranspiler extends JsonnetBaseVisitor<String> {

    private final String className;
    private final StringBuilder methods = new StringBuilder();
    private final java.util.Set<String> generatedMethods = new java.util.HashSet<>();
    private int varCounter = 0;

    public JavaTranspiler(String className) {
        this.className = className;
    }

    public String getJavaCode() {
        return methods.toString();
    }

    private String generateVarName() {
        return "v" + varCounter++;
    }

    @Override
    public String visitJsonnet(JsonnetParser.JsonnetContext ctx) {
        String mainBody = visit(ctx.expr());

        return "import jsonnetjvm.runtime.*;\n" + "import java.util.function.Supplier;\n"
                + "import java.util.ArrayList;\n" + "import java.util.List;\n\n" + "public class " + className
                + " implements Supplier<Val> {\n\n" + methods.toString() + "    @Override\n"
                + "    public Val get() {\n" + "        return " + mainBody + ";\n" + "    }\n" + "}\n";
    }

    @Override
    public String visitLocalVar(JsonnetParser.LocalVarContext ctx) {
        StringBuilder code = new StringBuilder();
        code.append("((Supplier<Val>) () -> {\n");

        for (JsonnetParser.BindContext bind : ctx.bind()) {
            String idName = bind.id().getText();
            if (bind.params() != null) { // Function definition: local Person(name) = { ... }
                generateMethod(bind); // This generates a static method.
                // The transpiler's visitVar will resolve 'Person' to 'Person()' when called.
            } else { // Variable definition: local arr = [ ... ]
                code.append("            final Val ").append(idName).append(" = ").append(visit(bind.expr()))
                        .append(";\n");
            }
        }

        // Final expression of the local block
        code.append("            return ").append(visit(ctx.expr())).append(";\n");
        code.append("        }).get()");
        return code.toString();
    }

    private void generateMethod(JsonnetParser.BindContext bind) {
        String methodName = bind.id().getText();
        generatedMethods.add(methodName);

        StringBuilder params = new StringBuilder();
        StringBuilder bodyPreamble = new StringBuilder();

        if (bind.params() != null) {
            for (JsonnetParser.ParamContext param : bind.params().param()) {
                if (params.length() > 0)
                    params.append(", ");
                String paramName = param.id().getText();
                String rawParamName = "_" + paramName;
                params.append("Val ").append(rawParamName);

                bodyPreamble.append("        final Val ").append(paramName).append(" = (").append(rawParamName)
                        .append(" != null) ? ").append(rawParamName).append(" : ");

                if (param.expr() != null) {
                    String defaultExpr = visit(param.expr());
                    bodyPreamble.append(defaultExpr);
                } else {
                    bodyPreamble.append("JNull.INSTANCE"); // Default for missing non-defaulted is JNull
                }
                bodyPreamble.append(";\n");
            }
        }

        String body = visit(bind.expr());

        methods.append("    public static Val ").append(methodName).append("(").append(params).append(") {\n")
                .append(bodyPreamble).append("        return ").append(body).append(";\n").append("    }\n\n");
    }

    @Override
    public String visitObject(JsonnetParser.ObjectContext ctx) {
        if (ctx.objinside() == null) {
            return "new JObject()";
        }

        String varName = generateVarName();
        StringBuilder code = new StringBuilder();
        code.append("((Supplier<Val>) () -> {\n");
        code.append("            JObject ").append(varName).append(" = new JObject();\n");

        JsonnetParser.ObjinsideContext inside = ctx.objinside();

        if (inside.member() != null) {
            for (JsonnetParser.MemberContext member : inside.member()) {
                if (member.field() != null) {
                    JsonnetParser.FieldContext field = member.field();
                    String key = field.fieldname().getText();
                    if (key.startsWith("\"") || key.startsWith("'")) {
                        key = key.substring(1, key.length() - 1);
                    }

                    String valueExpr = visit(field.expr());
                    code.append("            ").append(varName).append(".addField(\"" + key + "\", () -> ")
                            .append(valueExpr).append(");\n");
                }
            }
        }

        code.append("            return ").append(varName).append(";\n");
        code.append("        }).get()");

        return code.toString();
    }

    @Override
    public String visitArrayComp(JsonnetParser.ArrayCompContext ctx) {
        // '[' expr ','? 'for' id 'in' expr compspec* ']'

        StringBuilder code = new StringBuilder();
        code.append("((Supplier<Val>) () -> {\n");
        code.append("            List<Val> results = new ArrayList<>();\n");

        // Initial for loop
        String id = ctx.id().getText();
        String listExpr = visit(ctx.expr(1)); // 0 is result expr, 1 is list expr

        code.append("            for (Val ").append(id).append(" : ((JArray)").append(listExpr)
                .append(").getItems()) {\n");

        // Nested compspecs
        int depth = 1;
        for (JsonnetParser.CompspecContext comp : ctx.compspec()) {
            if (comp.FOR() != null) {
                String innerId = comp.id().getText();
                String innerExpr = visit(comp.expr());
                code.append("                for (Val ").append(innerId).append(" : ((JArray)").append(innerExpr)
                        .append(").getItems()) {\n");
                depth++;
            } else if (comp.IF() != null) {
                String cond = visit(comp.expr());
                code.append("                if (").append(cond).append(".asBoolean()) {\n");
                depth++;
            }
        }

        String resultExpr = visit(ctx.expr(0));
        code.append("                    results.add(").append(resultExpr).append(");\n");

        // Close braces
        for (int i = 0; i < depth; i++) {
            code.append("                }\n");
        }

        code.append("            return new JArray(results);\n");
        code.append("        }).get()");

        return code.toString();
    }

    @Override
    public String visitIfElse(JsonnetParser.IfElseContext ctx) {
        String cond = visit(ctx.expr(0));
        String thenExpr = visit(ctx.expr(1));
        String elseExpr = (ctx.expr().size() > 2) ? visit(ctx.expr(2)) : "JNull.INSTANCE";

        return "((" + cond + ").asBoolean() ? " + thenExpr + " : " + elseExpr + ")";
    }

    @Override
    public String visitRelational(JsonnetParser.RelationalContext ctx) {
        String left = visit(ctx.expr(0));
        String right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();
        if ("<".equals(op)) {
            return "new JBoolean(" + left + ".asNumber() < " + right + ".asNumber())";
        }
        return super.visitRelational(ctx);
    }

    @Override
    public String visitFieldAccess(JsonnetParser.FieldAccessContext ctx) {
        String obj = visit(ctx.expr());
        String field = ctx.id().getText();
        if ("std".equals(obj)) {
            return "Std." + field;
        }
        return super.visitFieldAccess(ctx);
    }

    @Override
    public String visitCall(JsonnetParser.CallContext ctx) {
        String funcName = visit(ctx.expr());

        StringBuilder args = new StringBuilder();
        if (ctx.args() != null) {
            for (JsonnetParser.ExprContext argExpr : ctx.args().expr()) {
                if (args.length() > 0)
                    args.append(", ");
                args.append(visit(argExpr));
            }
        }

        // Special case for `std.sum` call which expects a single JArray argument
        if (funcName.endsWith(".sum")) {
            return funcName + "(" + args + ")";
        }

        if (args.length() == 0 && !funcName.contains("Std.")) {
            args.append("null");
        }

        return funcName + "(" + args + ")";
    }

    @Override
    public String visitStringLit(JsonnetParser.StringLitContext ctx) {
        String text = ctx.getText();
        if (text.startsWith("'")) {
            String content = text.substring(1, text.length() - 1);
            content = content.replace("\"", "\\\"");
            return "new JString(\"" + content + "\")";
        }
        return "new JString(" + text + ")";
    }

    @Override
    public String visitNumberLit(JsonnetParser.NumberLitContext ctx) {
        return "new JNumber(" + ctx.getText() + ")";
    }

    @Override
    public String visitVar(JsonnetParser.VarContext ctx) {
        String varName = ctx.getText();
        if (generatedMethods.contains(varName)) {
            return varName + "()"; // It's a method call
        }
        return varName;
    }

    @Override
    public String visitMultiplicative(JsonnetParser.MultiplicativeContext ctx) {
        String left = visit(ctx.expr(0));
        String right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();

        if ("*".equals(op)) {
            return "new JNumber(" + left + ".asNumber() * " + right + ".asNumber())";
        }
        // TODO: Other multiplicative operators
        return super.visitMultiplicative(ctx);
    }

    @Override
    public String visitAdditive(JsonnetParser.AdditiveContext ctx) {
        String left = visit(ctx.expr(0));
        String right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();

        if ("+".equals(op)) {
            return "new JString(" + left + ".asString() + " + right + ".asString())";
        }
        // TODO: Other additive operators
        return super.visitAdditive(ctx);
    }

    @Override
    public String visitParen(JsonnetParser.ParenContext ctx) {
        return visit(ctx.expr());
    }
}
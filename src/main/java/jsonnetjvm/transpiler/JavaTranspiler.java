package jsonnetjvm.transpiler;

import jsonnetjvm.JsonnetBaseVisitor;
import jsonnetjvm.JsonnetParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.stream.Collectors;

public class JavaTranspiler extends JsonnetBaseVisitor<String> {

    private final String className;
    private final StringBuilder methods = new StringBuilder();
    private int varCounter = 0;

    public JavaTranspiler(String className) {
        this.className = className;
    }

    public String getJavaCode() {
        return methods.toString(); // This assumes visit() has been called to populate methods
    }

    private String generateVarName() {
        return "v" + varCounter++;
    }

    @Override

    public String visitJsonnet(JsonnetParser.JsonnetContext ctx) {
        String mainBody = visit(ctx.expr());

        return "import jsonnetjvm.runtime.*;\n" + "import java.util.function.Supplier;\n\n" + "public class "
                + className + " implements Supplier<Val> {\n\n" + methods.toString() + "    @Override\n"
                + "    public Val get() {\n" + "        return " + mainBody + ";\n" + "    }\n" + "}\n";
    }

    @Override
    public String visitLocalVar(JsonnetParser.LocalVarContext ctx) {
        for (JsonnetParser.BindContext bind : ctx.bind()) {
            generateMethod(bind);
        }
        return visit(ctx.expr());
    }

    private void generateMethod(JsonnetParser.BindContext bind) {
        String methodName = bind.id().getText();

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
                    bodyPreamble.append(rawParamName);
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

        for (JsonnetParser.MemberContext member : inside.member()) {
            if (member.field() != null) {
                JsonnetParser.FieldContext field = member.field();
                String key = field.fieldname().getText();
                if (key.startsWith("\"") || key.startsWith("'")) {
                    key = key.substring(1, key.length() - 1);
                }

                String valueExpr = visit(field.expr());
                code.append("            ").append(varName).append(".addField(\"").append(key).append("\", () -> ")
                        .append(valueExpr).append(");\n");
            }
        }

        code.append("            return ").append(varName).append(";\n");
        code.append("        }).get()");

        return code.toString();
    }

    @Override
    public String visitCall(JsonnetParser.CallContext ctx) {
        String funcName = ctx.expr().getText();

        StringBuilder args = new StringBuilder();
        if (ctx.args() != null) {
            for (JsonnetParser.ExprContext argExpr : ctx.args().expr()) {
                if (args.length() > 0)
                    args.append(", ");
                args.append(visit(argExpr));
            }
        }

        if (args.length() == 0) {
            args.append("null");
        }

        return funcName + "(" + args + ")";
    }

    @Override
    public String visitStringLit(JsonnetParser.StringLitContext ctx) {
        String text = ctx.getText();
        if (text.startsWith("'")) {
            // Convert '...' to "..." and escape double quotes inside if needed
            // Simple approach for POC: strip ' and wrap in "
            String content = text.substring(1, text.length() - 1);
            // We need to escape " inside the content if it was unescaped (valid in single
            // quoted jsonnet)
            content = content.replace("\"", "\\\"");
            return "new JString(\"" + content + "\")";
        }
        return "new JString(" + text + ")";
    }

    @Override
    public String visitVar(JsonnetParser.VarContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitAdditive(JsonnetParser.AdditiveContext ctx) {
        String left = visit(ctx.expr(0));
        String right = visit(ctx.expr(1));
        String op = ctx.getChild(1).getText();

        if ("+".equals(op)) {
            return "new JString(" + left + ".asString() + " + right + ".asString())";
        }
        return super.visitAdditive(ctx);
    }
}

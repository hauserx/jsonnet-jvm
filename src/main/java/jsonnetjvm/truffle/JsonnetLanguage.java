package jsonnetjvm.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;
import jsonnetjvm.JsonnetLexer;
import jsonnetjvm.JsonnetParser;
import jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import jsonnetjvm.truffle.parser.JsonnetTruffleASTBuilder;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

@TruffleLanguage.Registration(id = JsonnetLanguage.ID, name = "Jsonnet", version = "0.1", characterMimeTypes = JsonnetLanguage.MIME_TYPE)
public class JsonnetLanguage extends TruffleLanguage<JsonnetContext> {

    public static final String ID = "jsonnet";
    public static final String MIME_TYPE = "application/x-jsonnet";

    @Override
    protected JsonnetContext createContext(Env env) {
        return new JsonnetContext(env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        // Parse with ANTLR (existing parser)
        JsonnetLexer lexer = new JsonnetLexer(CharStreams.fromString(source.getCharacters().toString()));
        JsonnetParser parser = new JsonnetParser(new CommonTokenStream(lexer));
        JsonnetParser.JsonnetContext antlrParseTree = parser.jsonnet();

        // Convert ANTLR parse tree to Truffle AST
        JsonnetTruffleASTBuilder astBuilder = new JsonnetTruffleASTBuilder(this);
        JsonnetExpressionNode truffleAST = astBuilder.visit(antlrParseTree);

        // Create a CallTarget for the Truffle AST
        return truffleAST.asRootNode(this, astBuilder.getFrameDescriptor()).getCallTarget();
    }

    protected Object findExportedSymbol(JsonnetContext context, String globalName, boolean onlyExplicit) {
        return null; // Not supporting global symbols for now
    }
}

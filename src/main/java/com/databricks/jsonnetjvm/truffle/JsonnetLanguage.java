package com.databricks.jsonnetjvm.truffle;

import com.databricks.jsonnetjvm.JsonnetLexer;
import com.databricks.jsonnetjvm.JsonnetParser;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetConfig;
import com.databricks.jsonnetjvm.runtime.JsonnetStaticError;
import com.databricks.jsonnetjvm.runtime.JsonnetThunk;
import com.databricks.jsonnetjvm.stdlib.JsonnetStdLibrary;
import com.databricks.jsonnetjvm.truffle.nodes.JsonnetExpressionNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetLocalBlockNode;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetWriteLocalVariableNode;
import com.databricks.jsonnetjvm.truffle.parser.JsonnetTruffleASTBuilder;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

@TruffleLanguage.Registration(
    id = JsonnetLanguage.ID,
    name = "Jsonnet",
    version = "0.1",
    characterMimeTypes = JsonnetLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.REUSE)
public class JsonnetLanguage extends TruffleLanguage<JsonnetContext> {

  public static final String ID = "jsonnet";
  public static final String MIME_TYPE = "application/x-jsonnet";

  private JObject stdLibrary;

  /**
   * Parse cache: avoids re-lexing, re-parsing, and re-building the Truffle AST for the same source
   * content. Keyed by (sourceName, content) so that the same file imported from different locations
   * hits the cache. With {@code ContextPolicy.REUSE}, this cache persists across Context instances
   * sharing the same Engine, giving cross-evaluation parse reuse.
   */
  private final ConcurrentHashMap<ParseCacheKey, CallTarget> parseCache = new ConcurrentHashMap<>();

  private record ParseCacheKey(String name, String content) {}

  @Override
  protected JsonnetContext createContext(Env env) {
    return new JsonnetContext(env);
  }

  @TruffleBoundary
  public JObject getStdLibrary() {
    if (stdLibrary == null) {
      stdLibrary = JsonnetStdLibrary.create(this);
    }
    return stdLibrary;
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws Exception {
    CallTarget inner = parseSource(request.getSource());
    return new TlaRootNode(this, inner).getCallTarget();
  }

  /**
   * Root node that evaluates the inner program and applies TLA args if the result is a function.
   * TLA vars are read at execution time from the thread-local {@link JsonnetConfig} so that the
   * cached CallTarget works correctly with {@code ContextPolicy.REUSE}.
   */
  private static class TlaRootNode extends RootNode {
    private final CallTarget inner;
    private final JsonnetLanguage lang;

    TlaRootNode(JsonnetLanguage language, CallTarget inner) {
      super(language, FrameDescriptor.newBuilder().build());
      this.lang = language;
      this.inner = inner;
    }

    @Override
    public Object execute(VirtualFrame frame) {
      return doExecute();
    }

    @TruffleBoundary
    private Object doExecute() {
      Object result = inner.call();
      if (!(result instanceof JFunction func)) {
        return result;
      }
      String[] paramNames = func.getParamNames();
      int paramCount = func.getParamCount();
      if (paramNames == null || paramCount <= 0) {
        return func.call();
      }
      Map<String, String> tlaVars = JsonnetConfig.get().getTlaVars();
      Object[] args = new Object[paramCount];
      Arrays.fill(args, JFunction.UNSET_ARG);
      for (int i = 0; i < paramNames.length; i++) {
        String tlaCode = tlaVars.get(paramNames[i]);
        if (tlaCode != null) {
          Source src = Source.newBuilder(ID, tlaCode, "<tla:" + paramNames[i] + ">").build();
          Object val = lang.parseSource(src).call();
          args[i] = new JsonnetThunk(() -> val);
        }
      }
      return func.call(args);
    }
  }

  /**
   * Parse a Jsonnet source file into a CallTarget. Used both for the entry file and for imports.
   *
   * <p>Results are cached by (sourceName, content) so that repeated parses of the same source skip
   * ANTLR lexing/parsing and AST building. With {@code ContextPolicy.REUSE}, this cache persists
   * across Context instances sharing the same Engine.
   */
  @TruffleBoundary
  public CallTarget parseSource(Source source) {
    String code = source.getCharacters().toString();
    String sourcePath = source.getPath();
    String sourceName = sourcePath != null ? sourcePath : source.getName();

    ParseCacheKey key = new ParseCacheKey(sourceName, code);
    CallTarget cached = parseCache.get(key);
    if (cached != null) {
      return cached;
    }

    CallTarget callTarget = doParse(source, code, sourceName);
    parseCache.put(key, callTarget);
    return callTarget;
  }

  private CallTarget doParse(Source source, String code, String sourceName) {
    String displayName = source.getName();

    JsonnetLexer lexer = new JsonnetLexer(CharStreams.fromString(code));
    JsonnetParser parser = new JsonnetParser(new CommonTokenStream(lexer));

    lexer.removeErrorListeners();
    parser.removeErrorListeners();
    List<String> errors = new ArrayList<>();
    BaseErrorListener errorListener =
        new BaseErrorListener() {
          @Override
          public void syntaxError(
              Recognizer<?, ?> recognizer,
              Object offendingSymbol,
              int line,
              int charPositionInLine,
              String msg,
              RecognitionException e) {
            errors.add(
                displayName
                    + ":"
                    + line
                    + ":"
                    + (charPositionInLine + 1)
                    + ": "
                    + simplifyParseError(msg));
          }
        };
    lexer.addErrorListener(errorListener);
    parser.addErrorListener(errorListener);

    JsonnetParser.JsonnetContext antlrParseTree = parser.jsonnet();

    if (!errors.isEmpty()) {
      throw new JsonnetStaticError(errors.get(0));
    }

    JsonnetTruffleASTBuilder astBuilder = new JsonnetTruffleASTBuilder(this, sourceName, source);
    JsonnetExpressionNode truffleAST = astBuilder.visit(antlrParseTree);

    int stdSlot = astBuilder.getStdSlot();
    JObject std = getStdLibrary();

    JsonnetExpressionNode stdInit =
        new JsonnetWriteLocalVariableNode(
            stdSlot,
            new JsonnetExpressionNode() {
              @Override
              public Object executeGeneric(VirtualFrame frame) {
                return std;
              }
            });

    JsonnetExpressionNode wrappedBody =
        new JsonnetLocalBlockNode(new JsonnetExpressionNode[] {stdInit}, truffleAST);

    return wrappedBody
        .asRootNode(this, astBuilder.getFrameDescriptor(), "root", source)
        .getCallTarget();
  }

  /** Clears the parse cache. Useful when source files may have changed on disk. */
  public void clearParseCache() {
    parseCache.clear();
  }

  protected Object findExportedSymbol(
      JsonnetContext context, String globalName, boolean onlyExplicit) {
    return null;
  }

  private static String simplifyParseError(String msg) {
    if (msg.startsWith("token recognition error at: ")) {
      return "Unterminated or invalid string literal";
    }
    if (msg.startsWith("extraneous input ")) {
      String rest = msg.substring("extraneous input ".length());
      int idx = rest.indexOf(" expecting ");
      if (idx >= 0) {
        String token = rest.substring(0, idx);
        String expected = simplifyExpected(rest.substring(idx + " expecting ".length()));
        return "Unexpected " + token + ", expected " + expected;
      }
    }
    if (msg.startsWith("mismatched input ")) {
      String rest = msg.substring("mismatched input ".length());
      int idx = rest.indexOf(" expecting ");
      if (idx >= 0) {
        String token = rest.substring(0, idx);
        String expected = simplifyExpected(rest.substring(idx + " expecting ".length()));
        return "Unexpected " + token + ", expected " + expected;
      }
    }
    if (msg.startsWith("no viable alternative at input ")) {
      return "Unexpected input near " + msg.substring("no viable alternative at input ".length());
    }
    return msg;
  }

  private static final Set<String> CLOSING_TOKENS = Set.of("']'", "'}'", "')'", "':'", "','");

  private static String simplifyExpected(String expected) {
    if (!expected.startsWith("{")) return expected;
    String inner = expected.substring(1, expected.length() - 1).trim();
    String[] tokens = inner.split(",\\s*");
    if (tokens.length <= 3) return expected;
    for (String t : tokens) {
      if (CLOSING_TOKENS.contains(t.trim())) return t.trim();
    }
    boolean hasExpression = false;
    for (String t : tokens) {
      String trimmed = t.trim();
      if ("'if'".equals(trimmed) || "'function'".equals(trimmed) || "NUMBER".equals(trimmed)) {
        hasExpression = true;
        break;
      }
    }
    if (hasExpression) return "expression";
    for (String t : tokens) {
      String trimmed = t.trim();
      if (trimmed.startsWith("'") && trimmed.length() <= 5 && !"'$'".equals(trimmed))
        return trimmed;
    }
    return tokens[0].trim();
  }
}

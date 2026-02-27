package com.databricks.jsonnetjvm;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "jsonnet-jvm",
    mixinStandardHelpOptions = true,
    version = "0.1",
    description = "Jsonnet JVM Interpreter (Truffle-based)")
public class Main implements Callable<Integer> {

  @Parameters(index = "0", description = "The file to execute", arity = "0..1")
  private File file;

  @Option(
      names = {"-e", "--exec"},
      description = "Treat argument as code")
  private String execCode;

  @Option(
      names = {"--ext-str", "-V"},
      description = "<var>=<val> External variable (string)")
  private List<String> extStr = new ArrayList<>();

  @Option(
      names = {"--ext-code"},
      description = "<var>=<code> External variable (Jsonnet code)")
  private List<String> extCode = new ArrayList<>();

  @Option(
      names = {"--ext-str-file"},
      description = "<var>=<file> External variable (string from file)")
  private List<String> extStrFile = new ArrayList<>();

  @Option(
      names = {"--ext-code-file"},
      description = "<var>=<file> External variable (code from file)")
  private List<String> extCodeFile = new ArrayList<>();

  @Option(
      names = {"--tla-str", "-A"},
      description = "<var>=<val> Top-level argument (string)")
  private List<String> tlaStr = new ArrayList<>();

  @Option(
      names = {"--tla-code"},
      description = "<var>=<code> Top-level argument (Jsonnet code)")
  private List<String> tlaCode = new ArrayList<>();

  @Option(
      names = {"--tla-str-file"},
      description = "<var>=<file> Top-level argument (string from file)")
  private List<String> tlaStrFile = new ArrayList<>();

  @Option(
      names = {"--tla-code-file"},
      description = "<var>=<file> Top-level argument (code from file)")
  private List<String> tlaCodeFile = new ArrayList<>();

  @Option(
      names = {"-J", "--jpath"},
      description = "Library search path")
  private List<String> jpaths = new ArrayList<>();

  @Option(
      names = {"-o", "--output-file"},
      description = "Write to file instead of stdout")
  private File outputFile;

  @Option(
      names = {"-m", "--multi"},
      description = "Multi-file output directory")
  private File multiDir;

  @Option(
      names = {"-c", "--create-output-dirs"},
      description = "Create output directories")
  private boolean createOutputDirs;

  @Option(
      names = {"-S", "--string"},
      description = "Manifest as plain string")
  private boolean stringOutput;

  @Option(
      names = {"-y", "--yaml-stream"},
      description = "Output YAML stream")
  private boolean yamlStream;

  @Option(
      names = {"--no-trailing-newline"},
      description = "Omit trailing newline")
  private boolean noTrailingNewline;

  @Option(
      names = {"-s", "--max-stack"},
      description = "Max stack depth")
  private int maxStack = 500;

  @Option(
      names = {"-t", "--max-trace"},
      description = "Max trace depth for errors")
  private int maxTrace = 20;

  @Override
  public Integer call() throws Exception {
    if (file == null && execCode == null) {
      System.err.println("Error: Must provide a filename or use -e");
      return 1;
    }

    try (JsonnetEvaluator evaluator = new JsonnetEvaluator().registerDefaultNatives()) {
      Path wd = Path.of("").toAbsolutePath();
      for (String jp : jpaths) {
        evaluator.addJpath(wd.resolve(jp));
      }

      JsonnetEvaluator.Evaluation eval = evaluator.newEvaluation().stringOutput(stringOutput);
      bindVariables(eval);

      String output;
      if (execCode != null) {
        output = eval.evaluateSnippet(execCode, "<cmdline>");
      } else {
        output = eval.evaluateFile(file.toPath());
      }

      if (multiDir != null) {
        return handleMultiOutput(output);
      }

      writeOutput(output);

    } catch (PolyglotException e) {
      printJsonnetError(e, maxTrace);
      return 1;
    } catch (StackOverflowError e) {
      System.err.println(
          "Error: Stackoverflow while materializing, possibly due to recursive value");
      return 1;
    }
    return 0;
  }

  private void bindVariables(JsonnetEvaluator.Evaluation eval) {
    for (String s : extStr) {
      String[] parts = splitBinding(s);
      eval.extStr(parts[0], parts[1]);
    }
    for (String s : extCode) {
      String[] parts = splitBinding(s);
      eval.extCode(parts[0], parts[1]);
    }
    for (String s : extStrFile) {
      String[] parts = splitBinding(s);
      eval.extStrFile(parts[0], Path.of(parts[1]));
    }
    for (String s : extCodeFile) {
      String[] parts = splitBinding(s);
      eval.extCodeFile(parts[0], Path.of(parts[1]));
    }
    for (String s : tlaStr) {
      String[] parts = splitBinding(s);
      eval.tlaStr(parts[0], parts[1]);
    }
    for (String s : tlaCode) {
      String[] parts = splitBinding(s);
      eval.tlaCode(parts[0], parts[1]);
    }
    for (String s : tlaStrFile) {
      String[] parts = splitBinding(s);
      eval.tlaStrFile(parts[0], Path.of(parts[1]));
    }
    for (String s : tlaCodeFile) {
      String[] parts = splitBinding(s);
      eval.tlaCodeFile(parts[0], Path.of(parts[1]));
    }
  }

  private void writeOutput(String output) throws Exception {
    if (outputFile != null) {
      if (createOutputDirs) {
        outputFile.getAbsoluteFile().getParentFile().mkdirs();
      }
      try (FileWriter w = new FileWriter(outputFile)) {
        w.write(output);
        if (!noTrailingNewline && !output.endsWith("\n")) {
          w.write("\n");
        }
      }
    } else {
      if (noTrailingNewline) {
        System.out.print(output);
      } else {
        System.out.println(output);
      }
    }
  }

  private int handleMultiOutput(String output) throws Exception {
    if (createOutputDirs) {
      multiDir.mkdirs();
    }
    String[] lines = output.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.isEmpty()) continue;
      StringBuilder content = new StringBuilder();
      i++;
      while (i < lines.length && !lines[i].isEmpty()) {
        content.append(lines[i]).append("\n");
        i++;
      }
      File outFile = new File(multiDir, line);
      if (createOutputDirs) {
        outFile.getAbsoluteFile().getParentFile().mkdirs();
      }
      try (FileWriter w = new FileWriter(outFile)) {
        w.write(content.toString());
      }
      System.out.println(outFile.getPath());
    }
    return 0;
  }

  static void printJsonnetError(PolyglotException e, int maxTrace) {
    if (e.isInternalError()) {
      System.err.println("INTERNAL ERROR: " + e.getMessage());
      return;
    }

    if (e.isSyntaxError()) {
      System.err.println("Static error: " + e.getMessage());
      return;
    }

    if (e.isResourceExhausted()) {
      System.err.println(
          "Error: Stackoverflow while materializing, possibly due to recursive value");
      return;
    }

    System.err.println("Error: " + e.getMessage());
    int traceCount = 0;
    for (PolyglotException.StackFrame sf : e.getPolyglotStackTrace()) {
      if (!sf.isGuestFrame()) continue;
      String rootName = sf.getRootName();
      if (rootName == null) continue;
      if (traceCount >= maxTrace) {
        System.err.println("    ...");
        break;
      }
      SourceSection loc = sf.getSourceLocation();
      if (loc != null && loc.isAvailable()) {
        String sourceName = loc.getSource().getName();
        int line = loc.getStartLine();
        int col = loc.getStartColumn();
        System.err.println(
            "    at [" + rootName + "].(" + sourceName + ":" + line + ":" + col + ")");
      } else {
        System.err.println("    at [" + rootName + "]");
      }
      traceCount++;
    }
  }

  private static String[] splitBinding(String binding) {
    int eq = binding.indexOf('=');
    if (eq < 0) {
      return new String[] {binding, System.getenv().getOrDefault(binding, "")};
    }
    return new String[] {binding.substring(0, eq), binding.substring(eq + 1)};
  }

  public static void main(String... args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}

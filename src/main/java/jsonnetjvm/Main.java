package jsonnetjvm;

import jsonnetjvm.transpiler.JavaTranspiler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "jsonnet-jvm", mixinStandardHelpOptions = true, version = "1.0", description = "Jsonnet JVM Interpreter")
public class Main implements Callable<Integer> {

	@Parameters(index = "0", description = "The file to execute", defaultValue = "test.jsonnet")
	private File file;

	@Override
	public Integer call() throws Exception {
		var lexer = new JsonnetLexer(CharStreams.fromPath(file.toPath()));
		var tokens = new CommonTokenStream(lexer);
		var parser = new JsonnetParser(tokens);

		var tree = parser.jsonnet();

		// Use "ExampleGenerated" as the class name
		var transpiler = new JavaTranspiler("ExampleGenerated");
		String javaCode = transpiler.visit(tree);

		System.out.println(javaCode);

		return 0;
	}

	public static void main(String... args) {
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}
}

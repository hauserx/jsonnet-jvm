package jsonnetjvm;

import jsonnetjvm.transpiler.JavaTranspiler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.tools.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

		String className = "ExampleGenerated";
		var transpiler = new JavaTranspiler(className);
		String javaCode = transpiler.visit(tree);

		compileAndRun(className, javaCode);

		return 0;
	}

	private void compileAndRun(String className, String sourceCode) throws Exception {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new IllegalStateException("Cannot find system Java compiler. Ensure you are running with a JDK.");
		}

		Path tempDir = Files.createTempDirectory("jsonnet-jit");
		tempDir.toFile().deleteOnExit();

		// 1. Prepare compilation unit
		JavaFileObject fileObject = new InMemoryJavaFileObject(className, sourceCode);

		// 2. Set up diagnostics and file manager
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

		// 3. Configure options (Classpath and Output directory)
		List<String> options = Arrays.asList("-d", tempDir.toString(), "-cp", System.getProperty("java.class.path"));

		// 4. Compile
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
				Collections.singletonList(fileObject));

		boolean success = task.call();
		if (!success) {
			StringBuilder errorMsg = new StringBuilder("Compilation failed:\n");
			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
				errorMsg.append(diagnostic.getMessage(null)).append("\n");
			}
			throw new RuntimeException(errorMsg.toString());
		}

		// 5. Load and Run
		try (URLClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()},
				Main.class.getClassLoader() // Parent classloader (has runtime classes)
		)) {
			Class<?> cls = classLoader.loadClass(className);
			Method mainMethod = cls.getMethod("main", String[].class);

			// Invoke main(String[] args)
			mainMethod.invoke(null, (Object) new String[]{});
		}
	}

	// Helper class for in-memory source
	static class InMemoryJavaFileObject extends SimpleJavaFileObject {
		final String code;

		InMemoryJavaFileObject(String className, String code) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}

	public static void main(String... args) {
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}
}
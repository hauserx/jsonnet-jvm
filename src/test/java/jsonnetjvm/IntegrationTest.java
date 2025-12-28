package jsonnetjvm;

import jsonnetjvm.transpiler.JavaTranspiler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTest {

	@Test
	void testSimpleExample(@TempDir Path tempDir) throws Exception {
		Path resourceDir = Path.of("src/test/resources/simple");
		Path jsonnetFile = resourceDir.resolve("example.jsonnet");
		Path expectedJsonFile = resourceDir.resolve("expect.json");

		// 1. Transpile
		var lexer = new JsonnetLexer(CharStreams.fromPath(jsonnetFile));
		var tokens = new CommonTokenStream(lexer);
		var parser = new JsonnetParser(tokens);
		var tree = parser.jsonnet();

		String className = "ExampleGenerated";
		var transpiler = new JavaTranspiler(className);
		String javaCode = transpiler.visit(tree);

		// 2. Write generated code
		Path sourceFile = tempDir.resolve(className + ".java");
		Files.writeString(sourceFile, javaCode);

		// 3. Compile
		// We need the classpath that includes the runtime classes.
		// Assuming the build runs this test, the runtime classes are in
		// 'build/classes/java/main'
		// or equivalent. We can try to infer it or just assume standard Gradle layout.
		String classpath = System.getProperty("java.class.path");
		// Also add the build output directory explicitly if needed, but java.class.path
		// should cover it when running via Gradle?
		// When running via Gradle, the test classpath includes the main classes.
		// However, we are spawning a NEW javac process. We need to pass that classpath.

		// Let's assume standard gradle build output location for main classes
		Path mainClasses = Path.of("build/classes/java/main");
		if (!Files.exists(mainClasses)) {
			// Fallback or fail? Gradle might put them elsewhere (e.g. kotlin classes?)
			// But we are in Java project.
			// Actually, when running via Gradle, the classpath property is huge and
			// contains all jars.
		}

		ProcessBuilder javac = new ProcessBuilder("javac", "-cp", classpath, "-d", tempDir.toString(),
				sourceFile.toString());
		javac.inheritIO();
		Process javacProcess = javac.start();
		int javacExit = javacProcess.waitFor();
		assertEquals(0, javacExit, "Compilation failed");

		// 4. Run
		ProcessBuilder java = new ProcessBuilder("java", "-cp", tempDir.toString() + File.pathSeparator + classpath,
				className);

		// Capture output
		File outputFile = tempDir.resolve("output.json").toFile();
		java.redirectOutput(outputFile);
		java.redirectError(ProcessBuilder.Redirect.INHERIT);

		Process javaProcess = java.start();
		int javaExit = javaProcess.waitFor();
		assertEquals(0, javaExit, "Execution failed");

		// 5. Verify output
		String actualJson = Files.readString(outputFile.toPath()).trim();
		String expectedJson = Files.readString(expectedJsonFile).trim();

		// Normalize JSON for comparison (remove whitespace/newlines if needed, or use a
		// JSON comparator)
		// For this simple test, we might just strip whitespace.
		assertEquals(normalize(expectedJson), normalize(actualJson));
	}

	private String normalize(String json) {
		return json.replaceAll("\\s+", "");
	}
}

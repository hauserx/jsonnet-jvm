package jsonnetjvm;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTest {

    @Test
    void testSimpleExample() throws Exception {
        Path resourceDir = Path.of("src/test/resources/simple");
        Path jsonnetFile = resourceDir.resolve("example.jsonnet");
        Path expectedJsonFile = resourceDir.resolve("expect.json");

        // Capture stdout
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        try {
            // Run Main
            // We can call Main.main, but that calls System.exit().
            // Better to call Main.call() or CommandLine logic without exit.
            // But Main logic depends on picocli injection.
            // Simplest is to invoke via CommandLine manually.

            int exitCode = new picocli.CommandLine(new Main()).execute(jsonnetFile.toString());
            assertEquals(0, exitCode, "Main execution failed");

        } finally {
            System.setOut(originalOut);
        }

        // Verify Output
        String actualJson = outputStream.toString().trim();
        String expectedJson = Files.readString(expectedJsonFile).trim();

        assertEquals(normalize(expectedJson), normalize(actualJson));
    }

    private String normalize(String json) {
        return json.replaceAll("\\s+", "");
    }
}
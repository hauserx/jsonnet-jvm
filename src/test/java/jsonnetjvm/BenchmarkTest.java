package jsonnetjvm;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class BenchmarkTest {

    @Test
    void testLargeComprehension() throws Exception {
        System.out.println("Starting large comprehension benchmark...");
        long start = System.currentTimeMillis();

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode;
        try {
            exitCode = new picocli.CommandLine(new Main()).execute("src/test/resources/bench/comprehension.jsonnet");
        } finally {
            System.setOut(originalOut); // Restore original stdout
        }

        long end = System.currentTimeMillis();
        System.out.printf("Large Comprehension Benchmark finished in %d ms%n", end - start);

        assertEquals(0, exitCode, "Benchmark execution failed");

        String actualOutput = outputStream.toString().trim();
        // The expected output is the sum of (i < j) for i, j in 1..1000
        // This is sum(999+998+...+1) = 999 * 1000 / 2 = 499500
        assertEquals("499500", actualOutput);
    }
}

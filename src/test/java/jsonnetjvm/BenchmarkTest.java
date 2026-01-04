package jsonnetjvm;

import jsonnetjvm.truffle.JsonnetLanguage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BenchmarkTest {

    @Test
    @Timeout(60)
    void testLargeComprehension() throws Exception {
        File jsonnetFile = Path.of("src/test/resources/bench/comprehension.jsonnet").toFile();
        System.out.println("Starting large comprehension (50M ops) with JIT...");

        try (Context context = Context.newBuilder(JsonnetLanguage.ID).allowAllAccess(true).build()) {

            Source source = Source.newBuilder(JsonnetLanguage.ID, jsonnetFile).build();

            for (int i = 1; i <= 5; i++) {
                long start = System.currentTimeMillis();
                Value result = context.eval(source);
                long end = System.currentTimeMillis();

                assertEquals(50000000.0, result.asDouble(), "Iteration " + i + " failed");
                System.out.printf("Iteration %d finished in %d ms%n", i, end - start);
            }
        }
    }
}
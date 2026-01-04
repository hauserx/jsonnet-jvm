package jsonnetjvm;

import jsonnetjvm.truffle.JsonnetLanguage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TruffleTests {

    private Path getPath(String filename) {
        return Path.of("src/test/resources/truffle").resolve(filename);
    }

    @Test
    void testNumberLiteral() throws Exception {
        File jsonnetFile = getPath("number.jsonnet").toFile();
        try (Context context = Context.newBuilder(JsonnetLanguage.ID).allowAllAccess(true).build()) {
            Source source = Source.newBuilder(JsonnetLanguage.ID, jsonnetFile).build();
            Value result = context.eval(source);
            assertEquals(123.0, result.asDouble());
        }
    }

    @Test
    void testStringLiteral() throws Exception {
        File jsonnetFile = getPath("string.jsonnet").toFile();
        try (Context context = Context.newBuilder(JsonnetLanguage.ID).allowAllAccess(true).build()) {
            Source source = Source.newBuilder(JsonnetLanguage.ID, jsonnetFile).build();
            Value result = context.eval(source);
            assertEquals("hello", result.asString());
        }
    }
}
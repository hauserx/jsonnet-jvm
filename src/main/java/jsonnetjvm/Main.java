package jsonnetjvm;

import jsonnetjvm.runtime.Val;
import jsonnetjvm.truffle.JsonnetLanguage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "jsonnet-jvm", mixinStandardHelpOptions = true, version = "0.1", description = "Jsonnet JVM Interpreter (Truffle-based)")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "The file to execute")
    private File file;

    @Override
    public Integer call() throws Exception {
        try (Context context = Context.newBuilder(JsonnetLanguage.ID).allowAllAccess(true).build()) {

            Source source = Source.newBuilder(JsonnetLanguage.ID, file).build();
            Value result = context.eval(source);

            // Print the result using Polyglot interop if possible, otherwise fallback
            if (result.isNumber()) {
                double d = result.asDouble();
                if (d == (long) d)
                    System.out.println((long) d);
                else
                    System.out.println(d);
            } else if (result.isString()) {
                System.out.println("\"" + result.asString() + "\"");
            } else if (result.isBoolean()) {
                System.out.println(result.asBoolean());
            } else if (result.isNull()) {
                System.out.println("null");
            } else {
                // For Arrays and Objects, we might need custom logic or just toString for now
                // Ideally our Val types implement InteropLibrary correctly for all these.
                System.out.println(result.toString());
            }

        } catch (PolyglotException e) {
            System.err.println("Error: " + e.getMessage());
            // e.printStackTrace();
            return 1;
        }
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
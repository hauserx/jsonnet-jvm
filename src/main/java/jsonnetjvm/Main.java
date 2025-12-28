package jsonnetjvm;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "jsonnet-jvm", mixinStandardHelpOptions = true, version = "1.0",
         description = "Jsonnet JVM Interpreter")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "The file to execute", defaultValue = "test.jsonnet")
    private File file;

    @Override
    public Integer call() throws Exception {
        System.out.println("Hello from Jsonnet JVM! Processing: " + file.getName());
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}

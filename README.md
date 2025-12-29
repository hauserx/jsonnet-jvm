# jsonnet-jvm

An experimental [Jsonnet](https://jsonnet.org/) evaluator translating jsonnet code to Java Virtual Machine (JVM) instructions. This project aims to provide a fast and compliant implementation of the Jsonnet data templating language.

It's currently in alpha state - it's able to evaluate some simple jsonnet files. It also transpiles code to java instead of producing bytecode directly.

## Prerequisites

*   **Java 25**: This project uses Java 25 features. Ensure you have a compatible JDK installed (e.g., OpenJDK 25).

## Building

To build the project, run the following command in the project root:

```bash
./gradlew build
```

This will compile the Java sources, generate the ANTLR parsers, and run the tests.

## Usage

### Evaluating a file

You can run the CLI application directly using Gradle:

```bash
./gradlew run --args="src/test/resources/simple/example.jsonnet"
```

To run the built distribution, you must first generate it:

```bash
./gradlew installDist
```

Then execute the binary (ensure your `JAVA_HOME` or `java` in PATH points to JDK 25):

```bash
./build/install/jsonnet-jvm/bin/jsonnet-jvm src/test/resources/simple/example.jsonnet
```

### Transpiling to Java

To see the generated Java source code without compiling and running it, use the `--transpile-only` (or `-t`) flag. The output file will be saved to a temporary directory.

```bash
./gradlew run --args="-t src/test/resources/simple/example.jsonnet"
# Transpiled Java source saved to: /var/folders/.../ExampleGenerated.java
```

## Project Structure

*   `src/main/antlr/jsonnetjvm`: Contains the ANTLR4 grammar files (`Jsonnet.g4`).
*   `src/main/java/jsonnetjvm`: Contains the Java source code.
    *   `Main.java`: The CLI entry point.
*   `build.gradle.kts`: The Gradle build configuration.

## License

[Add License Here]

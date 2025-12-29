# jsonnet-jvm

An experimental [Jsonnet](https://jsonnet.org/) evaluator aimed to run jsonnet
code directly on Java Virtual Machine (JVM).

This project aims to provide a fast implementation of the Jsonnet for cases for
expensive jsonnet evaluators where cost of translation to JVM bytecode is
negligible compared to evaluation cost. It's domain are evaluations that take
tens to hundreds of seconds.

It's currently in Proof of Concept - it's able to evaluate some trivial jsonnet
files. It transpiles code to java instead of producing bytecode directly for now.

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

## Benchmark results

Results on very simple benchmark - `src/test/resources/bench/comprehension.jsonnet` file.
Just to validate jvm approach. Results from Apple M3 Pro.

|library|result|
|-------|------|
|jsonnet-jvm|1.3 sec|
|sjsonnet 0.5.10|4.8 sec|
|jrsonnet 0.4.2|11.0 sec|
|go-jsonnet 0.21.0|34.0 sec|



## Project Structure

*   `src/main/antlr/jsonnetjvm`: Contains the ANTLR4 grammar files (`Jsonnet.g4`).
*   `src/main/java/jsonnetjvm`: Contains the Java source code.
    *   `Main.java`: The CLI entry point.
*   `build.gradle.kts`: The Gradle build configuration.


## License

[Add License Here]

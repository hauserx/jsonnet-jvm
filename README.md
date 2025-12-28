# jsonnet-jvm

A [Jsonnet](https://jsonnet.org/) evaluator translating jsonnet code to Java Virtual Machine (JVM) instructions. This project aims to provide a fast and compliant implementation of the Jsonnet data templating language.

## Prerequisites

*   **Java 25**: This project uses Java 25 features. Ensure you have a compatible JDK installed (e.g., OpenJDK 25).

## Building

To build the project, run the following command in the project root:

```bash
./gradlew build
```

This will compile the Java sources, generate the ANTLR parsers, and run the tests.

## Running

You can run the CLI application directly using Gradle:

```bash
./gradlew run --args="path/to/file.jsonnet"
```

Or run the built distribution (after building):

```bash
./build/install/jsonnet-jvm/bin/jsonnet-jvm path/to/file.jsonnet
```

## Project Structure

*   `src/main/antlr/jsonnetjvm`: Contains the ANTLR4 grammar files (`Jsonnet.g4`).
*   `src/main/java/jsonnetjvm`: Contains the Java source code.
    *   `Main.java`: The CLI entry point.
*   `build.gradle.kts`: The Gradle build configuration.

## License

[Add License Here]

# jsonnet-jvm

An experimental [Jsonnet](https://jsonnet.org/) evaluator aimed to run jsonnet
code directly on Java Virtual Machine (JVM) using Graal Truffle.

This project aims to provide a fast implementation of the Jsonnet for expensive
jsonnet evaluators taking tens to hundreds of seconds.

It's currently in Proof of Concept - it's able to evaluate some trivial jsonnet
files. It needs a lot of cleanup - most of the code was vibe-coded.

## Prerequisites

*   **Java 25**: This project uses Java 25 features. Ensure you have a compatible JDK installed (e.g., OpenJDK 25).
*   **Native image**: If you want to self-contained native image for a platform, ensure [GraalVM](https://www.graalvm.org/) is installed.

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

## Building native image

One may also build native image - self-contained binary. Some initial tries
show fast startup (~25ms), but slower evaluation for the simplest benchmarks.
To build native image - it takes about a minute, also ensure you have graal JVM in PATH:

```
./gradlew nativeCompile
./build/native/nativeCompile/jsonnet-jvm src/test/resources/simple/example.jsonnet
```

## Benchmark results

Results for very simple benchmark - `src/test/resources/bench/comprehension.jsonnet` file.
Just to validate jvm approach. Results from Apple M3 Pro.

|library|result|
|-------|------|
|jsonnet-jvm (nativeImage)|0.6 sec|
|jsonnet-jvm|0.7 sec|
|sjsonnet 0.5.10|4.8 sec|
|jrsonnet 0.4.2|11.0 sec|
|go-jsonnet 0.21.0|34.0 sec|

Results for `src/test/resources/bench/loop_multi.jsonnet`

|library|result|
|-------|------|
|jsonnet-jvm|4.8 sec|
|jsonnet-jvm (nativeImage)|12 sec|
|sjsonnet 0.5.10|73 sec|
|jrsonnet 0.4.2|225 sec|


## Project Structure

*   `src/main/antlr/jsonnetjvm`: Contains the ANTLR4 grammar files (`Jsonnet.g4`).
*   `src/main/java/jsonnetjvm`: Contains the Java source code.
    *   `Main.java`: The CLI entry point.
*   `build.gradle.kts`: The Gradle build configuration.


## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

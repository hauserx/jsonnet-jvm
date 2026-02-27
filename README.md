# jsonnet-jvm

An experimental [Jsonnet](https://jsonnet.org/) evaluator aimed to run jsonnet
code directly on Java Virtual Machine (JVM) using Graal Truffle.

This project aims to provide a fast implementation of the Jsonnet for expensive
jsonnet evaluators taking tens to hundreds of seconds.

It's currently in Proof of Concept - it's able to evaluate some trivial jsonnet
files. It needs a lot of cleanup - most of the code was vibe-coded.

## Prerequisites

*   **Bazel**: This project uses [Bazel](https://bazel.build/) as its build system. GraalVM JDK 25 is downloaded automatically — no manual JDK setup required.

## Building

To build the project, run the following command in the project root:

```bash
bazel build //:jsonnet_jvm
```

This will compile the Java sources, generate the ANTLR parsers, and produce the binary.

To run all tests:

```bash
bazel test //:all_tests
```

## Usage

### Evaluating a file

Run directly with Bazel:

```bash
bazel run //:jsonnet_jvm -- src/test/resources/simple/example.jsonnet
```

Or build a standalone deploy JAR first:

```bash
bazel build //:jsonnet_jvm_deploy.jar
java -jar bazel-bin/jsonnet_jvm_deploy.jar src/test/resources/simple/example.jsonnet
```

### Native image

Build a self-contained native binary (no JVM required at runtime):

```bash
bazel build //:jsonnet_jvm_native
bazel-bin/jsonnet_jvm_native-bin src/test/resources/simple/example.jsonnet
```

## Benchmark results

Benchmark files live in `src/test/resources/bench/`. Build first, then run the binary directly
(avoids Bazel launcher overhead):

```bash
bazel build //:jsonnet_jvm
bazel-bin/jsonnet_jvm src/test/resources/bench/comprehension.jsonnet
```

### `comprehension.jsonnet` — 10k×5k array comprehension

Results from Apple M3 Pro (with Truffle JIT):

|library|result|
|-------|------|
| (previous POC version) jsonnet-jvm (native image)|0.6 sec|
| (previous POC version) jsonnet-jvm|0.7 sec|
|sjsonnet 0.5.10|4.8 sec|
|jrsonnet 0.4.2|11.0 sec|
|go-jsonnet 0.21.0|34.0 sec|

Results from AWS Linux:

|mode|result|
|----|------|
|GraalVM CE JIT|1.7 sec|
|native image|1.4 sec|

### `loop_multi.jsonnet` — 10×(10k×10k) array comprehensions

Results from Apple M3 Pro (with Truffle JIT):

|library|result|
|-------|------|
| (previous POC version) jsonnet-jvm|4.8 sec|
| (previous POC version) jsonnet-jvm (native image)|12 sec|
|sjsonnet 0.5.10|73 sec|
|jrsonnet 0.4.2|225 sec|

Results from AWS Linux:

|mode|result|
|----|------|
|GraalVM CE JIT|17 sec|
|native image|27 sec|


## Project Structure

*   `src/main/antlr/com/databricks/jsonnetjvm`: Contains the ANTLR4 grammar files (`Jsonnet.g4`).
*   `src/main/java/com/databricks/jsonnetjvm`: Contains the Java source code.
    *   `Main.java`: The CLI entry point.
*   `BUILD.bazel`: Build targets (library, binary, tests).
*   `MODULE.bazel`: Bzlmod dependencies.


## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

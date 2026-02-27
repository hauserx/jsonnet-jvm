# jsonnet-jvm Development Guide

## Build & Test

Gradle is configured to find JDK25 automatically. No need to set JAVA_HOME.

```bash
cd /home/stephen.amar/universe/experimental/stephen.amar/jsonnet-jvm
./gradlew test
```

Build only:
```bash
./gradlew build
```

Native image:
```bash
./gradlew nativeCompile
```

## Test Structure

- **Integration tests**: `src/test/java/com/databricks/jsonnetjvm/IntegrationTest.java`
  - Parameterized tests from `src/test/resources/cpp_test_suite/*.jsonnet`
  - Golden files: `*.jsonnet.golden`
  - Skip list in `IntegrationTest.SKIP` for tests requiring unimplemented features
- **Unit tests**: `src/test/java/com/databricks/jsonnetjvm/TruffleTests.java`

## Test Files

Test `.jsonnet` files in `cpp_test_suite/` should be kept identical to the sjsonnet originals at `~/sjsonnet/sjsonnet/test/resources/test_suite/`. When a test has lines that fail due to unimplemented features, comment them with `// COMMENTED: <reason>`. If the entire test body is commented, add the test name to the `SKIP` set in `IntegrationTest.java` instead.

## Architecture

- **Grammar**: `src/main/antlr/com/databricks/jsonnetjvm/Jsonnet.g4` (ANTLR4)
  - ANTLR4 gives EARLIER alternatives HIGHER precedence in left-recursive rules
- **AST Builder**: `src/main/java/com/databricks/jsonnetjvm/truffle/parser/JsonnetTruffleASTBuilder.java`
- **Runtime types**: `double`, `String`, `boolean`, `JNull.INSTANCE`, `JArray`, `JObject`, `JFunction`
- **Stdlib modules**: `src/main/java/com/databricks/jsonnetjvm/stdlib/Std*.java`
- **Truffle nodes**: `src/main/java/com/databricks/jsonnetjvm/truffle/nodes/expressions/`

## Key Design Decisions

- Functions capture closure frames (`MaterializedFrame`) for lexical scoping
- Local bindings use thunks (`JsonnetThunk`) for lazy evaluation and forward references
- Stdlib functions are Java lambdas wrapped in `JsonnetBuiltinRootNode`
- `std` object is a `JObject` injected into frame slot 0 of every scope

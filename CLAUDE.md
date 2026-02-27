# jsonnet-jvm Development Guide

## Build & Test

Bazel downloads GraalVM CE 25 automatically. No manual JDK setup needed.

Run all tests:
```bash
bazel test //:all_tests
```

Run a specific test:
```bash
bazel test //:IntegrationTest
bazel test //:ParserTest
```

Build the binary:
```bash
bazel build //:jsonnet_jvm
```

Run the binary:
```bash
bazel run //:jsonnet_jvm -- <args>
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

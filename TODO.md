# jsonnet-jvm TODO

## P1: Skipped Tests from sjsonnet Port — DONE

All previously skipped tests have been ported and pass. Key changes:
- Lazy builtin args: thunks pass through to `JsonnetBuiltinRootNode`, which auto-forces by default; `lazyFn()` builtins force selectively
- `std.get` uses `lazyFn` to avoid forcing default when key exists
- `std.mergePatch` uses `JObject.getFieldSupplier()` to copy fields without forcing values
- 22 error condition tests with golden error messages (including duplicate field, non-boolean condition, filter type, and unpaired surrogate detection)
- ExtVar/TLA test infrastructure via `.args` sidecar files
- `std.md5` long Lorem ipsum test restored
- Object comprehension visibility test confirmed working
- `std.mergePatch` target field order preservation (`preserveOrder`) skipped — sjsonnet-specific extension

### Previously skipped evaluator tests — FIXED
- `std.slice` negative indices for strings (codepoint-aware)
- Zero-arg method field syntax (`foo():: expr` properly wraps in function)
- Object comprehension `local` can reference loop variables (`{local x2 = k * 2, ... for k in ...}`)
- Object comprehension `+:` syntax (`{["foo"]+: x for x in ...}`)
- `super` in local/apply context (`local y = {b: super.a}; x + y`)
- `lib.foo()` with `global.base` and `super` in comprehension objlocals
- `%d` format for large doubles (uses `BigDecimal` for exact representation)

All evaluator tests pass — including `{local y = $["2"], [x]: y for ...}` (objlocal with `$` in comprehension)

## P2: Nice to have
- [x] **tailstrict** — TCO via trampoline with `TailCallException` (a `ControlFlowException`). Strict argument forcing + stack frame reuse for deep recursion
- [x] **std.native("xz")** — ported using `org.tukaani:xz:1.10` (LZMA2)

### P2: Feature parity with go-jsonnet CLI — DONE
All flags ported:
`-e`, `-o`, `-m`, `-c`, `-S`, `-y`, `--no-trailing-newline`, `-s/--max-stack`, `-t/--max-trace`,
`--ext-str-file`, `--ext-code-file`, `--tla-str-file`, `--tla-code-file`

### P2: Additional test suites from sjsonnet — DONE
Ported:
- [x] EvaluatorTests (~170 assertions + 14 error tests)
- [x] NonBooleanExprTests (2 assertions + 3 error tests)
- [x] ParseYamlTests (8 assertions)
- [x] UnicodeHandlingTests (~55 assertions + 9 error tests, codepoint-based fixes)

Not portable as jsonnet files (require internal APIs):
- [x] DecimalFormatTests — ported as Java unit test `DecimalFormatTest.java` (26 cases)
- [x] ParserTests — ported as Java unit test `ParserTest.java` (33 cases: operator precedence, duplicate detection, computed imports, identifier rules, digit separators)
- YamlRendererTests (tests YamlRenderer directly)

### Unit test suites for internal classes
Direct Java tests for non-trivial classes (complementary to IntegrationTests):
- [x] `JsonnetJsonTest` — formatNumber, parseJson, renderCompact, renderInline, render, round-trip (15 tests)
- [x] `ArrayBuilderTest` — type promotion, specialized arrays, capacity growth (10 tests)
- [x] `JsonnetStringsTest` — codepoint comparison, surrogates, emoji (8 tests)
- [x] `JObjectTest` — fields, visibility, merge, removeKey, locals (14 tests)
- [x] `StdFormatTest` — std.format: string/int/float/hex/octal/char/width/flags/named args (17 tests)
- [x] `ManifestTest` — manifestJsonEx, manifestYamlDoc, manifestIni, manifestPython, manifestXmlJsonml, manifestTomlEx (17 tests)
- [x] `NativeRegexTest` — partialMatch, fullMatch, replace, globalReplace, quoteMeta (9 tests)
- [x] `EncodingTest` — base64 encode/decode/decodeBytes, md5, sha1, sha256, sha512, sha3 (11 tests)

Skipped:
- PreserveOrderTests (requires preserveOrder flag — sjsonnet-specific extension)

### Code cleanup & refactoring — DONE
- [x] Dead code cleanup: removed unused `JsonnetLiteralNode` abstract class, `JsonnetConfig.sourceFileName` field/getter/setter, `JsonnetTruffleASTBuilder.getSource()`, unused imports
- [x] Extensibility API: `JsonnetEvaluator.registerNative(name, params, body)` for custom native functions via `std.native()`, with per-evaluation config propagation; `StdRuntimeModule` refactored to instance-based lookup
- [x] Main refactored to use `JsonnetEvaluator` — eliminated duplicate config wiring, Source/Context management; file-based ext/tla vars delegated to Evaluation API

### P3: Caching layers for repeated evaluation — DONE
Implemented multi-layer caching for the common pattern of evaluating the same jsonnet with different TLA/ExtVar inputs:
- [x] **Parse cache** — `JsonnetLanguage.parseCache` caches `CallTarget` per `(sourceName, content)`, skipping ANTLR lexing/parsing and AST building on repeat parses
- [x] **ExtVar evaluation cache** — `JsonnetConfig.extVarCache` caches evaluated extVar results by variable name, avoiding re-parsing/evaluating on each `std.extVar()` call
- [x] **importstr cache** — `JsonnetConfig.importStrCache` caches file contents by canonical path for `importstr` expressions
- [x] **Import evaluation cache** — pre-existing `JsonnetConfig.importCache` caches evaluated import results by canonical path
- [x] **ContextPolicy.REUSE** — Language instance (including parse cache and std library) persists across Context instances sharing the same Engine; JIT-compiled code is shared
- [x] **TlaRootNode deferred binding** — TLA vars read at execution time from thread-local config, not at parse time, so cached CallTargets work with different bindings
- [x] **JsonnetEvaluator** — high-level API wrapping a shared Engine for repeated evaluation with different ext/TLA bindings (9 tests)

### P3: Investigate Graal/Truffle AOT (native-image) — DONE
Native image builds and passes all 395 tests (`nativeCompile` + `nativeTest`):
- [x] Gradle toolchain configured: `javaLauncher` points to GraalVM JDK 25, `toolchainDetection` enabled
- [x] Build flags: `--no-fallback`, `-O3`, `--static-nolibc`, `-R:MaxHeapSize=2G`, `--initialize-at-build-time`, `-H:+ReportExceptionStackTraces`
- [x] Reflection config for picocli (`AutoHelpMixin`, `HelpCommand`) in `META-INF/native-image/jsonnet-jvm/reflect-config.json`
- [x] `@TruffleBoundary` on all non-primitive specializations and error paths to prevent partial evaluator from reaching blocklisted JDK methods (comparison nodes, modulo/format, field access, call, assert, arg read, thunk creation, stdlib init, parse)
- [x] Proper type checks for computed field names (replaces `ClassCastException` with `JsonnetException`)
- [ ] Benchmark startup time: JVM mode vs native-image
- [ ] Investigate closed-world assumptions — can we pre-specialize Truffle nodes for Jsonnet's fixed type system at build time?

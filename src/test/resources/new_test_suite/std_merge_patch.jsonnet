// Ported from sjsonnet StdMergePatchTests (happy-path tests)

// Top-level merging of non-objects
std.assertEqual(std.mergePatch([{a: 1}], [{b: 2}]), [{b: 2}]) &&
std.assertEqual(std.mergePatch({a: 1}, [{b: 2}]), [{b: 2}]) &&
std.assertEqual(std.mergePatch([{a: 1}], {b: 2}), {b: 2}) &&

// Top-level nulls
std.assertEqual(std.mergePatch(null, {a: 1}), {a: 1}) &&
std.assertEqual(std.mergePatch({a: 1}, null), null) &&

// Basic merges
std.assertEqual(std.mergePatch({a: 1}, {b: 2}), {a: 1, b: 2}) &&
std.assertEqual(std.mergePatch({a: 1}, {a: 2}), {a: 2}) &&
std.assertEqual(std.mergePatch({a: {b: 1}}, {a: {c: 2}}), {a: {b: 1, c: 2}}) &&
std.assertEqual(std.mergePatch({a: {b: 1, c: 1}}, {a: {b: 2}}), {a: {b: 2, c: 1}}) &&

// Null fields — nested nulls in patch remove fields from target
std.assertEqual(std.mergePatch({a: {b: 1, c: 1}}, {a: {b: null}}), {a: {c: 1}}) &&
// Nested nulls in target are preserved
std.assertEqual(std.mergePatch({a: null}, {b: 2}), {a: null, b: 2}) &&

// Hidden target fields are dropped in the output
std.assertEqual(std.objectFieldsAll(std.mergePatch({hidden:: 1, visible: 1}, {})), ["visible"]) &&
std.assertEqual(
  std.objectFieldsAll(std.mergePatch({hidden:: 1, visible: 1}, {added: 1})),
  ["added", "visible"]
) &&

// Hidden nested target fields are preserved if nothing merges with them
std.assertEqual(std.objectFields(std.mergePatch({a: {h:: 1, v: 1}}, {}).a), ["v"]) &&
std.assertEqual(std.objectFieldsAll(std.mergePatch({a: {h:: 1, v: 1}}, {}).a), ["h", "v"]) &&
std.assertEqual(std.mergePatch({a: {h:: 1, v: 1}}, {}).a.h, 1) &&

// Hidden nested fields dropped if something merges with enclosing object
std.assertEqual(std.objectFieldsAll(std.mergePatch({a: {h:: 1, v: 1}}, {a: {}}).a), ["v"]) &&

// Hidden target fields don't merge with visible patch fields
std.assertEqual(std.mergePatch({ a:: { a: 1 }, visible: 1 }, { a: { b: 2 }}), { visible: 1, a: { b: 2 }}) &&

// Hidden patch fields are dropped
std.assertEqual(std.objectFieldsAll(std.mergePatch({visible: 1}, {hidden:: 2})), ["visible"]) &&
std.assertEqual(std.mergePatch({ a: 1 }, { a:: 2}), {a: 1}) &&
std.assertEqual(std.mergePatch({ a: { b: 1 } }, { a:: { c: 1 }}), { a: { b: 1 }}) &&
std.assertEqual(std.objectFieldsAll(std.mergePatch({ a: { b: 1 } }, { a:: { c: 1 }}).a), ["b"]) &&

// +: is ignored during merge
std.assertEqual({a: 1} + {a+: 2}, {a: 3}) &&
std.assertEqual(std.mergePatch({a: 1}, {a+: 2}), {a: 2}) &&
std.assertEqual({a: 1} + std.mergePatch({}, {a+: 2}), {a: 2}) &&
std.assertEqual({a: 1} + std.mergePatch({a+: 2}, {}), {a: 2}) &&
std.assertEqual({a: {b: 1}} + std.mergePatch({a: {b+: 2}}, {}), {a: {b: 2}}) &&

// Default visibility — fields from mergePatch are default visibility
std.assertEqual({a:: 0} + std.mergePatch({a: 1}, {}), {}) &&
std.assertEqual(({a:: 0} + std.mergePatch({a: 1}, {})).a, 1) &&

true

// Comprehensive std.mergePatch tests from sjsonnet StdMergePatchTests
local assertEqual(a, b) =
  if a == b then true
  else error ("assertEqual failed: " + std.toString(a) + " != " + std.toString(b));

// top-level merging of non-objects
assertEqual(std.mergePatch([{a: 1}], [{b: 2}]), [{b: 2}]) &&
assertEqual(std.mergePatch({a: 1}, [{b: 2}]), [{b: 2}]) &&
assertEqual(std.mergePatch([{a: 1}], {b: 2}), {b: 2}) &&

// top-level nulls
assertEqual(std.mergePatch(null, {a: 1}), {a: 1}) &&
assertEqual(std.mergePatch({a: 1}, null), null) &&

// basic merges
assertEqual(std.mergePatch({a: 1}, {b: 2}), {a: 1, b: 2}) &&
assertEqual(std.mergePatch({a: 1}, {a: 2}), {a: 2}) &&
assertEqual(std.mergePatch({a: {b: 1}}, {a: {c: 2}}), {a: {b: 1, c: 2}}) &&
assertEqual(std.mergePatch({a: {b: 1, c: 1}}, {a: {b: 2}}), {a: {b: 2, c: 1}}) &&

// null fields
assertEqual(std.mergePatch({a: {b: 1, c: 1}}, {a: {b: null}}), {a: {c: 1}}) &&
assertEqual(std.mergePatch({a: null}, {b: 2}), {a: null, b: 2}) &&

// hidden target fields
assertEqual(std.objectFieldsAll({hidden:: 1, visible: 1}), ["hidden", "visible"]) &&
assertEqual(std.objectFieldsAll(std.mergePatch({hidden:: 1, visible: 1}, {})), ["visible"]) &&
assertEqual(std.objectFieldsAll(std.mergePatch({hidden:: 1, visible: 1}, {added: 1})), ["added", "visible"]) &&
assertEqual(std.objectFields(std.mergePatch({a: {h:: 1, v: 1}}, {}).a), ["v"]) &&
assertEqual(std.objectFieldsAll(std.mergePatch({a: {h:: 1, v: 1}}, {}).a), ["h", "v"]) &&
assertEqual(std.mergePatch({a: {h:: 1, v: 1}}, {}).a.h, 1) &&
assertEqual(std.objectFieldsAll(std.mergePatch({a: {h:: 1, v: 1}}, {a: {}}).a), ["v"]) &&
assertEqual(std.mergePatch({ a:: { a: 1 } , visible: 1 }, { a: { b: 2 }}), {visible: 1, a: {b: 2}}) &&

// hidden patch fields
assertEqual(std.objectFieldsAll(std.mergePatch({visible: 1}, {hidden:: 2})), ["visible"]) &&
assertEqual(std.mergePatch({ a: 1 }, { a:: 2}), {a: 1}) &&
assertEqual(std.mergePatch({ a: { b: 1 } }, { a:: { c: 1 }}), {a: {b: 1}}) &&
assertEqual(std.objectFieldsAll(std.mergePatch({ a: { b: 1 } }, { a:: { c: 1 }}).a), ["b"]) &&

// plus is ignored during merge
assertEqual({a: 1} + {a+: 2}, {a: 3}) &&
assertEqual(std.mergePatch({a: 1}, {a+: 2}), {a: 2}) &&
assertEqual({a: 1} + std.mergePatch({}, {a+: 2}), {a: 2}) &&
assertEqual({a: 1} + std.mergePatch({a+: 2}, {}), {a: 2}) &&
assertEqual({a: {b: 1}} + std.mergePatch({a: {b +: 2}}, {}), {a: {b: 2}}) &&

// default visibility of resulting fields
assertEqual(std.objectFields({a:: 0} + std.mergePatch({a: 1}, {})), []) &&
assertEqual(({a:: 0} + std.mergePatch({a: 1}, {})).a, 1) &&

true

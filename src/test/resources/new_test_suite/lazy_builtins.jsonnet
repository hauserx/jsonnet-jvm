// Tests for lazy evaluation of builtin arguments
local assertEqual(a, b) =
  if a == b then true
  else error ("assertEqual failed: " + std.toString(a) + " != " + std.toString(b));

// std.get should not force default when key exists
assertEqual(std.get({a: 1}, "a", error "a"), 1) &&

// std.mergePatch should not force target fields not accessed
assertEqual((std.mergePatch({ boom: error "should not error" }, {}) + { flag: 42 }).flag, 42) &&

// std.mergePatch should not force target fields removed by null patch
assertEqual((std.mergePatch({ boom: error "should not error" }, { boom: null }) + { flag: 42 }).flag, 42) &&

// std.mergePatch should not force target fields replaced by patch
assertEqual((std.mergePatch({ boom: error "should not error" }, { boom: 1 }) + { flag: 42 }).flag, 42) &&

true

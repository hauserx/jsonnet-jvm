local assertEqual(a, b) =
  if a == b then true
  else error ("assertEqual failed: " + std.toString(a) + " != " + std.toString(b));

assertEqual(std.extVar("num"), 1) &&
assertEqual(std.extVar("str"), "hello") &&
assertEqual(std.extVar("bool"), true) &&
assertEqual(std.extVar("jsonArrNums"), [1, 2, 3]) &&
assertEqual(std.extVar("jsonObjBools"), {"hello": false}) &&
assertEqual(std.extVar("code"), {"x": 1, "y": 2}) &&
assertEqual(std.extVar("std"), 5) &&
assertEqual(std.extVar("stdExtVar"), 15) &&
assertEqual(std.extVar("stdExtVarRecursive"), 115) &&
// The key can also be a lazily evaluated expression
assertEqual(std.extVar(std.join("", ["s", "t", "r"])), "hello") &&
true

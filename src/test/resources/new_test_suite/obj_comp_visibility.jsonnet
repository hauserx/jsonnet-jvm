// Object comprehension should inherit field visibility from the base object
// {a:: 0} makes 'a' hidden; merging with {[a]: 1 for a in ["a"]} should keep it hidden
local assertEqual(a, b) =
  if a == b then true
  else error ("assertEqual failed: " + std.toString(a) + " != " + std.toString(b));

assertEqual(std.objectFields({a:: 0} + {[a]: 1 for a in ["a"]}), []) &&
assertEqual(std.objectFieldsAll({a:: 0} + {[a]: 1 for a in ["a"]}), ["a"]) &&
true

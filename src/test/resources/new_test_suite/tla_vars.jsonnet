function(num, str, bool, jsonArrNums, jsonObjBools, code, std_val)
  local assertEqual(a, b) =
    if a == b then true
    else error ("assertEqual failed: " + std.toString(a) + " != " + std.toString(b));

  assertEqual(num, 1) &&
  assertEqual(str, "hello") &&
  assertEqual(bool, true) &&
  assertEqual(jsonArrNums, [1, 2, 3]) &&
  assertEqual(jsonObjBools, {"hello": false}) &&
  assertEqual(code, {"x": 1, "y": 2}) &&
  assertEqual(std_val, 5) &&
  true

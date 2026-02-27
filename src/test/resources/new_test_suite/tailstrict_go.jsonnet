// Ported from go_test_suite tailstrict tests
local assertEqual = std.assertEqual;

// tailstrict4.jsonnet: named args with default error that is overridden
local foo(x, y=error "xxx") = x;
assertEqual(foo(42, y=5) tailstrict, 42) &&

// tailstrict5.jsonnet: sum 1 to 1000
local sum(x, v) =
  if x <= 0 then v
  else sum(x - 1, x + v) tailstrict;
assertEqual(sum(1000, 0), 500500) &&

// tailstrict_operator1: function call inside operator
local g() = 41;
local f(x) = g() + x;
assertEqual(f(1) tailstrict, 42) &&

// tailstrict_operator2: && with tailstrict
local g2() = true;
local f2(x) = g2() && x;
assertEqual(f2(true) tailstrict, true) &&

// tailstrict_operator3: || with tailstrict
local g3() = false;
local f3(x) = g3() || x;
assertEqual(f3(true) tailstrict, true) &&

true

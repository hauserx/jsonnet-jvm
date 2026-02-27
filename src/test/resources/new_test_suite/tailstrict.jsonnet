// tailstrict tests — strict argument evaluation and TCO
local assertEqual = std.assertEqual;

// Basic tailstrict: sum 1 to N
local sum_from_one_to_n(counter, n) =
  if n == 0 then counter
  else sum_from_one_to_n(counter + n, n - 1) tailstrict;

assertEqual(sum_from_one_to_n(0, 10), 55) &&

// tailstrict forces arguments eagerly, enabling deep recursion without stack overflow
local countdown(n, acc) =
  if n == 0 then acc
  else countdown(n - 1, acc + 1) tailstrict;

assertEqual(countdown(100000, 0), 100000) &&

// tailstrict with applyNTimes pattern
local applyNTimes(n, f, x) =
  if n == 0 then x
  else applyNTimes(n - 1, f, f(x)) tailstrict;

assertEqual(applyNTimes(100000, function(x) x + 1, 0), 100000) &&

// Non-tailstrict recursive function (smaller depth to avoid stack overflow)
local factorial(n) =
  if n <= 1 then 1
  else n * factorial(n - 1);

assertEqual(factorial(10), 3628800) &&

true

// Ported from sjsonnet StdMapTests
std.assertEqual(std.map(function(x) x * x, []), []) &&
std.assertEqual(std.map(function(x) x * x, [1, 2, 3, 4]), [1, 4, 9, 16]) &&

// Map accepts strings, interpreting as array of one-character strings
std.assertEqual(std.map(function(x) x + x, 'Hello'), ["HH", "ee", "ll", "ll", "oo"]) &&

// Lazy evaluation: element 0 would error but we only access element 1
std.assertEqual(std.map(function(x) assert x != 'A'; x + x, 'AB')[1], "BB") &&

// Returning arbitrary values from the mapping function
std.assertEqual(std.map(function(x) std.codepoint(x), 'AB'), [65, 66]) &&

true

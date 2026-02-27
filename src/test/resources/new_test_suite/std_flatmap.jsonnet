// Ported from sjsonnet StdFlatMapTests
std.assertEqual(std.flatMap(function(x) [x, x], [1, 2, 3]), [1, 1, 2, 2, 3, 3]) &&
std.assertEqual(std.flatMap(function(x) if x == 2 then [] else [x], [1, 2, 3]), [1, 3]) &&
std.assertEqual(std.flatMap(function(x) if x == 2 then [] else [x * 3, x * 2], [1, 2, 3]), [3, 2, 9, 6]) &&

// flatMap on strings
std.assertEqual(std.flatMap(function(x) x + x, 'Hello'), "HHeelllloo") &&

// null in string flatMap skips the character
std.assertEqual(std.flatMap(function(x) if x == " " then null else x, "a b c d e"), "abcde") &&

true

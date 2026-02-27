// Ported from sjsonnet Std0150FunctionsTests (happy-path tests)

// clamp
std.assertEqual(std.clamp(-3, 0, 5), 0) &&
std.assertEqual(std.clamp(4, 0, 5), 4) &&
std.assertEqual(std.clamp(7, 0, 5), 5) &&

// member
std.assertEqual(std.member('foo', 'o'), true) &&
std.assertEqual(std.member('foo', 'f'), true) &&
std.assertEqual(std.member('foo', 'x'), false) &&
std.assertEqual(std.member([], 'o'), false) &&
std.assertEqual(std.member(['f'], 'o'), false) &&
std.assertEqual(std.member(['f', 'o', 'o'], 'o'), true) &&
std.assertEqual(std.member(['f', 'o', 'o'], 'f'), true) &&
std.assertEqual(std.member(['f', 'o', 'o'], 'g'), false) &&
std.assertEqual(std.member([1, 2, 3], 1), true) &&
std.assertEqual(std.member([1, 2, 3], 4), false) &&
std.assertEqual(std.member([['f', 'o', 'o'], ['b', 'a', 'r']], ['f', 'o', 'o']), true) &&

// repeat
std.assertEqual(std.repeat([], 0), []) &&
std.assertEqual(std.repeat([1], 1), [1]) &&
std.assertEqual(std.repeat([1, 2], 1), [1, 2]) &&
std.assertEqual(std.repeat([1], 2), [1, 1]) &&
std.assertEqual(std.repeat([1, 2], 2), [1, 2, 1, 2]) &&
std.assertEqual(std.repeat('a', 1), "a") &&
std.assertEqual(std.repeat('a', 4), "aaaa") &&
std.assertEqual(std.repeat('ab', 4), "abababab") &&
std.assertEqual(std.repeat('a', 0), "") &&

// join
std.assertEqual(std.join(' ', ['', 'foo']), " foo") &&
std.assertEqual(std.join(' ', [null, 'foo']), "foo") &&

// slice
std.assertEqual(std.slice([1, 2, 3, 4, 5, 6], 0, 4, 1), [1, 2, 3, 4]) &&
std.assertEqual(std.slice([1, 2, 3, 4, 5, 6], 1, 6, 2), [2, 4, 6]) &&
std.assertEqual(std.slice("jsonnet", 0, 4, 1), "json") &&

// manifestJsonMinified
std.assertEqual(
  std.manifestJsonMinified({ x: [1, 2, 3, true, false, null, "string\nstring", []], y: { a: 1, b: 2, c: [1, 2], d: {} } }),
  '{"x":[1,2,3,true,false,null,"string\\nstring",[]],"y":{"a":1,"b":2,"c":[1,2],"d":{}}}'
) &&

// manifestXmlJsonml
std.assertEqual(
  std.manifestXmlJsonml([
    'svg', { height: 100, width: 100 },
    [
      'circle', {
        cx: 50, cy: 50, r: 40,
        stroke: 'black', 'stroke-width': 3,
        fill: 'red',
      },
    ],
  ]),
  '<svg height="100" width="100"><circle cx="50" cy="50" fill="red" r="40" stroke="black" stroke-width="3"></circle></svg>'
) &&

// fold
std.assertEqual(std.foldr(function(acc, it) acc + " " + it, "jsonnet", "this is"), "j s o n n e t this is") &&
std.assertEqual(std.foldr(function(v, i) i + v + v, 'bcd', 'a'), "addccbb") &&
std.assertEqual(std.foldl(function(acc, it) acc + " " + it, "jsonnet", "this is"), "this is j s o n n e t") &&

// reverse
std.assertEqual(std.reverse([]), []) &&
std.assertEqual(std.reverse([1]), [1]) &&
std.assertEqual(std.reverse(["1", true, null]), [null, true, "1"]) &&

// get
std.assertEqual(std.get({a: 1}, "a"), 1) &&
std.assertEqual(std.get({a:: 1}, "a"), 1) &&
std.assertEqual(std.get({a: 1}, "b"), null) &&
std.assertEqual(std.get({a: 1}, "b", default=2), 2) &&
std.assertEqual(std.get({a:: 1}, "a", inc_hidden=false), null) &&

// any
std.assertEqual(std.any([]), false) &&
std.assertEqual(std.any([true, true, true]), true) &&
std.assertEqual(std.any([false, true, false]), true) &&
std.assertEqual(std.any([false, false, false]), false) &&

// all
std.assertEqual(std.all([]), true) &&
std.assertEqual(std.all([true, true, true]), true) &&
std.assertEqual(std.all([false, true, false]), false) &&
std.assertEqual(std.all([false, false, false]), false) &&

// isEmpty
std.assertEqual(std.isEmpty(""), true) &&
std.assertEqual(std.isEmpty("non-empty string"), false) &&

// trim
std.assertEqual(std.trim("already trimmed string"), "already trimmed string") &&
std.assertEqual(std.trim("    string with spaces on both ends     "), "string with spaces on both ends") &&
std.assertEqual(std.trim("string with newline character at end\n"), "string with newline character at end") &&
std.assertEqual(std.trim("string with tabs at end\t\t"), "string with tabs at end") &&

// xnor
std.assertEqual(std.xnor(false, true), false) &&
std.assertEqual(std.xnor(false, false), true) &&

// xor
std.assertEqual(std.xor(false, true), true) &&
std.assertEqual(std.xor(true, true), false) &&

// equalsIgnoreCase
std.assertEqual(std.equalsIgnoreCase("hello", "HELLO"), true) &&
std.assertEqual(std.equalsIgnoreCase("hello", "world"), false) &&

true

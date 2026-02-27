// Ported from sjsonnet StdWithKeyFTests (happy-path tests)

// setMember with keyF
std.assertEqual(std.setMember("a", std.set(["a", "b", "c"], function(x) x), function(x) x), true) &&
std.assertEqual(std.setMember("a", std.set(["a", "b", "c"])), true) &&
std.assertEqual(std.setMember("d", std.set(["a", "b", "c"], function(x) x), function(x) x), false) &&

std.assertEqual(
  local arr = std.set([
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Scala", version: "1.0" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  local testObj = { name: "TestObj", language: { name: "Java", version: "1.7" } };
  std.setMember(testObj, arr, function(x) x.language.name),
  true
) &&

// sort with keyF
std.assertEqual(std.sort(["a", "b", "c"]), ["a", "b", "c"]) &&
std.assertEqual(std.sort([1, 2, 3]), [1, 2, 3]) &&
std.assertEqual(std.sort([1, 2, 3], keyF=function(x) -x), [3, 2, 1]) &&
std.assertEqual(std.sort([1, 2, 3], function(x) -x), [3, 2, 1]) &&
std.assertEqual(std.sort([[1, 'b'], [2], [1], [], [1, 'a']]), [[], [1], [1, 'a'], [1, 'b'], [2]]) &&
std.assertEqual(
  std.sort([{a: [2]}, {a: []}, {a: [1]}, {a: [1, 'b']}, {a: [1, 'a']}], function(x) x.a),
  [{a: []}, {a: [1]}, {a: [1, 'a']}, {a: [1, 'b']}, {a: [2]}]
) &&

std.assertEqual(
  local arr = [
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Scala", version: "1.0" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ];
  std.sort(arr, function(x) x.language.name),
  [
    { language: { name: "C++", version: "n/a" }, name: "FooBar" },
    { language: { name: "Java", version: "1.8" }, name: "Foo" },
    { language: { name: "Scala", version: "1.0" }, name: "Bar" },
  ]
) &&

// sort on string
std.assertEqual(std.sort("lskdhdfjblksgh"), ["b", "d", "d", "f", "g", "h", "h", "j", "k", "k", "l", "l", "s", "s"]) &&

// uniq with keyF
std.assertEqual(std.uniq(["c", "c", "b", "b", "b", "a", "b", "a"]), ["c", "b", "a", "b", "a"]) &&
std.assertEqual(
  local arr = [
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Java", version: "1.7" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ];
  std.uniq(arr, function(x) x.language.name),
  [
    { language: { name: "Java", version: "1.8" }, name: "Foo" },
    { language: { name: "C++", version: "n/a" }, name: "FooBar" },
  ]
) &&

// set with keyF
std.assertEqual(std.set(["c", "c", "b", "b", "b", "a", "b", "a"]), ["a", "b", "c"]) &&
std.assertEqual(
  local arr = [
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Java", version: "1.7" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ];
  std.set(arr, function(x) x.language.name),
  [
    { language: { name: "C++", version: "n/a" }, name: "FooBar" },
    { language: { name: "Java", version: "1.8" }, name: "Foo" },
  ]
) &&

// setUnion with keyF
std.assertEqual(
  std.setUnion(std.set(["c", "c", "b"]), std.set(["b", "b", "a", "b", "a"])),
  ["a", "b", "c"]
) &&
std.assertEqual(
  std.setUnion(std.set([]), std.set(["b", "b", "a", "b", "a"])),
  ["a", "b"]
) &&
std.assertEqual(
  std.setUnion(std.set(["c", "c", "b"]), std.set([])),
  ["b", "c"]
) &&
std.assertEqual(
  local arr1 = std.set([
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Java", version: "1.7" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  local arr2 = std.set([
    { name: "Foo", language: { name: "Java", version: "12" } },
    { name: "Bar", language: { name: "Scala", version: "2.13" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  std.setUnion(arr1, arr2, function(x) x.language.name),
  [
    { language: { name: "C++", version: "n/a" }, name: "FooBar" },
    { language: { name: "Java", version: "1.8" }, name: "Foo" },
    { language: { name: "Scala", version: "2.13" }, name: "Bar" },
  ]
) &&

// setInter with keyF
std.assertEqual(std.setInter(["b", "c"], ["a", "b"]), ["b"]) &&
std.assertEqual(
  local arr1 = std.set([
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Java", version: "1.7" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  local arr2 = std.set([
    { name: "Foo", language: { name: "Java", version: "12" } },
    { name: "Bar", language: { name: "Scala", version: "2.13" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  std.setInter(arr1, arr2, function(x) x.language.name),
  [
    { language: { name: "C++", version: "n/a" }, name: "FooBar" },
    { language: { name: "Java", version: "1.8" }, name: "Foo" },
  ]
) &&

// setDiff with keyF
std.assertEqual(std.setDiff(["b", "c"], ["a", "b"]), ["c"]) &&
std.assertEqual(
  local arr1 = std.set([
    { name: "Foo", language: { name: "Java", version: "12" } },
    { name: "Bar", language: { name: "Scala", version: "2.13" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  local arr2 = std.set([
    { name: "Foo", language: { name: "Java", version: "1.8" } },
    { name: "Bar", language: { name: "Java", version: "1.7" } },
    { name: "FooBar", language: { name: "C++", version: "n/a" } },
  ], function(x) x.language.name);
  std.setDiff(arr1, arr2, function(x) x.language.name),
  [
    { language: { name: "Scala", version: "2.13" }, name: "Bar" },
  ]
) &&

true

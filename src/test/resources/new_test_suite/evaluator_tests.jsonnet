// Ported from sjsonnet EvaluatorTests.scala
//
// Error tests ported separately as error.*.jsonnet files:
//   array_bounds_empty, manifest_function, field_name_non_string, unknown_variable,
//   self_outside_object, dollar_outside_object, super_outside_object, error_non_string,
//   assert_inherited, assert_inherited_access, super_no_superclass
//
// Error tests also ported for duplicate fields and non-boolean conditions:
//   error.duplicate_field_comprehension, error.duplicate_field_computed, error.duplicate_field_mixed,
//   error.non_boolean_comprehension, error.filter_non_boolean, error.filtermap_non_boolean
//
local assertEqual = std.assertEqual;

// arithmetic
assertEqual(1 + 2 + 3, 6) &&
assertEqual(1 + 2 * 3, 7) &&
assertEqual(-1 + 2 * 3, 5) &&
assertEqual(6 - 3 + 2, 5) &&

// objects
assertEqual({x: 1}.x, 1) &&

// arrays
assertEqual([1, [2, 3], 4][1][0], 2) &&
assertEqual(([1, 2, 3] + [4, 5, 6])[3], 4) &&
assertEqual(std.slice(std.range(1, 4), 0, null, 2), [1, 3]) &&
assertEqual(std.slice(std.range(1, 4), null, null, 2), [1, 3]) &&
assertEqual(std.slice(std.range(1, 4), null, null, null), [1, 2, 3, 4]) &&
assertEqual(std.slice("jsonnet", -3, null, null), "net") &&
assertEqual(std.range(1, 4)[0::2], [1, 3]) &&

// functions
assertEqual((function(x) x)(1), 1) &&
assertEqual((function(a=1) a)(), 1) &&
assertEqual((function(x, y = x + 1) y)(x=10), 11) &&
assertEqual(local f(x) = function() true; f(42)() == true, true) &&
assertEqual({foo: (function() true)()}, {"foo": true}) &&

// members
assertEqual({local x = 1, x: x}["x"], 1) &&
assertEqual({local x(y) = y + "1", x: x("2")}["x"], "21") &&
assertEqual({local x(y) = y + "1", x: x(y="2")}["x"], "21") &&
assertEqual({local x(y="2") = y + "1", x: x()}["x"], "21") &&
assertEqual({[{local x = $.y + "lol", y: x, z: "1"}.z]: 2}["1"], 2) &&
assertEqual({local x = 1, y: {z: x}}.y.z, 1) &&

// extends
assertEqual((function(a) a.x + a.y)({x: 1}{y: 2}), 3) &&
assertEqual((function(a) a.x + a.y)({x: 1}{x: 2, y: 3}), 5) &&
assertEqual(({x: 1}{x+: 2}).x, 3) &&
assertEqual(({x+: 1}{x: 2}).x, 2) &&
assertEqual(({x+: 1}{x+: 2}).x, 3) &&
assertEqual(({x+: 1} + {x+: 2}).x, 3) &&
assertEqual((function(a, b) a + b)({x+: 1}, {x+: 2}).x, 3) &&
assertEqual(({a: [1]} + {a+: "1"}).a, "[1]1") &&
assertEqual(({a: 1} + {a+: "1"}).a, "11") &&
assertEqual(({a: "1"} + {a+: 1}).a, "11") &&

// ifElse
assertEqual(if true then 1 else 0, 1) &&
assertEqual(if 2 > 1 then 1 else 0, 1) &&
assertEqual(if false then 1 else 0, 0) &&
assertEqual(if 1 > 2 then 1 else 0, 0) &&

// self
assertEqual({x: 1, y: $.x + 10}.y, 11) &&
assertEqual({x: 1, y: self.x}.y, 1) &&
assertEqual({x: 1, y: {x: 2, z: $.x + 10}}.y.z, 11) &&
assertEqual({x: 1, y: {x: 2, z: self.x + 10}}.y.z, 12) &&
assertEqual({x: 1, y: {x: 0, y: self.x}.y}.y, 0) &&
assertEqual({x: 1, y: "x" in self}.y, true) &&
assertEqual({x: 1, y: "z" in self}.y, false) &&
assertEqual({y: "y" in self}.y, true) &&

// topLevel
assertEqual(local p(n="A") = {w: "H" + n}; {p: p()}.p.w, "HA") &&

// lazy
assertEqual([{x: $.y, y: $.x}.x, 2][1], 2) &&
assertEqual({x: $.y, y: $.x, local z(z0) = [3], w: z($.x)}.w[0], 3) &&
assertEqual((function(a=[1, b[1]], b=[a[0], 2]) [a, b])()[0][1], 2) &&

// comprehensions
assertEqual([x + 1 for x in [1, 2, 3]][2], 4) &&
assertEqual([x + 1, for x in [1, 2, 3]][2], 4) &&
assertEqual([x + y for x in [1, 2, 3] for y in [4, 5, 6] if x + y != 7][3], 8) &&
assertEqual({["" + x]: x * x for x in [1, 2, 3]}["3"], 9) &&
assertEqual({local y = $["2"], [x]: if x == "1" then y else 0, for x in ["1", "2"]}["1"], 0) &&
assertEqual({local a = 1, local b = a + 1, [k]: b + 1 for k in ["x"]}.x, 3) &&
assertEqual({local x2 = k * 2, [std.toString(k)]: x2 for k in [1]}["1"], 2) &&
assertEqual(
  local lib = {
    foo()::
      {
        local global = self,
        [iterParam]: global.base {
          foo: iterParam
        }
        for iterParam in ["foo"]
      },
  };
  ({ base:: {} } + lib.foo()),
  {"foo": {"foo": "foo"}}
) &&
assertEqual(
  local lib = {
    foo():: {
      local sx = super.x,
      [k]: sx + 1
      for k in ["x"]
    },
  };
  ({ x: 2 } + lib.foo()),
  {"x": 3}
) &&
assertEqual(
  local funcs = {[a]: (function(x) x * 2) for a in ["f1", "f2", "f3"]};
  funcs.f1(10),
  20
) &&
assertEqual(
  local funcs = {local y = b, [a]: (function(x) x * y) for a in ["f1", "f2", "f3"] for b in [10]};
  funcs.f1(10),
  100
) &&

// super - implicit
assertEqual(({x: 1, y: self.x} + {x: 2}).y, 2) &&
assertEqual(({local x = $.y, y: 1, z: x} + {y: 2}).z, 2) &&
assertEqual(({local x = self.y, y: 1, z: x} + {y: 2}).z, 2) &&
assertEqual(local A = {x: 1, local outer = self, y: A{z: outer}}; A.y.z.x, 1) &&
assertEqual({local x = self, y: 1, z: {a: x, y: 2}}.z.a.y, 1) &&
assertEqual(local A = {x: 1, local outer = self, y: A{x: outer.x}}; A.y.x, 1) &&
assertEqual(local A = {x: 1, local outer = self, y: A{x: outer.x + 1}}; A.y.y.x, 3) &&
assertEqual("a" in ({a: 1}{b: 2}), true) &&

// super - explicit
assertEqual(
  local x = {a: 1, b:: {c: 2}};
  local y = {b: super.a};
  (x + y).b,
  1
) &&
assertEqual(
  local x = {a: 1, b:: {c: 2}};
  x {a: super.a * 10, b:: {c: super.b.c * 10}},
  {"a": 10}
) &&

// hidden
assertEqual({i:: 1}, {}) &&
assertEqual({i:: 1} + {i: 2}, {}) &&
assertEqual({i: 1} + {i:: 2}, {}) &&
assertEqual({i:: 1} + {i:: 2}, {}) &&
assertEqual({i::: 1} + {i:: 2}, {}) &&
assertEqual({i:: 1} + {i::: 2}, {"i": 2}) &&
assertEqual({i: 1} + {i::: 2}, {"i": 2}) &&
assertEqual(local M = {x+: self.i, i:: 1}; {x: 1} + M, {"x": 2}) &&
assertEqual("%(hello)s" % {hello:: "world"}, "world") &&
assertEqual("%(hello)s" % {hello:: "world", bad:: error "lol"}, "world") &&

// shadowing
assertEqual(local x = 1; local x = 2; x, 2) &&
assertEqual(local x = 1; x + local x = 2; x, 3) &&
assertEqual(
  local str1 = |||
        text
    |||;
  local str1 = |||
        \n
    |||;
  (str1 == "\\n\n"),
  true
) &&

// stdLib
assertEqual(std.pow(2, 3), 8) &&
assertEqual(std.pow(x=2, n=3), 8) &&
assertEqual(std.pow(n=3, x=2), 8) &&
assertEqual(({a:: 1} + {a+::: 2}).a, 3) &&
assertEqual((std.prune({a:: 1}) + {a+::: 2}).a, 2) &&
assertEqual(std.toString(std.mapWithIndex(function(idx, elem) elem, [2, 1, 0])), "[2, 1, 0]") &&

// validParam
assertEqual(
  local Person(name="Alice") = {
    name: name,
    welcome: "Hello " + name + "!",
  };
  {person2: Person(name="Bob")}.person2.welcome,
  "Hello Bob!"
) &&

// equalDollar
assertEqual(local f(x) = x; {hello: 123, world: f(x=$.hello)}, {"hello": 123, "world": 123}) &&

// stdSubstr
assertEqual(std.substr("cookie", 6, 2), "") &&

// manifestIni
assertEqual(
  std.manifestIni({
    main: {a: "1", b: 2, c: true, d: null, e: [1, {"2": 2}, [3]], f: {"hello": "world"}},
    sections: {}
  }),
  "a = 1\nb = 2\nc = true\nd = null\ne = 1\ne = {\"2\": 2}\ne = [3]\nf = {\"hello\": \"world\"}\n"
) &&

// format
assertEqual("%s" % "world", "world") &&
assertEqual("%s" % ["world"], "world") &&
assertEqual("%s %s" % ["hello", "world"], "hello world") &&
assertEqual("%(hello)s" % {hello: "world"}, "world") &&
assertEqual("%()s %()s!" % ["Hello", "World"], "Hello World!") &&

// stdToString
assertEqual(std.toString({k: "v"}), "{\"k\": \"v\"}") &&

// floatFormatRegression
assertEqual("%.4f" % 0.01, "0.0100") &&
assertEqual("%05d" % 2, "00002") &&
assertEqual("%000d" % 2, "2") &&
assertEqual("%000d" % 2.123, "2") &&
assertEqual("%5d" % 2, "    2") &&
assertEqual("%5f" % 2, "2.000000") &&
assertEqual("%10d" % 2.123, "         2") &&
assertEqual("%+5.5f" % 123.456, "+123.45600") &&
assertEqual("%+5.5f" % -123.456, "-123.45600") &&
assertEqual("% 5.5f" % -123.456, "-123.45600") &&
assertEqual("%--+5.5f" % -123.456, "-123.45600") &&
assertEqual("%#-0- + 5.5f" % -123.456, "-123.45600") &&

// formatIntegerOverflow
assertEqual("%d" % 1e100, "10000000000000000159028911097599180468360808563945281389781327557747838772170381060813469985856815104") &&

// strict (only non-error cases - strict=false allows adjacent object literals)
assertEqual(({a: 1}{b: 2}).a, 1) &&
assertEqual(local x = {c: 3}; (x {a: 1}{b: 2}).a, 1) &&
assertEqual(local x = {c: 3}; (x {a: 1}).a, 1) &&

// objectDeclaration (+: in comprehension)
assertEqual({["foo"]: x for x in []}, {}) &&
assertEqual({["foo"]: x for x in [1]}, {"foo": 1}) &&
assertEqual({["foo"]+: x for x in []}, {}) &&
assertEqual({["foo"]+: x for x in [1]}, {"foo": 1}) &&
assertEqual(
  {["foo"]+: [x] for x in [1]} + {["foo"]+: [x] for x in [2]},
  {"foo": [1, 2]}
) &&

// givenNoDuplicateFieldsInListComprehension1_expectSuccess
assertEqual({["bar"]: x for x in [-876.89]}, {"bar": -876.89}) &&

// givenNoDuplicateFieldsInListComprehension2_expectSuccess
assertEqual({["bar_" + x]: x for x in [5, 12]}, {"bar_5": 5, "bar_12": 12}) &&

// dynamicDuplicateFields - lazy eval case that passes
assertEqual({x: {["k"]: 1, ["k"]: 2}, y: 1}.y, 1) &&

// functionEqualsNull
assertEqual(local f(x) = null; f == null, false) &&
assertEqual(local f = null; f == null, true) &&

// identifierStartsWithKeyword (expanded from Parser.keywords)
assertEqual(local assertFoo = 123; assertFoo, 123) &&
assertEqual(local elseFoo = 123; elseFoo, 123) &&
assertEqual(local errorFoo = 123; errorFoo, 123) &&
assertEqual(local falseFoo = 123; falseFoo, 123) &&
assertEqual(local forFoo = 123; forFoo, 123) &&
assertEqual(local functionFoo = 123; functionFoo, 123) &&
assertEqual(local ifFoo = 123; ifFoo, 123) &&
assertEqual(local importFoo = 123; importFoo, 123) &&
assertEqual(local importstrFoo = 123; importstrFoo, 123) &&
assertEqual(local inFoo = 123; inFoo, 123) &&
assertEqual(local localFoo = 123; localFoo, 123) &&
assertEqual(local nullFoo = 123; nullFoo, 123) &&
assertEqual(local tailstrictFoo = 123; tailstrictFoo, 123) &&
assertEqual(local thenFoo = 123; thenFoo, 123) &&
assertEqual(local selfFoo = 123; selfFoo, 123) &&
assertEqual(local superFoo = 123; superFoo, 123) &&
assertEqual(local trueFoo = 123; trueFoo, 123) &&
assertEqual(local importbinFoo = 123; importbinFoo, 123) &&
assertEqual({assertFoo: 123}, {"assertFoo": 123}) &&
assertEqual({elseFoo: 123}, {"elseFoo": 123}) &&
assertEqual({errorFoo: 123}, {"errorFoo": 123}) &&
assertEqual({falseFoo: 123}, {"falseFoo": 123}) &&
assertEqual({forFoo: 123}, {"forFoo": 123}) &&
assertEqual({functionFoo: 123}, {"functionFoo": 123}) &&
assertEqual({ifFoo: 123}, {"ifFoo": 123}) &&
assertEqual({importFoo: 123}, {"importFoo": 123}) &&
assertEqual({importstrFoo: 123}, {"importstrFoo": 123}) &&
assertEqual({inFoo: 123}, {"inFoo": 123}) &&
assertEqual({localFoo: 123}, {"localFoo": 123}) &&
assertEqual({nullFoo: 123}, {"nullFoo": 123}) &&
assertEqual({tailstrictFoo: 123}, {"tailstrictFoo": 123}) &&
assertEqual({thenFoo: 123}, {"thenFoo": 123}) &&
assertEqual({selfFoo: 123}, {"selfFoo": 123}) &&
assertEqual({superFoo: 123}, {"superFoo": 123}) &&
assertEqual({trueFoo: 123}, {"trueFoo": 123}) &&
assertEqual({importbinFoo: 123}, {"importbinFoo": 123}) &&

true

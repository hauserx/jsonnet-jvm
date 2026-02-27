// Ported from sjsonnet NonBooleanExprTests.scala
//
// Error tests ported as separate files:
//   error.non_boolean_comprehension.jsonnet — [x for x in [1,2,3] if "a"]
//   error.filter_non_boolean.jsonnet — std.filter with non-boolean predicate
//   error.filtermap_non_boolean.jsonnet — std.filterMap with non-boolean predicate
std.assertEqual(local boo(x) = true; [x for x in [1,2,3] if boo(x)], [1,2,3]) &&
std.assertEqual([x for x in [1,2,3] if x < 0], []) &&
true

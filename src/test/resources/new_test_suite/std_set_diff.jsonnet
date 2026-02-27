// Ported from sjsonnet StdSetDiffTests
// setDiff accepts a string as first arg, treating it as array of chars
std.assertEqual(
  { local options = ['a', 'b', 'c'], diff: std.setDiff('d', options) },
  { diff: ['d'] }
) &&

true

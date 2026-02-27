// Ported from sjsonnet StdSetUnionTests
// setUnion accepts a string as first arg, treating it as array of chars
std.assertEqual(
  { local options = ['a', 'b', 'c'], union: std.setUnion('d', options) },
  { union: ['a', 'b', 'c', 'd'] }
) &&

true

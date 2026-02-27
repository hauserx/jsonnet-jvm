// Ported from sjsonnet NativeXzTests.scala
local xz = std.native("xz");
local assertEqual = std.assertEqual;

assertEqual(xz([1, 2]), "/Td6WFoAAATm1rRGAgAhARYAAAB0L+WjAQABAQIAAADRC9qlUgJ94gABGgLcLqV+H7bzfQEAAAAABFla") &&
assertEqual(xz("hi"), "/Td6WFoAAATm1rRGAgAhARYAAAB0L+WjAQABaGkAAAD+qTgRvMqlSAABGgLcLqV+H7bzfQEAAAAABFla") &&
assertEqual(xz([1, 2], compressionLevel=0), "/Td6WFoAAATm1rRGAgAhAQwAAACPmEGcAQABAQIAAADRC9qlUgJ94gABGgLcLqV+H7bzfQEAAAAABFla") &&
assertEqual(xz("hi", compressionLevel=1), "/Td6WFoAAATm1rRGAgAhARAAAACocI6GAQABaGkAAAD+qTgRvMqlSAABGgLcLqV+H7bzfQEAAAAABFla") &&

true

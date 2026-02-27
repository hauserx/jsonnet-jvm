// Ported from sjsonnet UnicodeHandlingTests.scala
//
// Error tests ported as error.string_index_bounds, error.codepoint_empty/multi/multi_ascii.
// Skipped (no error in jsonnet-jvm): unpaired surrogate parse errors,
//   unpairedSurrogatesInEscapes high/low surrogate alone, invalidSurrogateHandling (all),
//   codepointVsUtf16OrderingDemonstration (uses Scala internals).
//
std.assertEqual(std.length("🌍"), 1) &&
std.assertEqual(std.length("Hello 🌍"), 7) &&
std.assertEqual(std.length("👨‍👩‍👧‍👦"), 7) &&
// stringLength

std.assertEqual("Hello 🌍 World"[6], "🌍") &&
std.assertEqual("A🌍B"[1], "🌍") &&
// stringIndex (evalErr: bounds error skipped)

std.assertEqual(std.codepoint("🌍"), 127757) &&
// codepoint (evalErr: empty, multi-char skipped)

std.assertEqual(std.char(127757), "🌍") &&
// char

std.assertEqual(std.stringChars("🌍"), ["🌍"]) &&
std.assertEqual(std.stringChars("Hello 🌍"), ["H", "e", "l", "l", "o", " ", "🌍"]) &&
// stringChars

std.assertEqual(std.map(function(x) std.codepoint(x), "🌍"), [127757]) &&
std.assertEqual(std.map(function(x) std.codepoint(x), "A🌍B"), [65, 127757, 66]) &&
// map

std.assertEqual(std.flatMap(function(x) x + "!", "🌍🚀"), "🌍!🚀!") &&
// flatMap

std.assertEqual(std.substr("A🌍B", 0, 1), "A") &&
std.assertEqual(std.substr("A🌍B", 1, 1), "🌍") &&
std.assertEqual(std.substr("A🌍B", 2, 1), "B") &&
std.assertEqual(std.substr("Hello 🌍 World", 6, 100), "🌍 World") &&
std.assertEqual(std.substr("🌍", 1, 5), "") &&
// substr

std.assertEqual("A🌍B"[0:1], "A") &&
std.assertEqual("A🌍B"[1:2], "🌍") &&
std.assertEqual("A🌍B🚀C"[0:5:2], "ABC") &&
std.assertEqual("ABC🚀"[-2:], "C🚀") &&
// stringSlice

std.assertEqual("\u0041", "A") &&
std.assertEqual("\u0048\u0065\u006C\u006C\u006F", "Hello") &&
std.assertEqual("\uD83C\uDF0D", "🌍") &&
std.assertEqual("\uD83D\uDE80", "🚀") &&
std.assertEqual("\uFFFF", "\uFFFF") &&
// unicodeEscapeSequences

std.assertEqual("\uFFFF" < "\uD800\uDC00", true) &&
std.assertEqual(std.sort(["\uD800\uDC00", "\uFFFF"]), ["\uFFFF", "\uD800\uDC00"]) &&
// codepointOrderingInJsonnet

std.assertEqual("\uD83D\uDE00", "😀") &&
std.assertEqual("\uD83C\uDF0D", "🌍") &&
// unpairedSurrogatesInEscapes — ported as error.surrogate_*.jsonnet

std.assertEqual(std.codepoint(std.char(55296)), 55296) &&
std.assertEqual(std.codepoint(std.char(56320)), 56320) &&
// stdCharPreservesRawSurrogates

std.assertEqual(std.char(127757), "🌍") &&
std.assertEqual(std.char(128640), "🚀") &&
std.assertEqual(std.char(65), "A") &&
std.assertEqual(std.char(65535), "\uFFFF") &&
// stdCharHandling

std.assertEqual("\uFFFF" < "\uD800\uDC00", true) &&
std.assertEqual("\uD800\uDC00" < "\uFFFF", false) &&
std.assertEqual("\uFFFF" <= "\uD800\uDC00", true) &&
std.assertEqual("\uD800\uDC00" <= "\uFFFF", false) &&
std.assertEqual("\uFFFF" <= "\uFFFF", true) &&
std.assertEqual("\uD800\uDC00" > "\uFFFF", true) &&
std.assertEqual("\uFFFF" > "\uD800\uDC00", false) &&
std.assertEqual("\uD800\uDC00" >= "\uFFFF", true) &&
std.assertEqual("\uFFFF" >= "\uD800\uDC00", false) &&
std.assertEqual("\uFFFF" >= "\uFFFF", true) &&
std.assertEqual("\uFFFF" == "\uD800\uDC00", false) &&
std.assertEqual("\uFFFF" != "\uD800\uDC00", true) &&
std.assertEqual("\uFFFF" == "\uFFFF", true) &&
std.assertEqual("\uFFFF" != "\uFFFF", false) &&
std.assertEqual(std.sort(["\uD800\uDC00", "\uFFFF", "Z", "A"]), ["A", "Z", "\uFFFF", "\uD800\uDC00"]) &&
// stringComparisons

(
  local testObject = {"\uD800\uDC00": 4, "\uFFFF": 3, "z": 2, "a": 1};
  std.assertEqual(std.objectFields(testObject), ["a", "z", "\uFFFF", "\uD800\uDC00"]) &&
  std.assertEqual(std.objectFieldsAll(testObject), ["a", "z", "\uFFFF", "\uD800\uDC00"]) &&
  std.assertEqual(std.manifestJsonMinified(testObject), "{\"a\":1,\"z\":2,\"\uFFFF\":3,\"\uD800\uDC00\":4}") &&
  std.assertEqual(std.manifestJson(testObject), "{\n   \"a\": 1,\n   \"z\": 2,\n   \"\uFFFF\": 3,\n   \"\uD800\uDC00\": 4\n}") &&
  std.assertEqual(std.manifestJsonEx(testObject, "  "), "{\n  \"a\": 1,\n  \"z\": 2,\n  \"\uFFFF\": 3,\n  \"\uD800\uDC00\": 4\n}") &&
  std.assertEqual(std.manifestTomlEx(testObject, "  "), "a = 1\nz = 2\n\"\uFFFF\" = 3\n\"\uD800\uDC00\" = 4")
) &&
// objectFieldOrdering

std.assertEqual(std.findSubstr("🌍", "Hello 🌍 World"), [6]) &&
std.assertEqual(std.findSubstr("o", "Hello 🌍 World"), [4, 9]) &&
std.assertEqual(std.findSubstr("🌍🚀", "🌍🚀 and more 🌍🚀"), [0, 12]) &&
// findSubstr

std.assertEqual(std.stripChars("Hello 🌍 World🌍H", "Hello 🌍"), "World") &&
std.assertEqual(std.lstripChars("Hello 🌍 World🌍H", "Hello 🌍"), "World🌍H") &&
std.assertEqual(std.rstripChars("Hello 🌍 World🌍H", "Hello 🌍"), "Hello 🌍 World") &&
std.assertEqual(std.rstripChars("hello🎉🎉🎉", "🎉"), "hello") &&
std.assertEqual(std.lstripChars("🎉🎉🎉hello", "🎉"), "hello") &&
std.assertEqual(std.stripChars("🎉🎉hello🎉🎉", "🎉"), "hello") &&
std.assertEqual(std.rstripChars("🌍 ", " "), "🌍") &&
std.assertEqual(std.trim("🌍   "), "🌍") &&
std.assertEqual(std.trim("   🌍   "), "🌍") &&
// stripChars

std.assertEqual(std.foldl(function(acc, c) acc + [c], "a😀b", []), ["a", "😀", "b"]) &&
std.assertEqual(std.foldl(function(acc, c) acc + 1, "a😀b", 0), 3) &&
std.assertEqual(std.foldl(function(acc, c) acc + [c], "🎉🔥", []), ["🎉", "🔥"]) &&
std.assertEqual(std.foldl(function(acc, c) acc + c, "a😀b", ""), "a😀b") &&
// foldl

std.assertEqual(std.foldr(function(c, acc) acc + [c], "a😀b", []), ["b", "😀", "a"]) &&
std.assertEqual(std.foldr(function(c, acc) acc + [c], "🎉🔥", []), ["🔥", "🎉"]) &&
std.assertEqual(std.foldr(function(c, acc) acc + c, "a😀b", ""), "b😀a") &&
// foldr

std.assertEqual(std.format("%c", [128512]), "😀") &&
std.assertEqual(std.format("%c", [128293]), "🔥") &&
std.assertEqual(std.format("%c", [127757]), "🌍") &&
std.assertEqual(std.format("%c", [65]), "A") &&
// formatPercentC

true

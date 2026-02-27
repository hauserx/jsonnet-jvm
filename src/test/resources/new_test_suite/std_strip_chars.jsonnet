// Ported from sjsonnet StdStripCharsTests

// rstripChars
std.assertEqual(std.rstripChars(" test test test ", " "), " test test test") &&
std.assertEqual(std.rstripChars("aaabbbbcccc", "ac"), "aaabbbb") &&
std.assertEqual(std.rstripChars("cacabbbbaacc", "ac"), "cacabbbb") &&
std.assertEqual(std.rstripChars("cacabbcacabbaacc", "ac"), "cacabbcacabb") &&
std.assertEqual(std.rstripChars("cacabbcacabb-aacc", "a-c"), "cacabbcacabb") &&
std.assertEqual(std.rstripChars("cacabbcacabb[aacc]", "ac[]$%^&*("), "cacabbcacabb") &&

// lstripChars
std.assertEqual(std.lstripChars(" test test test ", " "), "test test test ") &&
std.assertEqual(std.lstripChars("aaabbbbcccc", "ac"), "bbbbcccc") &&
std.assertEqual(std.lstripChars("cacabbcacabbaacc", "ac"), "bbcacabbaacc") &&
std.assertEqual(std.lstripChars("-cacabbcacabbaacc", "a-c"), "bbcacabbaacc") &&
std.assertEqual(std.lstripChars("[]aaabbbbcccc", "[ac]"), "bbbbcccc") &&

// stripChars
std.assertEqual(std.stripChars(" test test test ", " "), "test test test") &&
std.assertEqual(std.stripChars("aaabbbbcccc", "ac"), "bbbb") &&
std.assertEqual(std.stripChars("cacabbcacabbaacc", "ac"), "bbcacabb") &&
std.assertEqual(std.stripChars("c-acabbca-cabbaacc-", "a-c"), "bbca-cabb") &&
std.assertEqual(std.stripChars("[aaabbbbcccc]", "ac[]"), "bbbb") &&

true

// JDK 16+ changed the OS field in GZIP headers (JDK-8244706)
std.assertEqual(std.native('gzip')([1, 2]), 'H4sIAAAAAAAA/2NkAgCSQsy2AgAAAA==') &&
std.assertEqual(std.native('gzip')('hi'), 'H4sIAAAAAAAA/8vIBACsKpPYAgAAAA==') &&

// std.native for unknown names returns null
std.assertEqual(std.native('nonexistent'), null) &&

true

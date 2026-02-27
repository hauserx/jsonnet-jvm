// Ported from sjsonnet ParseYamlTests.scala
std.assertEqual(std.parseYaml('foo: bar'), {"foo": "bar"}) &&
std.assertEqual(std.parseYaml('- foo: bar'), [{"foo": "bar"}]) &&
std.assertEqual(std.parseYaml(''), null) &&
std.assertEqual(std.parseYaml("0777"), 511) &&
std.assertEqual(std.parseYaml('foo: bar\n---\nbar: baz\n'), [{"foo": "bar"}, {"bar": "baz"}]) &&
std.assertEqual(std.parseYaml('---  \nfoo: bar\n---\nbar: baz\n'), [{"foo": "bar"}, {"bar": "baz"}]) &&
std.assertEqual(std.parseYaml('---a: 1\nb---: 2\nc: 3---\nd: ---4'), {"---a": 1, "b---": 2, "c": "3---", "d": "---4"}) &&
std.assertEqual(
  std.parseYaml('{"a":"\\\"\\\\n\\n\\r\\f\\b\\t' + "\u263A" + '"}') {l: std.length(self.a)},
  {"a": '"\\n\n\r\f\b\t' + "\u263A", "l": 9}
) &&
true

local arr = ([ i < j for i in std.range(1, 1000) for j in std.range(1, 1000)]);
std.sum([if b then 1 else 0 for b in arr])
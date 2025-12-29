local arr = ([ i < j for i in std.range(1, 10000) for j in std.range(1, 5000)]);
std.length(arr)

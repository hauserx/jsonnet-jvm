package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import java.util.function.Function;

/** Math functions and constants for the std library. */
public class StdMathModule {

  public static void register(JObject std, JsonnetLanguage language) {
    // Constants
    std.addField("pi", () -> Math.PI);

    // Basic math
    std.addField(
        "floor", () -> fn(language, "floor", new String[] {"x"}, args -> Math.floor(num(args[0]))));
    std.addField(
        "ceil", () -> fn(language, "ceil", new String[] {"x"}, args -> Math.ceil(num(args[0]))));
    std.addField(
        "round",
        () -> fn(language, "round", new String[] {"x"}, args -> (double) Math.round(num(args[0]))));
    std.addField(
        "sqrt", () -> fn(language, "sqrt", new String[] {"x"}, args -> Math.sqrt(num(args[0]))));
    std.addField(
        "abs", () -> fn(language, "abs", new String[] {"n"}, args -> Math.abs(num(args[0]))));
    std.addField(
        "sign", () -> fn(language, "sign", new String[] {"n"}, args -> Math.signum(num(args[0]))));

    // Two-argument math
    std.addField(
        "max",
        () ->
            fn(
                language,
                "max",
                new String[] {"a", "b"},
                args -> Math.max(num(args[0]), num(args[1]))));
    std.addField(
        "min",
        () ->
            fn(
                language,
                "min",
                new String[] {"a", "b"},
                args -> Math.min(num(args[0]), num(args[1]))));
    std.addField(
        "pow",
        () ->
            fn(
                language,
                "pow",
                new String[] {"x", "n"},
                args -> Math.pow(num(args[0]), num(args[1]))));
    std.addField(
        "mod",
        () -> fn(language, "mod", new String[] {"a", "b"}, args -> num(args[0]) % num(args[1])));
    std.addField(
        "modulo",
        () -> fn(language, "modulo", new String[] {"a", "b"}, args -> num(args[0]) % num(args[1])));
    std.addField(
        "clamp",
        () ->
            fn(
                language,
                "clamp",
                new String[] {"x", "minVal", "maxVal"},
                args -> Math.max(num(args[1]), Math.min(num(args[0]), num(args[2])))));

    // Trigonometric
    std.addField(
        "sin", () -> fn(language, "sin", new String[] {"x"}, args -> Math.sin(num(args[0]))));
    std.addField(
        "cos", () -> fn(language, "cos", new String[] {"x"}, args -> Math.cos(num(args[0]))));
    std.addField(
        "tan", () -> fn(language, "tan", new String[] {"x"}, args -> Math.tan(num(args[0]))));
    std.addField(
        "asin", () -> fn(language, "asin", new String[] {"x"}, args -> Math.asin(num(args[0]))));
    std.addField(
        "acos", () -> fn(language, "acos", new String[] {"x"}, args -> Math.acos(num(args[0]))));
    std.addField(
        "atan", () -> fn(language, "atan", new String[] {"x"}, args -> Math.atan(num(args[0]))));
    std.addField(
        "atan2",
        () ->
            fn(
                language,
                "atan2",
                new String[] {"y", "x"},
                args -> Math.atan2(num(args[0]), num(args[1]))));
    std.addField(
        "hypot",
        () ->
            fn(
                language,
                "hypot",
                new String[] {"a", "b"},
                args -> Math.hypot(num(args[0]), num(args[1]))));

    // Angle conversion
    std.addField(
        "deg2rad",
        () -> fn(language, "deg2rad", new String[] {"x"}, args -> Math.toRadians(num(args[0]))));
    std.addField(
        "rad2deg",
        () -> fn(language, "rad2deg", new String[] {"x"}, args -> Math.toDegrees(num(args[0]))));

    // Logarithmic / Exponential
    std.addField(
        "log", () -> fn(language, "log", new String[] {"x"}, args -> Math.log(num(args[0]))));
    std.addField(
        "log2",
        () ->
            fn(
                language,
                "log2",
                new String[] {"x"},
                args -> Math.log(num(args[0])) / Math.log(2.0)));
    std.addField(
        "log10", () -> fn(language, "log10", new String[] {"x"}, args -> Math.log10(num(args[0]))));
    std.addField(
        "exp", () -> fn(language, "exp", new String[] {"x"}, args -> Math.exp(num(args[0]))));

    // Mantissa / Exponent (matching sjsonnet behavior)
    std.addField(
        "mantissa",
        () ->
            fn(
                language,
                "mantissa",
                new String[] {"x"},
                args -> {
                  double x = num(args[0]);
                  if (x == 0) return 0.0;
                  long exponent = (long) Math.floor(Math.log(Math.abs(x)) / Math.log(2) + 1);
                  return x * Math.pow(2.0, -exponent);
                }));
    std.addField(
        "exponent",
        () ->
            fn(
                language,
                "exponent",
                new String[] {"x"},
                args -> {
                  double x = num(args[0]);
                  if (x == 0) return 0.0;
                  return (double) (long) Math.floor(Math.log(Math.abs(x)) / Math.log(2) + 1);
                }));

    // Parity / Integer checks
    std.addField(
        "isEven",
        () ->
            fn(language, "isEven", new String[] {"x"}, args -> Math.round(num(args[0])) % 2 == 0));
    std.addField(
        "isOdd",
        () -> fn(language, "isOdd", new String[] {"x"}, args -> Math.round(num(args[0])) % 2 != 0));
    std.addField(
        "isInteger",
        () ->
            fn(
                language,
                "isInteger",
                new String[] {"x"},
                args -> {
                  double x = num(args[0]);
                  return (double) Math.round(x) == x;
                }));
    std.addField(
        "isDecimal",
        () ->
            fn(
                language,
                "isDecimal",
                new String[] {"x"},
                args -> {
                  double x = num(args[0]);
                  return (double) Math.round(x) != x;
                }));

    // Boolean logic
    std.addField(
        "xor",
        () -> fn(language, "xor", new String[] {"x", "y"}, args -> bool(args[0]) ^ bool(args[1])));
    std.addField(
        "xnor",
        () ->
            fn(
                language,
                "xnor",
                new String[] {"x", "y"},
                args -> !(bool(args[0]) ^ bool(args[1]))));
  }

  private static double num(Object arg) {
    if (arg instanceof Double) return (Double) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected Number, got " + JsonnetException.typeName(arg));
  }

  private static boolean bool(Object arg) {
    if (arg instanceof Boolean) return (Boolean) arg;
    throw new JsonnetException(
        "Wrong parameter type: expected Boolean, got " + JsonnetException.typeName(arg));
  }

  private static JFunction fn(
      JsonnetLanguage language, String name, String[] paramNames, Function<Object[], Object> body) {
    return new JFunction(
        name,
        new JsonnetBuiltinRootNode(language, name, body).getCallTarget(),
        null,
        paramNames.length,
        paramNames);
  }
}

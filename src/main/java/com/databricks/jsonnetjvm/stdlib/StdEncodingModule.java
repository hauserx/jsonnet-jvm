package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JNumberArray;
import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;
import com.databricks.jsonnetjvm.truffle.nodes.expressions.JsonnetBuiltinRootNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.function.Function;

/** Encoding and hashing functions for the std library. */
public class StdEncodingModule {

  public static void register(JObject std, JsonnetLanguage language) {
    // base64(input) -> string
    std.addField(
        "base64",
        () ->
            fn(
                language,
                "base64",
                new String[] {"input"},
                args -> {
                  Object input = args[0];
                  if (input instanceof String) {
                    return Base64.getEncoder()
                        .encodeToString(((String) input).getBytes(StandardCharsets.UTF_8));
                  } else if (input instanceof JArray) {
                    JArray arr = (JArray) input;
                    byte[] bytes = new byte[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                      Object item = arr.get(i);
                      if (!(item instanceof Double)) {
                        throw new JsonnetException("std.base64: expected number at position " + i);
                      }
                      int v = (int) ((Double) item).doubleValue();
                      if (v < 0 || v > 255) {
                        throw new JsonnetException(
                            "std.base64: byte value out of range at position " + i + ": " + v);
                      }
                      bytes[i] = (byte) v;
                    }
                    return Base64.getEncoder().encodeToString(bytes);
                  }
                  throw new JsonnetException("std.base64: expected string or array");
                }));

    // base64Decode(str) -> string
    std.addField(
        "base64Decode",
        () ->
            fn(
                language,
                "base64Decode",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  try {
                    return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
                  } catch (IllegalArgumentException e) {
                    throw new JsonnetException(
                        "std.base64Decode: invalid base64: " + e.getMessage());
                  }
                }));

    // base64DecodeBytes(str) -> array of numbers
    std.addField(
        "base64DecodeBytes",
        () ->
            fn(
                language,
                "base64DecodeBytes",
                new String[] {"str"},
                args -> {
                  String s = str(args[0]);
                  try {
                    byte[] bytes = Base64.getDecoder().decode(s);
                    double[] result = new double[bytes.length];
                    for (int i = 0; i < bytes.length; i++) {
                      result[i] = bytes[i] & 0xff;
                    }
                    return new JNumberArray(result);
                  } catch (IllegalArgumentException e) {
                    throw new JsonnetException(
                        "std.base64DecodeBytes: invalid base64: " + e.getMessage());
                  }
                }));

    // md5(s) -> string
    std.addField(
        "md5", () -> fn(language, "md5", new String[] {"s"}, args -> hash(str(args[0]), "MD5")));

    // sha1(s) -> string
    std.addField(
        "sha1",
        () -> fn(language, "sha1", new String[] {"str"}, args -> hash(str(args[0]), "SHA-1")));

    // sha256(s) -> string
    std.addField(
        "sha256",
        () -> fn(language, "sha256", new String[] {"str"}, args -> hash(str(args[0]), "SHA-256")));

    // sha512(s) -> string
    std.addField(
        "sha512",
        () -> fn(language, "sha512", new String[] {"str"}, args -> hash(str(args[0]), "SHA-512")));

    // sha3(s) -> string
    std.addField(
        "sha3",
        () -> fn(language, "sha3", new String[] {"str"}, args -> hash(str(args[0]), "SHA3-512")));
  }

  private static String hash(String input, String algorithm) {
    try {
      MessageDigest md = MessageDigest.getInstance(algorithm);
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b & 0xff));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new JsonnetException("Hash algorithm not available: " + algorithm);
    }
  }

  private static String str(Object arg) {
    if (arg instanceof String) return (String) arg;
    throw new JsonnetException(
        "Expected string, got: " + (arg == null ? "null" : arg.getClass().getSimpleName()));
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

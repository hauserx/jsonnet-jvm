package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.JsonnetEvaluator;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/** Native gzip function, matching sjsonnet's NativeGzip. */
public class NativeGzip {

  public static void register(JsonnetEvaluator evaluator) {
    evaluator.registerNative(
        "gzip",
        new String[] {"v"},
        args -> {
          Object v = args[0];
          if (v instanceof String s) {
            return gzipBytes(s.getBytes());
          } else if (v instanceof JGenericArray arr) {
            byte[] bytes = new byte[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
              Object elem = arr.get(i);
              if (elem instanceof Double d) {
                bytes[i] = d.byteValue();
              } else {
                throw new JsonnetException("gzip: array elements must be numbers");
              }
            }
            return gzipBytes(bytes);
          } else {
            throw new JsonnetException("Cannot gzip encode " + v.getClass().getSimpleName());
          }
        });
  }

  private static String gzipBytes(byte[] b) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
    try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
      gzip.write(b);
    } catch (Exception e) {
      throw new JsonnetException("gzip failed: " + e.getMessage());
    }
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }
}

package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.JsonnetEvaluator;
import com.databricks.jsonnetjvm.runtime.JGenericArray;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.databricks.jsonnetjvm.runtime.JsonnetException;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/** Native xz function, matching sjsonnet's NativeXz. */
public class NativeXz {

  public static void register(JsonnetEvaluator evaluator) {
    evaluator.registerNative(
        "xz",
        new String[] {"v", "compressionLevel"},
        args -> {
          Object v = args[0];
          Object levelArg = args.length > 1 ? args[1] : JNull.INSTANCE;

          int level = LZMA2Options.PRESET_DEFAULT;
          if (levelArg instanceof Double d) {
            level = d.intValue();
          } else if (!(levelArg instanceof JNull)) {
            throw new JsonnetException("Cannot xz encode with compression level " + levelArg);
          }

          if (v instanceof String s) {
            return xzBytes(s.getBytes(), level);
          } else if (v instanceof JGenericArray arr) {
            byte[] bytes = new byte[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
              Object elem = arr.get(i);
              if (elem instanceof Double d) {
                bytes[i] = d.byteValue();
              } else {
                throw new JsonnetException("xz: array elements must be numbers");
              }
            }
            return xzBytes(bytes, level);
          } else {
            throw new JsonnetException("Cannot xz encode " + JsonnetException.typeName(v));
          }
        });
  }

  private static String xzBytes(byte[] b, int compressionLevel) {
    ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
    try (XZOutputStream xz = new XZOutputStream(out, new LZMA2Options(compressionLevel))) {
      xz.write(b);
    } catch (Exception e) {
      throw new JsonnetException("xz failed: " + e.getMessage());
    }
    return Base64.getEncoder().encodeToString(out.toByteArray());
  }
}

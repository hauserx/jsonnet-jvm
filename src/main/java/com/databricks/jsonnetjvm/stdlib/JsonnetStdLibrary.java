package com.databricks.jsonnetjvm.stdlib;

import com.databricks.jsonnetjvm.runtime.JObject;
import com.databricks.jsonnetjvm.truffle.JsonnetLanguage;

/** Builds the global `std` object containing all standard library functions. */
public class JsonnetStdLibrary {

  public static JObject create(JsonnetLanguage language) {
    JObject std = new JObject();

    StdTypeModule.register(std, language);
    StdMathModule.register(std, language);
    StdArrayModule.register(std, language);
    StdStringModule.register(std, language);
    StdObjectModule.register(std, language);
    StdSetModule.register(std, language);
    StdEncodingModule.register(std, language);
    StdManifestModule.register(std, language);
    StdRuntimeModule.register(std, language);

    // All std fields are hidden per Jsonnet spec
    std.setAllFieldsHidden();

    return std;
  }
}

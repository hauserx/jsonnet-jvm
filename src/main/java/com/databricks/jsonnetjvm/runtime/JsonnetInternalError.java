package com.databricks.jsonnetjvm.runtime;

/**
 * Raised for internal interpreter errors that should never be seen by the user. Indicates a bug or
 * an unimplemented feature in the interpreter itself, not a problem with the Jsonnet program.
 */
public class JsonnetInternalError extends RuntimeException {
  public JsonnetInternalError(String message) {
    super(message);
  }

  public JsonnetInternalError(String message, Throwable cause) {
    super(message, cause);
  }
}

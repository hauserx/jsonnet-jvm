package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;

/**
 * Raised for compile-time/static errors detected during parsing or AST building: unknown variables,
 * self/super used outside objects, and ANTLR syntax errors. Corresponds to sjsonnet's StaticError.
 */
@ExportLibrary(InteropLibrary.class)
public class JsonnetStaticError extends AbstractTruffleException {
  public JsonnetStaticError(String message) {
    super(message);
  }

  public JsonnetStaticError(String message, Node location) {
    super(message, location);
  }

  @ExportMessage
  ExceptionType getExceptionType() {
    return ExceptionType.PARSE_ERROR;
  }
}

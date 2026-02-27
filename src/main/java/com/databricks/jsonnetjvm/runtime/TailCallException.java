package com.databricks.jsonnetjvm.runtime;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Thrown by a tailstrict call inside a trampoline to signal that the caller should retry with the
 * new function and arguments instead of growing the stack.
 */
public final class TailCallException extends ControlFlowException {

  private static final long serialVersionUID = 1L;

  private JFunction function;
  private Object[] args;

  public TailCallException(JFunction function, Object[] args) {
    this.function = function;
    this.args = args;
  }

  public JFunction getFunction() {
    return function;
  }

  public Object[] getArgs() {
    return args;
  }

  public void set(JFunction function, Object[] args) {
    this.function = function;
    this.args = args;
  }
}

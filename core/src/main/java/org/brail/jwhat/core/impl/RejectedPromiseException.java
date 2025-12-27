package org.brail.jwhat.core.impl;

public class RejectedPromiseException extends Exception {
  private final Object value;

  public RejectedPromiseException(Object value) {
    super("Rejected promise");
    this.value = value;
  }

  public Object getValue() {
    return value;
  }
}

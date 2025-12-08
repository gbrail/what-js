package org.brail.jwhat.core.impl;

public class Result<T> {
  private final T result;
  private final String error;

  private Result(T result, String error) {
    this.result = result;
    this.error = error;
  }

  public static <T> Result<T> success(T result) {
    return new Result<>(result, null);
  }

  public static <T> Result<T> failure(String error) {
    assert error != null;
    return new Result<>(null, error);
  }

  public boolean isSuccess() {
    return error == null;
  }

  public boolean isFailure() {
    return error != null;
  }

  public T get() {
    assert error == null;
    return result;
  }

  public String error() {
    assert error != null;
    return error;
  }
}

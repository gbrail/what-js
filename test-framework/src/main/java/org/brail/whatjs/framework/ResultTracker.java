package org.brail.whatjs.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class ResultTracker {
  private final ArrayList<Result> results = new ArrayList<>();
  private String finalMessage;
  private Optional<FinalStatus> finalStatus = Optional.empty();

  public Function getResultCallback(Scriptable scope) {
    return new LambdaFunction(
        scope,
        "resultCallback",
        1,
        (Context cx, Scriptable ls, Scriptable thisObj, Object[] args) -> {
          assert args.length >= 1;
          var result = new Result(cx, (Scriptable) args[0]);
          results.add(result);
          return Undefined.instance;
        });
  }

  public Function getCompletionCallback(Scriptable scope) {
    return new LambdaFunction(
        scope,
        "completionCallback",
        2,
        (Context cx, Scriptable ls, Scriptable thisObj, Object[] args) -> {
          assert args.length >= 2;
          Scriptable ts = (Scriptable) args[1];
          finalStatus = Optional.of(toFinalStatus(getInt(ts, "status")));
          finalMessage = getString(ts, "message");
          return Undefined.instance;
        });
  }

  public List<Result> getResults() {
    return results;
  }

  public Optional<FinalStatus> getFinalStatus() {
    return finalStatus;
  }

  public String getFinalMessage() {
    return finalMessage;
  }

  public Supplier<String> getFailureReason() {
    return () -> {
      if (finalStatus.isPresent() && finalStatus.get() != FinalStatus.OK) {
        return "Test harness failed: " + finalStatus.get() + ": " + finalMessage;
      }

      var b = new StringBuilder();
      for (var r : results) {
        b.append(r).append('\n');
      }
      return b.toString();
    };
  }

  // Return true only if harness ran successfully and all tests passed.
  public boolean success() {
    if (finalStatus.isEmpty() || finalStatus.get() != FinalStatus.OK) {
      return false;
    }
    return results.stream().allMatch(r -> r.status == Status.PASS);
  }

  private static String getString(Scriptable o, String name) {
    Object val = o.get(name, o);
    return (val == Scriptable.NOT_FOUND || val == null) ? "" : Context.toString(val);
  }

  private static int getInt(Scriptable o, String name) {
    Object val = o.get(name, o);
    if (val == Scriptable.NOT_FOUND || val == null) {
      return 0;
    }
    return (int) Context.toNumber(val);
  }

  private static Status toStatus(int i) {
    switch (i) {
      case 0:
        return Status.PASS;
      case 1:
        return Status.FAIL;
      case 2:
        return Status.TIMEOUT;
      case 3:
        return Status.NOTRUN;
      case 4:
        return Status.PRECONDITION_FAILED;
      default:
        throw new IllegalArgumentException("Unknown result status: " + i);
    }
  }

  private static FinalStatus toFinalStatus(int i) {
    switch (i) {
      case 0:
        return FinalStatus.OK;
      case 1:
        return FinalStatus.ERROR;
      case 2:
        return FinalStatus.TIMEOUT;
      case 3:
        return FinalStatus.PRECONDITION_FAILED;
      default:
        throw new IllegalArgumentException("Unknown final status: " + i);
    }
  }

  public enum FinalStatus {
    OK,
    ERROR,
    TIMEOUT,
    PRECONDITION_FAILED,
  }

  public enum Status {
    PASS,
    FAIL,
    TIMEOUT,
    NOTRUN,
    PRECONDITION_FAILED,
  }

  public static class Result {
    final String name;
    final int phase;
    final Status status;
    final String message;
    final String stack;

    public Result(Context cx, Scriptable o) {
      name = getString(o, "name");
      phase = getInt(o, "phase");
      status = toStatus(getInt(o, "status"));
      message = getString(o, "message");
      stack = getString(o, "stack");
    }

    @Override
    public String toString() {
      var b = new StringBuilder();
      b.append(name).append(": ").append(status);
      if (!message.isEmpty()) {
        b.append(" (").append(message).append(')');
      }
      return b.toString();
    }
  }
}

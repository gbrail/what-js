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

  /**
   * Return a function that can be passed to add_result_callback in the test harness. The function
   * takes an argument of the "Test" class from the harness and extracts the test result from that.
   */
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

  /**
   * Return a function that can be passed to add_completion_callback in the test harness. The first
   * argument is an array of Test objects and the second is a TestsStatus object that we parse to
   * see if the harness worked.
   */
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

  /** Generate a description of the test results. This may be used in a JUnit assertion. */
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

  /** Return true only if harness ran successfully and all tests passed. */
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
    return switch (i) {
      case 0 -> Status.PASS;
      case 1 -> Status.FAIL;
      case 2 -> Status.TIMEOUT;
      case 3 -> Status.NOTRUN;
      case 4 -> Status.PRECONDITION_FAILED;
      default -> throw new IllegalArgumentException("Unknown result status: " + i);
    };
  }

  private static FinalStatus toFinalStatus(int i) {
    return switch (i) {
      case 0 -> FinalStatus.OK;
      case 1 -> FinalStatus.ERROR;
      case 2 -> FinalStatus.TIMEOUT;
      case 3 -> FinalStatus.PRECONDITION_FAILED;
      default -> throw new IllegalArgumentException("Unknown final status: " + i);
    };
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

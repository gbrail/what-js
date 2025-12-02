package org.brail.jwhat.console;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

public class Console extends ScriptableObject {
  public enum Level {
    LOG,
    DEBUG,
    INFO,
    WARN,
    ERROR
  }

  private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
  private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);

  private final Printer printer;
  private final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Instant> timers = new ConcurrentHashMap<>();

  public static class Builder {
    private Printer printer = null;

    private Builder() {}

    public Builder printer(Printer printer) {
      this.printer = printer;
      return this;
    }

    public void install(Context cx, Scriptable scope) {
      if (printer == null) {
        printer = new StdoutPrinter();
      }
      var c = new Console(printer);
      c.init(cx, scope);
    }
  }

  private Console(Printer printer) {
    this.printer = printer;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void init(Context cx, Scriptable scope) {
    setParentScope(scope);
    setPrototype(cx.newObject(scope));
    defineProperty(
        SymbolKey.TO_STRING_TAG, "console", ScriptableObject.DONTENUM | ScriptableObject.READONLY);
    defineProperty(
        "assert",
        new LambdaFunction(
            this,
            "assert",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> assertImpl(args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "debug",
        new LambdaFunction(
            this,
            "debug",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                logImpl("debug", args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "error",
        new LambdaFunction(
            this,
            "error",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                logImpl("error", args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "info",
        new LambdaFunction(
            this,
            "info",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                logImpl("info", args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "log",
        new LambdaFunction(
            this,
            "log",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                logImpl("log", args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "warn",
        new LambdaFunction(
            this,
            "warn",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                logImpl("warn", args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "count",
        new LambdaFunction(
            this,
            "count",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> count(args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "countReset",
        new LambdaFunction(
            this,
            "countReset",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> countReset(args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "time",
        new LambdaFunction(
            this,
            "time",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> time(args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "timeLog",
        new LambdaFunction(
            this,
            "timeLog",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> timeLog(args)),
        ScriptableObject.DONTENUM);
    defineProperty(
        "timeEnd",
        new LambdaFunction(
            this,
            "timeEnd",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> timeEnd(args)),
        ScriptableObject.DONTENUM);
    ScriptableObject.defineProperty(scope, "console", this, ScriptableObject.DONTENUM);
  }

  @Override
  public String getClassName() {
    return "console";
  }

  private Object assertImpl(Object[] args) {
    if (args.length > 0) {
      if (!ScriptRuntime.toBoolean(args[0])) {
        if (args.length == 1) {
          logString("assert", "assertion failed");
        } else {
          Object[] logArgs;
          if (args[1] instanceof CharSequence) {
            logArgs = new Object[args.length - 1];
            System.arraycopy(args, 1, logArgs, 0, logArgs.length);
            logArgs[0] = "assertion failed: " + logArgs[0];
          } else {
            logArgs = new Object[args.length];
            System.arraycopy(args, 1, logArgs, 1, args.length - 1);
            logArgs[0] = "assertion failed";
          }
          logImpl("assert", logArgs);
        }
      }
    }
    return Undefined.instance;
  }

  private Object count(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      Integer newVal =
          counts.compute(
              label,
              (k, oldVal) -> {
                if (oldVal == null) {
                  return 1;
                }
                return oldVal + 1;
              });
      logString("count", label + ": " + newVal);
    }
    return Undefined.instance;
  }

  private Object countReset(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      if (counts.remove(label) == null) {
        logString("warn", label + ": not found");
      }
    }
    return Undefined.instance;
  }

  private Object time(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      if (timers.putIfAbsent(label, Instant.now()) != null) {
        logString("warn", label + ": already exists");
      }
    }
    return Undefined.instance;
  }

  private Object timeLog(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      var start = timers.get(label);
      if (start != null) {
        var d = Duration.between(start, Instant.now());
        String msg = label + ": " + formatDuration(d);
        if (args.length == 1) {
          logString("timeLog", msg);
        } else {
          Object[] logArgs = new Object[args.length];
          System.arraycopy(args, 1, logArgs, 1, args.length - 1);
          logArgs[0] = msg;
          logImpl("timeLog", logArgs);
        }
      } else {
        logString("warn", label + ": not found");
      }
    }
    return Undefined.instance;
  }

  private Object timeEnd(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      var start = timers.remove(label);
      if (start != null) {
        var d = Duration.between(start, Instant.now());
        logString("timeEnd", label + ": " + formatDuration(d));
      } else {
        logString("warn", label + ": not found");
      }
    }
    return Undefined.instance;
  }

  private Object logImpl(String kind, Object[] args) {
    if (args.length > 0) {
      var level = mapLevel(kind);
      if (args.length == 1) {
        printer.print(level, kind, Formatter.formatFormatString(args[0]));
      } else {
        var r = Formatter.format(args);
        printer.print(level, kind, r.getFormatted(), r.getRemaining());
      }
    }
    return Undefined.instance;
  }

  private void logString(String kind, String msg) {
    var level = mapLevel(kind);
    printer.print(level, kind, msg);
  }

  private static String formatDuration(Duration d) {
    if (d.compareTo(FIVE_MINUTES) > 0) {
      double minutes = d.getSeconds() / 60.0 + (d.getNano() / 1_000_000_000.0 / 60.0);
      return String.format("%.3fmin", minutes);
    }
    if (d.compareTo(FIVE_SECONDS) > 0) {
      double secs = d.getSeconds() + (d.getNano() / 1_000_000_000.0);
      return String.format("%.3fs", secs);
    }
    double millis = d.toNanos() / 1_000_000.0;
    return String.format("%.3fms", millis);
  }

  private static Level mapLevel(String kind) {
    return switch (kind) {
      case "debug" -> Level.DEBUG;
      case "error", "assert" -> Level.ERROR;
      case "info", "count", "timeEnd" -> Level.INFO;
      case "warn" -> Level.WARN;
      default -> Level.LOG;
    };
  }
}

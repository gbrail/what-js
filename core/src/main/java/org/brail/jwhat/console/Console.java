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

  private final Printer printer = new StdoutPrinter();
  private final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Instant> timers = new ConcurrentHashMap<>();

  public static void init(Context cx, Scriptable scope) {
    Console c = new Console();
    c.setParentScope(scope);
    c.setPrototype(cx.newObject(scope));
    c.defineProperty(
        SymbolKey.TO_STRING_TAG, "console", ScriptableObject.DONTENUM | ScriptableObject.READONLY);
    c.defineProperty(
        "debug",
        new LambdaFunction(
            c,
            "debug",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                c.logImpl(Level.DEBUG, args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "error",
        new LambdaFunction(
            c,
            "error",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                c.logImpl(Level.ERROR, args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "info",
        new LambdaFunction(
            c,
            "info",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                c.logImpl(Level.INFO, args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "log",
        new LambdaFunction(
            c,
            "log",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                c.logImpl(Level.LOG, args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "warn",
        new LambdaFunction(
            c,
            "warn",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) ->
                c.logImpl(Level.WARN, args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "count",
        new LambdaFunction(
            c,
            "count",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> c.count(args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "countReset",
        new LambdaFunction(
            c,
            "countReset",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> c.countReset(args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "time",
        new LambdaFunction(
            c,
            "time",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> c.time(args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "timeLog",
        new LambdaFunction(
            c,
            "timeLog",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> c.timeLog(args)),
        ScriptableObject.DONTENUM);
    c.defineProperty(
        "timeEnd",
        new LambdaFunction(
            c,
            "timeEnd",
            1,
            (Context lcx, Scriptable ls, Scriptable thisObj, Object[] args) -> c.timeEnd(args)),
        ScriptableObject.DONTENUM);
    ScriptableObject.defineProperty(scope, "console", c, ScriptableObject.DONTENUM);
  }

  @Override
  public String getClassName() {
    return "console";
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
      logString(Level.INFO, label + ": " + newVal);
    }
    return Undefined.instance;
  }

  private Object countReset(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      if (counts.remove(label) == null) {
        logString(Level.WARN, label + ": not found");
      }
    }
    return Undefined.instance;
  }

  private Object time(Object[] args) {
    if (args.length > 0) {
      String label = ScriptRuntime.toString(args[0]);
      if (timers.computeIfAbsent(label, (k) -> Instant.now()) != null) {
        logString(Level.WARN, label + ": already exists");
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
        String msg = label + ": " + d;
        Object[] logArgs = new Object[args.length + 1];
        logArgs[0] = msg;
        System.arraycopy(args, 0, logArgs, 1, args.length);
        logImpl(Level.LOG, logArgs);
      } else {
        logString(Level.WARN, label + ": does not exist");
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
        logString(Level.INFO, label + ": " + d);
      } else {
        logString(Level.WARN, label + ": does not exist");
      }
    }
    return Undefined.instance;
  }

  private Object logImpl(Level level, Object[] args) {
    if (args.length > 0) {
      if (args.length == 1) {
        printer.print(level, Formatter.formatFormatString(args[0]));
      } else {
        var r = Formatter.format(args);
        printer.print(level, r.getFormatted(), r.getRemaining());
      }
    }
    return Undefined.instance;
  }

  private void logString(Level level, String msg) {
    printer.print(level, msg);
  }
}

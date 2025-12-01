package org.brail.jwhat.console;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
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
    ScriptableObject.defineProperty(scope, "console", c, ScriptableObject.DONTENUM);
  }

  @Override
  public String getClassName() {
    return "console";
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
}

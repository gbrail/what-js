package org.brail.jwhat.console;

import java.util.regex.Pattern;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Symbol;

public class Formatter {
  private static final Object[] emptyArgs = new Object[] {};
  private static final Pattern SPECIFIERS = Pattern.compile("%s|%d|%i|%f|%o|%O|%c");

  public static class Result {
    String formatted;
    Object[] remainingArgs;

    private Result(String formatted, Object[] remainingArgs) {
      this.formatted = formatted;
      this.remainingArgs = remainingArgs;
    }

    public String getFormatted() {
      return formatted;
    }

    public Object[] getRemaining() {
      return remainingArgs;
    }
  }

  public static String formatFormatString(Object o) {
    if (o instanceof Symbol) {
      return o.toString();
    }
    return ScriptRuntime.toString(o);
  }

  public static Result format(Object[] args) {
    assert args.length > 0;
    var formatted = new StringBuilder();
    int current = 1;
    int start = 0;
    String format = formatFormatString(args[0]);
    var m = SPECIFIERS.matcher(format);
    while (current < args.length && m.find()) {
      if (m.start() > start) {
        // Move un-matched stuff to the target
        formatted.append(format.substring(start, m.start()));
      }
      // Append the formatted arg
      formatted.append(formatArg(m.group(), args[current]));
      current++;
      start = m.end();
    }
    // Append any remaining text
    if (start < format.length()) {
      formatted.append(format.substring(start));
    }
    // Return any remaining args
    if (current < args.length) {
      Object[] remaining = new Object[args.length - current];
      System.arraycopy(args, current, remaining, 0, remaining.length);
      return new Result(formatted.toString(), remaining);
    }
    return new Result(formatted.toString(), emptyArgs);
  }

  private static String formatArg(String spec, Object arg) {
    switch (spec) {
      case "%s":
        return ScriptRuntime.toString(arg);
      case "%d":
      case "%i":
        // TODO need ScriptRuntime.isSymbol
        if (arg instanceof Symbol) {
          return "NaN";
        }
        var d = ScriptRuntime.toNumber(arg);
        if (Double.isFinite(d)) {
          return ScriptRuntime.toString(ScriptRuntime.toInt32(d));
        }
        return ScriptRuntime.toString(d);
      case "%f":
        // TODO need ScriptRuntime.isSymbol
        if (arg instanceof Symbol) {
          return "NaN";
        }
        return ScriptRuntime.toString(ScriptRuntime.toNumber(arg));
      case "%o":
      case "%O":
        return arg.toString();
      default:
        throw new AssertionError("Invalid spec " + spec);
    }
  }
}

package org.brail.jwhat.framework;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class MinimalConsole extends ScriptableObject {
  public static Scriptable init(Context cx, Scriptable scope) {
    Scriptable c = cx.newObject(scope);
    ScriptableObject.defineProperty(
        c,
        "log",
        new LambdaFunction(scope, "log", 1, MinimalConsole::log),
        ScriptableObject.DONTENUM);
    ScriptableObject.defineProperty(
        c,
        "debug",
        new LambdaFunction(scope, "debug", 1, MinimalConsole::log),
        ScriptableObject.DONTENUM);
    return c;
  }

  @Override
  public String getClassName() {
    return "Console";
  }

  private static Object log(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    StringBuilder msg = new StringBuilder();
    boolean first = true;
    for (Object arg : args) {
      if (first) {
        first = false;
      } else {
        msg.append(' ');
      }
      msg.append(Context.toString(arg));
    }
    System.out.println(msg);
    return Undefined.instance;
  }
}

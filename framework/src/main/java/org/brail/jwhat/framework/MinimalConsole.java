package org.brail.jwhat.framework;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

public class MinimalConsole extends ScriptableObject {
  public static Scriptable init(Context cx, VarScope scope) {
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

  private static Object log(Context cx, VarScope scope, Object thisObj, Object[] args) {
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

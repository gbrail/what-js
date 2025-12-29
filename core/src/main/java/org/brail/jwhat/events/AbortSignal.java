package org.brail.jwhat.events;

import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class AbortSignal extends ScriptableObject {
  private boolean aborted;
  Object reason = Undefined.instance;

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "AbortSignal", 0, AbortSignal::constructor);
    c.defineConstructorMethod(scope, "abort", 1, (lcx, ls, to, args) -> abort(lcx, ls, args, c));
    ScriptableObject.defineProperty(scope, "AbortSignal", c, DONTENUM);
  }

  private AbortSignal() {}

  @Override
  public String getClassName() {
    return "AbortSignal";
  }

  private static AbortSignal realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, AbortSignal.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new AbortSignal();
  }

  static Object getAbortReason(Object[] args) {
    Object reason;
    if (args.length < 1 || Undefined.isUndefined(args[0])) {
      // TODO should be a "DOM Exception"
      return "AbortError";
    } else {
      return args[0];
    }
  }

  static Object getAbortReason(Object arg) {
    Object reason;
    if (Undefined.isUndefined(arg)) {
      // TODO should be a "DOM Exception"
      return "AbortError";
    } else {
      return arg;
    }
  }

  private static Object abort(
      Context cx, Scriptable scope, Object[] args, Constructable constructor) {
    var s = (AbortSignal) constructor.construct(cx, scope, args);
    s.reason = getAbortReason(args);
    return s;
  }
}

package org.brail.jwhat.events;

import java.util.ArrayList;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class AbortSignal extends ScriptableObject {
  Object reason = Undefined.instance;
  Scriptable onAbort;
  private final ArrayList<Callable> steps = new ArrayList<>();

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "AbortSignal", 0, AbortSignal::constructor);
    c.defineConstructorMethod(scope, "abort", 1, (lcx, ls, to, args) -> abort(lcx, ls, args, c));
    c.definePrototypeProperty(cx, "aborted", AbortSignal::isAborted);
    c.definePrototypeProperty(cx, "reason", AbortSignal::getReason);
    c.definePrototypeProperty(cx, "onabort", AbortSignal::getOnAbort, AbortSignal::setOnAbort);
    c.definePrototypeMethod(scope, "throwIfAborted", 0, AbortSignal::throwIfAborted);
    ScriptableObject.defineProperty(scope, "AbortSignal", c, DONTENUM);
  }

  private AbortSignal() {}

  @Override
  public String getClassName() {
    return "AbortSignal";
  }

  public boolean isAborted() {
    return reason != Undefined.instance;
  }

  void addStep(Callable step) {
    steps.add(step);
  }

  private static AbortSignal realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, AbortSignal.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new AbortSignal();
  }

  private static Object isAborted(Scriptable thisObj) {
    return realThis(thisObj).isAborted();
  }

  private static Object getReason(Scriptable thisObj) {
    return realThis(thisObj).reason;
  }

  private static Object getOnAbort(Scriptable thisObj) {
    return realThis(thisObj).onAbort;
  }

  private static void setOnAbort(Scriptable thisObj, Object val) {
    if (val instanceof Scriptable s) {
      realThis(thisObj).onAbort = s;
    }
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

  private static Object throwIfAborted(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.reason != Undefined.instance) {
      throw new JavaScriptException(self.reason);
    }
    return Undefined.instance;
  }
}

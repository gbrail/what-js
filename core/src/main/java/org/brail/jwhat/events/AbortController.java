package org.brail.jwhat.events;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class AbortController extends ScriptableObject {
  private final AbortSignal signal;

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "AbortController", 0, AbortController::constructor);
    c.definePrototypeProperty(cx, "signal", AbortController::getSignal);
    c.definePrototypeMethod(scope, "abort", 1, AbortController::abort);
    ScriptableObject.defineProperty(scope, "AbortController", c, DONTENUM);
  }

  private AbortController(AbortSignal signal) {
    this.signal = signal;
  }

  @Override
  public String getClassName() {
    return "AbortController";
  }

  private static AbortController realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, AbortController.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    var signal = (AbortSignal) cx.newObject(scope, "AbortSignal");
    return new AbortController(signal);
  }

  private static Object abort(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    Object arg = args.length > 0 ? args[0] : Undefined.instance;
    realThis(thisObj).abort(arg);
    return Undefined.instance;
  }

  public void abort(Object arg) {
    signal.reason = AbortSignal.getAbortReason(arg);
    // TODO certainly quite a bit!
  }

  private static Object getSignal(Scriptable thisObj) {
    return realThis(thisObj).getSignal();
  }

  public Object getSignal() {
    return signal;
  }
}

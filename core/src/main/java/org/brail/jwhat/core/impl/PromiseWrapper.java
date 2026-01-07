package org.brail.jwhat.core.impl;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/** PromiseWrapper adapts a Rhino Promise so that we can take action when it is resolved. */
public class PromiseWrapper {
  private final NativePromise promise;

  public interface ResultCallback {
    void deliver(Context cx, Scriptable scope, Object result);
  }

  private PromiseWrapper(NativePromise p) {
    this.promise = p;
  }

  /**
   * Wrap a Promise or an ordinary object. If a promise, then the adapter may be used with that
   * promise. If a regular value, then the adapter will be backed by a promise that is already
   * resolved.
   */
  public static PromiseWrapper wrap(Context cx, Scriptable scope, Object val) {
    if (val instanceof NativePromise pv) {
      return new PromiseWrapper(pv);
    }
    return new PromiseWrapper(PromiseAdapter.newResolvedPromise(cx, scope, val));
  }

  /**
   * Wrap a function call that's supposed to return a promise. Return a
   * rejected promise if it throws an exception.
   */
  public static PromiseWrapper wrapCall(Context cx, Scriptable scope,
                                        Scriptable thisObj, Object[] args,
                                        Callable f) {
    try {
      var result = f.call(cx, scope, thisObj, args);
      return wrap(cx, scope, result);
    } catch (JavaScriptException jse) {
      return new PromiseWrapper(PromiseAdapter.newRejectedPromise(cx, scope, jse.getValue()));
    }
  }

  /** Register a callback that will be called when the promise resolves. */
  public void then(Context cx, Scriptable scope, ResultCallback cb) {
    var resolve =
        new LambdaFunction(
            scope,
            "resolve",
            1,
            (lcx, ls, to, args) -> {
              var val = args.length > 0 ? args[0] : Undefined.instance;
              cb.deliver(lcx, ls, val);
              return Undefined.instance;
            });
    var then = (Callable) ScriptableObject.getProperty(promise, "then");
    then.call(cx, scope, promise, new Object[] {resolve});
  }

  /** Register a callback that will be called when the promise resolves. */
  public void then(
      Context cx, Scriptable scope, ResultCallback resolveCb, ResultCallback rejectCb) {
    var resolve =
        new LambdaFunction(
            scope,
            "resolve",
            1,
            (lcx, ls, to, args) -> {
              var val = args.length > 0 ? args[0] : Undefined.instance;
              resolveCb.deliver(lcx, ls, val);
              return Undefined.instance;
            });
    var reject =
        new LambdaFunction(
            scope,
            "reject",
            1,
            (lcx, ls, to, args) -> {
              var val = args.length > 0 ? args[0] : Undefined.instance;
              rejectCb.deliver(lcx, ls, val);
              return Undefined.instance;
            });
    var then = (Callable) ScriptableObject.getProperty(promise, "then");
    then.call(cx, scope, promise, new Object[] {resolve, reject});
  }
}

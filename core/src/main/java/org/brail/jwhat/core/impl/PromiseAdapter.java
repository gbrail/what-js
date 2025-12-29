package org.brail.jwhat.core.impl;

import java.util.concurrent.atomic.AtomicReference;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class PromiseAdapter {
  private final NativePromise promise;
  private final Callable resolve;
  private final Callable reject;
  private boolean pending;

  public interface ResultCallback {
    void deliver(Context cx, Scriptable scope, Object result);
  }

  private PromiseAdapter(NativePromise p, Callable resolve, Callable reject) {
    this.promise = p;
    this.resolve = resolve;
    this.reject = reject;
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

  /**
   * Wrap a Promise or an ordinary object. If a promise, then the adapter may be used with that
   * promise. If a regular value, then the adapter will be backed by a promise that is already
   * resolved.
   */
  public static PromiseAdapter wrap(Context cx, Scriptable scope, Object val) {
    if (val instanceof NativePromise pv) {
      return new PromiseAdapter(pv, null, null);
    }
    return resolved(cx, scope, val);
  }

  /** Create a new, uninitialized promise. */
  public static PromiseAdapter uninitialized(Context cx, Scriptable scope) {
    AtomicReference<Callable> resolve = new AtomicReference<>();
    AtomicReference<Callable> reject = new AtomicReference<>();
    var setup =
        new LambdaFunction(
            scope,
            "setup",
            2,
            (lcx, ls, to, args) -> {
              assert args.length == 2;
              resolve.set((Callable) args[0]);
              reject.set((Callable) args[1]);
              return Undefined.instance;
            });
    var p = (NativePromise) cx.newObject(scope, "Promise", new Object[] {setup});
    // Promise contract says that "setup" should be called immediately.
    var a = new PromiseAdapter(p, resolve.get(), reject.get());
    a.pending = true;
    return a;
  }

  /** Create a promise that is already resolved. */
  public static PromiseAdapter resolved(Context cx, Scriptable scope, Object value) {
    Scriptable promise = (Scriptable) ScriptableObject.getProperty(scope, "Promise");
    Callable resolve = (Callable) ScriptableObject.getProperty(promise, "resolve");
    NativePromise p = (NativePromise) resolve.call(cx, scope, promise, new Object[] {value});
    return new PromiseAdapter(p, null, null);
  }

  /** Create a promise that is already rejected. */
  public static PromiseAdapter rejected(Context cx, Scriptable scope, Object value) {
    Scriptable promise = (Scriptable) ScriptableObject.getProperty(scope, "Promise");
    Callable reject = (Callable) ScriptableObject.getProperty(promise, "reject");
    NativePromise p = (NativePromise) reject.call(cx, scope, promise, new Object[] {value});
    return new PromiseAdapter(p, null, null);
  }

  /** Return the JavaScript Promise that is part of this adapter. */
  public Object getPromise() {
    return promise;
  }

  /**
   * Return true if the promise hasn't been resolved yet. TODO This is not sufficient -- we need
   * real support in PromiseImpl for this to be accurate. Only works currently with promises created
   * by this class.
   */
  public boolean isPending() {
    return pending;
  }

  /**
   * Deliver a result to this promise and resolve it. It is invalid to call this if "resolved" or
   * "rejected" was used.
   */
  public void fulfill(Context cx, Scriptable scope, Object value) {
    assert resolve != null;
    pending = false;
    resolve.call(cx, scope, promise, new Object[] {value});
  }

  /**
   * Deliver an error to this promise and reject it. It is invalid to call this if "resolved" or
   * "rejected" was used.
   */
  public void reject(Context cx, Scriptable scope, Object error) {
    assert reject != null;
    pending = false;
    reject.call(cx, scope, promise, new Object[] {error});
  }
}

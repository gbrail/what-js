package org.brail.jwhat.core.impl;

import java.util.concurrent.atomic.AtomicReference;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/** PromiseAdapter creates a Promise in Rhino and allows us to resolve or reject it when we need. */
public class PromiseAdapter {
  private final NativePromise promise;
  private final Callable resolve;
  private final Callable reject;
  private boolean pending;

  private PromiseAdapter(NativePromise p, Callable resolve, Callable reject) {
    this.promise = p;
    this.resolve = resolve;
    this.reject = reject;
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

  static NativePromise newResolvedPromise(Context cx, Scriptable scope, Object value) {
    Scriptable promise = (Scriptable) ScriptableObject.getProperty(scope, "Promise");
    Callable resolve = (Callable) ScriptableObject.getProperty(promise, "resolve");
    return (NativePromise) resolve.call(cx, scope, promise, new Object[] {value});
  }

  static NativePromise newRejectedPromise(Context cx, Scriptable scope, Object value) {
    Scriptable promise = (Scriptable) ScriptableObject.getProperty(scope, "Promise");
    Callable resolve = (Callable) ScriptableObject.getProperty(promise, "reject");
    return (NativePromise) resolve.call(cx, scope, promise, new Object[] {value});
  }

  /** Create a promise that is already resolved. */
  public static PromiseAdapter resolved(Context cx, Scriptable scope, Object value) {
    return new PromiseAdapter(newResolvedPromise(cx, scope, value), null, null);
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
    if (pending) {
      pending = false;
      resolve.call(cx, scope, promise, new Object[] {value});
    }
  }

  /**
   * Deliver an error to this promise and reject it. It is invalid to call this if "resolved" or
   * "rejected" was used.
   */
  public void reject(Context cx, Scriptable scope, Object error) {
    if (pending) {
      pending = false;
      reject.call(cx, scope, promise, new Object[] {error});
    }
  }
}

package org.brail.jwhat.core.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class PromiseAdapter {
  private final NativePromise promise;
  private final Callable resolve;
  private final Callable reject;

  private PromiseAdapter(NativePromise p, Callable resolve, Callable reject) {
    this.promise = p;
    this.resolve = resolve;
    this.reject = reject;
  }

  /**
   * Return an object that may be used to take action when a JavaScript promise is resolved or
   * rejected, or throw a TypeError if the supplied object is not a Promise.
   */
  public static CompletableFuture<Object> toFuture(Context cx, Scriptable scope, Object promise) {
    if (!(promise instanceof NativePromise)) {
      throw ScriptRuntime.typeError("A Promise object must be supplied");
    }
    var p = (NativePromise) promise;
    var f = new CompletableFuture<Object>();
    var resolve =
        new LambdaFunction(
            scope,
            "resolve",
            1,
            (lcx, ls, to, args) -> {
              var val = args.length > 0 ? args[0] : Undefined.instance;
              f.complete(val);
              return Undefined.instance;
            });
    var reject =
        new LambdaFunction(
            scope,
            "reject",
            1,
            (lcx, ls, to, args) -> {
              var val = args.length > 0 ? args[0] : Undefined.instance;
              if (val instanceof Throwable) {
                f.completeExceptionally((Throwable) val);
              } else {
                f.completeExceptionally(new RejectedPromiseException(val));
              }
              return Undefined.instance;
            });
    var then = (Callable) ScriptableObject.getProperty(p, "then");
    then.call(cx, scope, p, new Object[] {resolve, reject});
    return f;
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
    return new PromiseAdapter(p, resolve.get(), reject.get());
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
   * Deliver a result to this promise and resolve it. It is invalid to call this if "resolved" or
   * "rejected" was used.
   */
  public void fulfill(Context cx, Scriptable scope, Object value) {
    assert resolve != null;
    resolve.call(cx, scope, promise, new Object[] {value});
  }

  /**
   * Deliver an error to this promise and reject it. It is invalid to call this if "resolved" or
   * "rejected" was used.
   */
  public void reject(Context cx, Scriptable scope, Object error) {
    assert reject != null;
    reject.call(cx, scope, promise, new Object[] {error});
  }
}

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
    return new PromiseAdapter(p, resolve.get(), reject.get());
  }

  public static PromiseAdapter resolved(Context cx, Scriptable scope, Object value) {
    Scriptable promise = (Scriptable) ScriptableObject.getProperty(scope, "Promise");
    Callable resolve = (Callable) ScriptableObject.getProperty(promise, "resolve");
    NativePromise p = (NativePromise) resolve.call(cx, scope, promise, new Object[] {value});
    return new PromiseAdapter(p, null, null);
  }

  public static PromiseAdapter rejected(Context cx, Scriptable scope, Object value) {
    Scriptable promise = (Scriptable) ScriptableObject.getProperty(scope, "Promise");
    Callable reject = (Callable) ScriptableObject.getProperty(promise, "reject");
    NativePromise p = (NativePromise) reject.call(cx, scope, promise, new Object[] {value});
    return new PromiseAdapter(p, null, null);
  }

  public Object getPromise() {
    return promise;
  }

  public void fulfill(Context cx, Scriptable scope, Object value) {
    assert resolve != null;
    resolve.call(cx, scope, promise, new Object[] {value});
  }

  public void reject(Context cx, Scriptable scope, Object error) {
    assert reject != null;
    reject.call(cx, scope, promise, new Object[] {error});
  }
}

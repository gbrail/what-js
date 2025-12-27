package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseAdapter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class DefaultWriter extends ScriptableObject {
  private WritableStream stream;
  private PromiseAdapter closed;
  private PromiseAdapter ready;

  public static LambdaConstructor init(Context cx, Scriptable scope) {
    var constructor =
        new LambdaConstructor(
            scope,
            "WritableStreamDefaultWriter",
            1,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            DefaultWriter::constructor);
    constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

    constructor.definePrototypeMethod(scope, "abort", 1, DefaultWriter::abort);
    constructor.definePrototypeMethod(scope, "close", 0, DefaultWriter::close);
    constructor.definePrototypeMethod(scope, "releaseLock", 0, DefaultWriter::releaseLock);
    constructor.definePrototypeMethod(scope, "write", 1, DefaultWriter::write);
    constructor.definePrototypeProperty(cx, "closed", DefaultWriter::getClosed);
    constructor.definePrototypeProperty(cx, "desiredSize", DefaultWriter::getDesiredSize);
    constructor.definePrototypeProperty(cx, "ready", DefaultWriter::getReady);

    return constructor;
  }

  @Override
  public String getClassName() {
    return "WritableStreamDefaultWriter";
  }

  private static DefaultWriter realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, DefaultWriter.class);
  }

  void setUp(Context cx, Scriptable scope, WritableStream stream) {
    this.stream = stream;
    switch (stream.state) {
      case WRITABLE:
        if (!stream.isCloseQueuedOrInFlight() && stream.isBackpressure()) {
          ready = PromiseAdapter.uninitialized(cx, scope);
        } else {
          ready = PromiseAdapter.resolved(cx, scope, Undefined.instance);
        }
        closed = PromiseAdapter.uninitialized(cx, scope);
        break;
      case ERRORING:
        ready = PromiseAdapter.rejected(cx, scope, stream.getError());
        closed = PromiseAdapter.uninitialized(cx, scope);
        break;
      case ERRORED:
        ready = PromiseAdapter.rejected(cx, scope, stream.getError());
        closed = PromiseAdapter.rejected(cx, scope, stream.getError());
        break;
      case CLOSED:
        ready = PromiseAdapter.rejected(cx, scope, Undefined.instance);
        closed = PromiseAdapter.rejected(cx, scope, Undefined.instance);
        break;
    }
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new DefaultWriter();
  }

  private static Object abort(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object close(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object releaseLock(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object write(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object getClosed(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.closed == null ? Undefined.instance : self.closed.getPromise();
  }

  private static Object getReady(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.ready == null ? Undefined.instance : self.ready.getPromise();
  }

  private static Object getDesiredSize(Scriptable thisObj) {
    return Undefined.instance;
  }
}

package org.brail.jwhat.stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class DefaultWriter extends ScriptableObject {
  private WritableStream stream;
  private NativePromise closed;
  private NativePromise ready;

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

  void setUp(Context cx, Scriptable scope, WritableStream stream) {
    this.stream = stream;
    switch (stream.state) {
      case WRITABLE:
        if (!stream.isCloseQueuedOrInFlight() && stream.isBackpressure()) {
          ready = (NativePromise) cx.newObject(scope, "Promise");
        }
        break;
      case ERRORING:
        break;
      case ERRORED:
        break;
      case CLOSED:
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
    return Undefined.instance;
  }

  private static Object getReady(Scriptable thisObj) {
    return Undefined.instance;
  }

  private static Object getDesiredSize(Scriptable thisObj) {
    return Undefined.instance;
  }
}

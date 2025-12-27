package org.brail.jwhat.stream;

import java.util.ArrayList;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class WritableStream extends ScriptableObject {
  enum State {
    WRITABLE,
    CLOSED,
    ERRORING,
    ERRORED
  };

  State state = State.WRITABLE;
  private boolean backpressure;
  private NativePromise closeRequest;
  private NativePromise inFlightCloseRequest;
  private WritableController controller;
  private NativePromise inFlightWriteRequest;
  private PendingAbort pendingAbort;
  private Object error;
  private DefaultWriter writer = null;
  private final ArrayList<NativePromise> writeRequests = new ArrayList<>();

  public static void init(Context cx, Scriptable scope) {
    var writerConstructor = DefaultWriter.init(cx, scope);
    var controllerConstructor = WritableController.init(cx, scope);
    var constructor =
        new LambdaConstructor(
            scope,
            "WritableStream",
            2,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            (lcx, ls, args) -> constructor(lcx, ls, args, controllerConstructor));
    constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

    constructor.definePrototypeMethod(scope, "abort", 0, WritableStream::abort);
    constructor.definePrototypeMethod(scope, "close", 0, WritableStream::close);
    constructor.definePrototypeMethod(
        scope,
        "getWriter",
        0,
        (Context lcx, Scriptable ls, Scriptable to, Object[] args) ->
            getWriter(lcx, ls, to, writerConstructor));
    constructor.definePrototypeProperty(cx, "locked", WritableStream::isLocked);

    ScriptableObject.defineProperty(scope, "WritableStream", constructor, DONTENUM);
  }

  @Override
  public String getClassName() {
    return "WritableStream";
  }

  private static WritableStream realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, WritableStream.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args,
                                        Constructable controllerCons) {
    Object sink = args.length > 0 ? args[0] : null;
    Object strategy = args.length > 1 ? args[1] : null;
    var ws = new WritableStream();
    ws.controller = (WritableController)controllerCons.construct(cx, scope, ScriptRuntime.emptyArgs);
    ws.controller.setUp(cx, scope, sink, ws,
      getSizeStrategy(scope, strategy),
      getHighWaterStrategy(strategy));
    return ws;
  }

  private static Object abort(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object close(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object getWriter(
      Context cx, Scriptable scope, Scriptable thisObj, Constructable writerConstructor) {
    WritableStream self = realThis(thisObj);
    if (self.writer != null) {
      throw ScriptRuntime.typeError("Stream is locked");
    }
    var writer =
        (DefaultWriter)
            writerConstructor.construct(
                cx, scope, ScriptRuntime.emptyArgs);
    writer.setUp(cx, scope, self);
    self.writer = writer;
    return writer;
  }

  private static Object isLocked(Scriptable thisObj) {
    return Undefined.instance;
  }

  boolean isCloseQueuedOrInFlight() {
    return closeRequest != null || inFlightCloseRequest != null;
  }

  boolean isBackpressure() {
    return backpressure;
  }

  void setBackpressure(boolean bp) {
    this.backpressure = bp;
  }

  Object getError() {
    return error;
  }

  private static Callable getSizeStrategy(Scriptable scope, Object stratObj) {
    if (stratObj instanceof Scriptable strategy) {
      Object sizeObj = ScriptableObject.getProperty(strategy, "size");
      if (sizeObj instanceof Callable sizeFunc) {
        return sizeFunc;
      }
    }
    return new LambdaFunction(scope, "getSize", 0, (cx1, scope1, thisObj, args) -> 1);
  }

  private static double getHighWaterStrategy(Object stratObj) {
    if (stratObj instanceof Scriptable strategy) {
      Object hwmObj = ScriptableObject.getProperty(strategy, "highWaterMark");
      double val = ScriptRuntime.toNumber(hwmObj);
      if (Double.isNaN(val) || val < 0.0) {
        throw ScriptRuntime.rangeError("Invalid HWM");
      }
      return val;
    }
    return 1.0;
  }

  private static final class PendingAbort {}
}

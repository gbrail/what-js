package org.brail.jwhat.stream;

import java.util.ArrayList;
import org.brail.jwhat.core.impl.PromiseAdapter;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
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
  private PromiseAdapter closeRequest;
  private PromiseAdapter inFlightCloseRequest;
  private WritableController controller;
  private PromiseAdapter inFlightWriteRequest;
  private PendingAbort pendingAbort;
  private Object error;
  private DefaultWriter writer = null;
  private final ArrayList<PromiseAdapter> writeRequests = new ArrayList<>();

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

  private static Scriptable constructor(
      Context cx, Scriptable scope, Object[] args, Constructable controllerCons) {
    Object sink = args.length > 0 ? args[0] : null;
    Object strategy = args.length > 1 ? args[1] : null;
    var ws = new WritableStream();
    ws.controller =
        (WritableController) controllerCons.construct(cx, scope, ScriptRuntime.emptyArgs);
    ws.controller.setUp(
        cx, scope, sink, ws, getSizeStrategy(scope, strategy), getHighWaterStrategy(strategy));
    return ws;
  }

  private static Object abort(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var reason = args.length > 0 ? args[0] : Undefined.instance;
    var self = realThis(thisObj);
    if (self.isLocked()) {
      throw ScriptRuntime.typeError("Writable stream is locked");
    }
    self.controller.getAbortController().abort(reason);
    return self.abort(cx, scope, reason);
  }

  // WritableStreamAbort
  Object abort(Context cx, Scriptable scope, Object reason) {
    if (state == State.CLOSED || state == State.ERRORED) {
      return PromiseAdapter.resolved(cx, scope, Undefined.instance);
    }
    if (pendingAbort != null) {
      return pendingAbort.promise.getPromise();
    }
    boolean wasAlreadyErroring = false;
    if (state == State.ERRORING) {
      wasAlreadyErroring = true;
      reason = Undefined.instance;
    }
    var promise = PromiseAdapter.uninitialized(cx, scope);
    pendingAbort = new PendingAbort(promise, reason, wasAlreadyErroring);
    if (!wasAlreadyErroring) {
      startErroring(reason);
    }
    return promise.getPromise();
  }

  private static Object close(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.isLocked() || self.isCloseQueuedOrInFlight()) {
      return PromiseAdapter.rejected(cx, scope, ScriptRuntime.typeError("Closing a locked stream"));
    }
    return self.doClose(cx, scope);
  }

  // WritableStreamClose
  Object doClose(Context cx, Scriptable scope) {
    if (state == State.CLOSED || state == State.ERRORED) {
      return PromiseAdapter.resolved(cx, scope, Undefined.instance);
    }
    var promise = PromiseAdapter.uninitialized(cx, scope);
    closeRequest = promise;
    if (writer != null && backpressure && state == State.WRITABLE) {
      writer.getReadyPromise().fulfill(cx, scope, Undefined.instance);
    }
    controller.doClose();
    return promise.getPromise();
  }

  private static Object getWriter(
      Context cx, Scriptable scope, Scriptable thisObj, Constructable writerConstructor) {
    WritableStream self = realThis(thisObj);
    if (self.writer != null) {
      throw ScriptRuntime.typeError("Stream is locked");
    }
    var writer = (DefaultWriter) writerConstructor.construct(cx, scope, ScriptRuntime.emptyArgs);
    writer.setUp(cx, scope, self);
    self.writer = writer;
    return writer;
  }

  private static Object isLocked(Scriptable thisObj) {
    return realThis(thisObj).isLocked();
  }

  void setWriter(DefaultWriter writer) {
    this.writer = writer;
  }

  State getStreamState() {
    return state;
  }

  WritableController getController() {
    return controller;
  }

  boolean isLocked() {
    return writer != null;
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

  void updateBackpressure(boolean bp) {
    throw new AssertionError("WritableStream&UpdateBackpressure");
  }

  Object getError() {
    return error;
  }

  PromiseAdapter getInFlightWriteRequest() {
    return inFlightWriteRequest;
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

  // WritableStreamAddWriteRequest
  PromiseAdapter addWriteRequest(Context cx, Scriptable scope) {
    var p = PromiseAdapter.uninitialized(cx, scope);
    writeRequests.add(p);
    return p;
  }

  void startErroring(Object error) {
    throw new AssertionError("WritableStreamStartErroring");
  }

  void finishErroring() {
    throw new AssertionError("WritableStreamFinishErroring");
  }

  private static final class PendingAbort {
    PromiseAdapter promise;
    Object reason;
    boolean wasAlreadyErroring;

    PendingAbort(PromiseAdapter promise, Object reason, boolean wasAlreadyErroring) {
      this.promise = promise;
      this.reason = reason;
      this.wasAlreadyErroring = wasAlreadyErroring;
    }
  }
}

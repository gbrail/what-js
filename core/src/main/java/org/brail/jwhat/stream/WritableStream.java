package org.brail.jwhat.stream;

import java.util.ArrayDeque;
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
  private final ArrayDeque<PromiseAdapter> writeRequests = new ArrayDeque<>();
  Constructable controllerConstructor;
  Constructable defaultWriterConstructor;

  public static void init(Context cx, Scriptable scope) {
    var writerConstructor = DefaultWriter.init(cx, scope);
    var controllerConstructor = WritableController.init(cx, scope);
    var constructor =
        new LambdaConstructor(
            scope,
            "WritableStream",
            2,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            (lcx, ls, args) ->
                constructor(lcx, ls, args, writerConstructor, controllerConstructor));
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
      Context cx,
      Scriptable scope,
      Object[] args,
      Constructable writerCons,
      Constructable controllerCons) {
    Object sink = args.length > 0 ? args[0] : null;
    Object strategy = args.length > 1 ? args[1] : null;
    var ws = new WritableStream();
    ws.initialize();
    ws.defaultWriterConstructor = writerCons;
    ws.controllerConstructor = controllerCons;
    ws.controller =
        (WritableController) controllerCons.construct(cx, scope, ScriptRuntime.emptyArgs);
    ws.controller.setUpFromSink(
        cx, scope, ws, sink, getHighWaterStrategy(strategy), getSizeStrategy(scope, strategy));
    return ws;
  }

  // InitializeWritableStream
  private void initialize() {
    state = State.WRITABLE;
    error = null;
    writer = null;
    controller = null;
    inFlightWriteRequest = null;
    inFlightCloseRequest = null;
    closeRequest = null;
    writeRequests.clear();
    backpressure = false;
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
      startErroring(cx, scope, reason);
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

  // AcquireWritableStreamDefaultWriter
  Object acquireDefaultWriter(Context cx, Scriptable scope) {
    var w = (DefaultWriter) defaultWriterConstructor.construct(cx, scope, ScriptRuntime.emptyArgs);
    setUpDefaultWriter(w);
    return w;
  }

  // CreateWritableStream
  static Object createWritableStream(
      Context cx,
      Scriptable scope,
      Callable startAlgo,
      Callable writeAlgo,
      Callable closeAlgo,
      Callable abortAlgo,
      double highWater,
      Callable sizeAlgo) {
    assert highWater >= 0.0;
    var w = new WritableStream();
    w.initialize();
    w.controller =
        (WritableController) w.controllerConstructor.construct(cx, scope, ScriptRuntime.emptyArgs);
    w.controller.setUp(
        cx, scope, w, startAlgo, writeAlgo, closeAlgo, abortAlgo, highWater, sizeAlgo);
    return w;
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
    controller.doClose(cx, scope);
    return promise.getPromise();
  }

  // SetUpWritableStreamDefaultWriter
  void setUpDefaultWriter(DefaultWriter writer) {
    throw new AssertionError("SetUpWritableStreamDefaultWriter");
  }

  // IsWritableStreamLocked
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

  void updateBackpressure(Context cx, Scriptable scope, boolean bp) {
    assert state == State.WRITABLE;
    assert !isCloseQueuedOrInFlight();
    if (writer != null && bp != backpressure) {
      if (backpressure) {
        writer.ready = PromiseAdapter.uninitialized(cx, scope);
      } else {
        writer.ready.fulfill(cx, scope, Undefined.instance);
      }
    }
    this.backpressure = bp;
  }

  Object getError() {
    return error;
  }

  PromiseAdapter getInFlightWriteRequest() {
    return inFlightWriteRequest;
  }

  // WritableStreamDealWithrejection
  void dealWithRejection(Context cx, Scriptable scope, Object error) {
    if (state == WritableStream.State.WRITABLE) {
      startErroring(cx, scope, error);
    } else {
      assert state == State.ERRORING;
      finishErroring(cx, scope);
    }
  }

  private static Callable getSizeStrategy(Scriptable scope, Object stratObj) {
    if (stratObj instanceof Scriptable strategy) {
      Object sizeObj = ScriptableObject.getProperty(strategy, "size");
      if (sizeObj instanceof Callable sizeFunc) {
        return sizeFunc;
      }
    }
    return new LambdaFunction(scope, "getSize", 0, (cx1, scope1, thisObj, args) -> 1.0);
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
    assert isLocked();
    assert state == State.WRITABLE;
    var p = PromiseAdapter.uninitialized(cx, scope);
    writeRequests.add(p);
    return p;
  }

  // WritableStreamStartErroring
  void startErroring(Context cx, Scriptable scope, Object err) {
    assert this.error == null;
    assert state == State.WRITABLE;
    assert controller != null;
    state = State.ERRORING;
    this.error = err;
    if (writer != null) {
      writer.ensureReadyRejected(cx, scope, err);
    }
    if (!hasOperationInFlight() || controller.started) {
      finishErroring(cx, scope);
    }
  }

  // WritableStreamFinishErroring
  void finishErroring(Context cx, Scriptable scope) {
    assert state == State.ERRORING;
    assert !hasOperationInFlight();
    state = State.ERRORED;
    controller.runErrorSteps();
    PromiseAdapter req;
    while ((req = writeRequests.poll()) != null) {
      req.reject(cx, scope, error);
    }
    if (pendingAbort == null) {
      rejectCloseAsNeeded(cx, scope);
      return;
    }
    var abortReq = pendingAbort;
    pendingAbort = null;
    if (abortReq.wasAlreadyErroring) {
      abortReq.promise.reject(cx, scope, error);
      rejectCloseAsNeeded(cx, scope);
      return;
    }
    var ap = controller.runAbortSteps(cx, scope, abortReq.reason);
    ap.then(
        cx,
        scope,
        (lcx, ls, v) -> {
          abortReq.promise.fulfill(cx, scope, Undefined.instance);
          rejectCloseAsNeeded(cx, scope);
        },
        (lcx, ls, v) -> {
          abortReq.promise.reject(cx, scope, v);
          rejectCloseAsNeeded(cx, scope);
        });
  }

  // WritableStreamRejectCloseAndClosedPromiseAsNeeded
  private void rejectCloseAsNeeded(Context cx, Scriptable scope) {
    assert state == State.ERRORED;
    if (closeRequest != null) {
      assert inFlightCloseRequest == null;
      closeRequest.reject(cx, scope, error);
      closeRequest = null;
    }
    if (writer != null) {
      writer.closed.reject(cx, scope, error);
    }
  }

  // WritableStreamHasOperationMarkedInFlight
  private boolean hasOperationInFlight() {
    return inFlightWriteRequest != null || inFlightCloseRequest != null;
  }

  void markCloseRequestInFlight() {
    assert inFlightCloseRequest == null;
    assert closeRequest != null;
    inFlightCloseRequest = closeRequest;
    closeRequest = null;
  }

  void markFirstWriteRequestInFlight() {
    assert inFlightWriteRequest == null;
    assert !writeRequests.isEmpty();
    inFlightWriteRequest = writeRequests.removeFirst();
  }

  // WritableStreamFinishInFlightWrite
  void finishInFlightWrite(Context cx, Scriptable scope) {
    assert inFlightWriteRequest != null;
    inFlightWriteRequest.fulfill(cx, scope, Undefined.instance);
    inFlightWriteRequest = null;
  }

  // WritableStreamFinishInFlightWriteWithError
  void finishInFlightWriteWithError(Context cx, Scriptable scope, Object err) {
    assert inFlightWriteRequest != null;
    inFlightWriteRequest.reject(cx, scope, err);
    inFlightWriteRequest = null;
    assert state == State.ERRORING || state == State.WRITABLE;
    dealWithRejection(cx, scope, err);
  }

  // WritableStreamFinishInFlightClose
  void finishInFlightClose(Context cx, Scriptable scope) {
    assert inFlightCloseRequest != null;
    inFlightCloseRequest.fulfill(cx, scope, Undefined.instance);
    inFlightCloseRequest = null;
    assert state == State.ERRORING || state == State.WRITABLE;
    if (state == State.ERRORING) {
      error = null;
      if (pendingAbort != null) {
        pendingAbort.promise.fulfill(cx, scope, Undefined.instance);
        pendingAbort = null;
      }
    }
    state = State.CLOSED;
    if (writer != null) {
      writer.closed.fulfill(cx, scope, Undefined.instance);
    }
  }

  // WritableStreamFinishInFlightCloseWithError
  void finishInFlightCloseWithError(Context cx, Scriptable scope, Object err) {
    assert inFlightCloseRequest != null;
    inFlightCloseRequest.reject(cx, scope, err);
    inFlightCloseRequest = null;
    assert state == State.ERRORING || state == State.WRITABLE;
    if (pendingAbort != null) {
      pendingAbort.promise.reject(cx, scope, err);
      pendingAbort = null;
    }
    dealWithRejection(cx, scope, err);
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

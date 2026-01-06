package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.Errors;
import org.brail.jwhat.core.impl.PromiseAdapter;
import org.brail.jwhat.core.impl.PromiseWrapper;
import org.brail.jwhat.core.impl.Properties;
import org.brail.jwhat.events.AbortController;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

class WritableController extends ScriptableObject {
  private WritableStream stream;
  private Callable abortAlgorithm;
  private Callable closeAlgorithm;
  private Callable writeAlgorithm;
  private Callable strategySizeAlgorithm;
  private Scriptable sink;
  private AbortController abortController;
  private final QueueWithSize<Object> writeQueue = new QueueWithSize<>();
  private double strategyHwm;
  boolean started;

  private static final Object CLOSE_SENTINEL = new Object();

  public static LambdaConstructor init(Context cx, Scriptable scope) {
    var constructor =
        new LambdaConstructor(
            scope,
            "WritableStreamDefaultController",
            0,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            WritableController::constructor);
    constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

    constructor.definePrototypeMethod(scope, "error", 1, WritableController::error);
    constructor.definePrototypeProperty(cx, "signal", WritableController::getSignal);
    return constructor;
  }

  @Override
  public String getClassName() {
    return "WritableStreamDefaultController";
  }

  AbortController getAbortController() {
    return abortController;
  }

  private static WritableController realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, WritableController.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    if (args.length != 0) {
      throw ScriptRuntime.typeError("not constructable");
    }
    return new WritableController();
  }

  // [ErrorSteps()]
  void runErrorSteps() {
    writeQueue.reset();
  }

  // [AbortSteps()]
  PromiseWrapper runAbortSteps(Context cx, Scriptable scope, Object reason) {
    var result = abortAlgorithm.call(cx, scope, sink, new Object[] {reason});
    clearAlgorithms();
    return PromiseWrapper.wrap(cx, scope, result);
  }

  private static Object error(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream.getStreamState() == WritableStream.State.WRITABLE) {
      Object err = args.length > 0 ? args[0] : Undefined.instance;
      self.doError(cx, scope, err);
    }
    return Undefined.instance;
  }

  private static Object getSignal(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.abortController != null) {
      return self.abortController.getSignal();
    }
    return Undefined.instance;
  }

  // SetUpWritableStreamDefaultController
  void setUp(
      Context cx,
      Scriptable scope,
      WritableStream stream,
      Callable startAlgo,
      Callable writeAlgo,
      Callable closeAlgo,
      Callable abortAlgo,
      double highWater,
      Callable sizeAlgo) {
    this.stream = stream;
    this.writeQueue.reset();
    this.started = false;
    this.abortController = (AbortController) cx.newObject(scope, "AbortController");
    this.strategySizeAlgorithm = sizeAlgo;
    this.strategyHwm = highWater;
    this.writeAlgorithm = writeAlgo;
    this.closeAlgorithm = closeAlgo;
    this.abortAlgorithm = abortAlgo;
    stream.updateBackpressure(cx, scope, getBackpressure());
    var sa = PromiseWrapper.wrap(cx, scope, startAlgo.call(cx, scope, sink, new Object[] {this}));
    sa.then(
        cx,
        scope,
        (lcx, ls, val) -> {
          started = true;
          advanceQueueIfNeeded(cx, scope);
        },
        (lcx, ls, val) -> {
          started = true;
          stream.dealWithRejection(cx, scope, val);
        });
  }

  // SetUpWritableStreamDefaultControllerFromUnderlyingSink
  void setUpFromSink(
      Context cx,
      Scriptable scope,
      WritableStream stream,
      Object sinkObj,
      Callable sizeStrategy,
      double highWater) {
    var returnUndefined =
        new LambdaFunction(
            scope,
            "default",
            0,
            (lcx, ls, to, args) -> PromiseAdapter.resolved(cx, scope, Undefined.instance));
    Callable startAlgorithm = returnUndefined;
    Callable writeAlgorithm = returnUndefined;
    Callable closeAlgorithm = returnUndefined;
    Callable abortAlgorithm = returnUndefined;
    if (sinkObj != null) {
      if (!(sinkObj instanceof Scriptable s)) {
        throw ScriptRuntime.typeError("Invalid sink type");
      }
      this.sink = s;
      if (s.has("type", s)) {
        throw ScriptRuntime.rangeError("Invalid sink");
      }
      var sc = Properties.getOptionalCallable(s, "start");
      if (sc != null) {
        startAlgorithm = sc;
      }
      var wc = Properties.getOptionalCallable(s, "write");
      if (wc != null) {
        writeAlgorithm = wc;
      }
      var cc = Properties.getOptionalCallable(s, "close");
      if (cc != null) {
        closeAlgorithm = cc;
      }
      var ac = Properties.getOptionalCallable(s, "abort");
      if (ac != null) {
        abortAlgorithm = ac;
      }
    }
    setUp(
        cx,
        scope,
        stream,
        startAlgorithm,
        writeAlgorithm,
        closeAlgorithm,
        abortAlgorithm,
        highWater,
        sizeStrategy);
  }

  // WritableStreamDefaultControllerClearAlgorithms
  void clearAlgorithms() {
    writeAlgorithm = null;
    closeAlgorithm = null;
    abortAlgorithm = null;
    strategySizeAlgorithm = null;
  }

  private void advanceQueueIfNeeded(Context cx, Scriptable scope) {
    if (!started || stream.getInFlightWriteRequest() != null) {
      return;
    }
    if (stream.getStreamState() == WritableStream.State.ERRORING) {
      stream.finishErroring(cx, scope);
      return;
    }
    if (writeQueue.isEmpty()) {
      return;
    }
    var val = writeQueue.peek();
    if (val == CLOSE_SENTINEL) {
      processClose(cx, scope);
    } else {
      processWrite(cx, scope, val);
    }
  }

  // WritableStreamDefaultControllerWrite
  void doWrite(Context cx, Scriptable scope, Object chunk, double chunkSize) {
    try {
      writeQueue.enqueue(cx, scope, chunk, chunkSize);
    } catch (RhinoException re) {
      errorIfNeeded(cx, scope, re);
      return;
    }
    if (!stream.isCloseQueuedOrInFlight()
        && stream.getStreamState() == WritableStream.State.WRITABLE) {
      stream.updateBackpressure(cx, scope, getBackpressure());
    }
    advanceQueueIfNeeded(cx, scope);
  }

  // WritableStreamDefaultControllerGetChunkSize
  double getChunkSize(Context cx, Scriptable scope, Object chunk) {
    if (strategySizeAlgorithm == null) {
      return 1.0;
    }
    double size;
    try {
      var sv =
          strategySizeAlgorithm.call(
              cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] {chunk});
      size = ScriptRuntime.toNumber(sv);
    } catch (JavaScriptException jse) {
      errorIfNeeded(cx, scope, jse.getValue());
      return 1.0;
    }
    if (!Double.isFinite(size) || size < 0.0) {
      errorIfNeeded(cx, scope, Errors.newRangeError(cx, scope, "size out of range"));
      return 1.0;
    }
    return size;
  }

  double getDesiredSize() {
    return strategyHwm - writeQueue.getTotalSize();
  }

  // WritableStreamDefaultControllerGetBackpressure
  boolean getBackpressure() {
    return getDesiredSize() <= 0.0;
  }

  // WritableStreamDefaultControllerClose
  void doClose(Context cx, Scriptable scope) {
    writeQueue.enqueue(cx, scope, CLOSE_SENTINEL, 0.0);
    advanceQueueIfNeeded(cx, scope);
  }

  // WritableStreamDefaultControllerError
  void doError(Context cx, Scriptable scope, Object err) {
    clearAlgorithms();
    stream.startErroring(cx, scope, err);
  }

  // WritableStreamDefaultControllerProcessClose
  private void processClose(Context cx, Scriptable scope) {
    stream.markCloseRequestInFlight();
    writeQueue.dequeue();
    // assert writeQueue.isEmpty();
    var cp =
        PromiseWrapper.wrap(
            cx, scope, closeAlgorithm.call(cx, scope, sink, ScriptRuntime.emptyArgs));
    clearAlgorithms();
    cp.then(
        cx,
        scope,
        (lcx, ls, v) -> stream.finishInFlightClose(cx, scope),
        (lcx, ls, v) -> stream.finishInFlightCloseWithError(cx, scope, v));
  }

  // WritableStreamDefaultControllerProcessWrite
  private void processWrite(Context cx, Scriptable scope, Object chunk) {
    stream.markFirstWriteRequestInFlight();
    var wp =
        PromiseWrapper.wrap(
            cx, scope, writeAlgorithm.call(cx, scope, sink, new Object[] {chunk, this}));
    wp.then(
        cx,
        scope,
        (lcx, ls, v) -> {
          stream.finishInFlightWrite(cx, scope);
          writeQueue.dequeue();
          if (!stream.isCloseQueuedOrInFlight()
              && stream.getStreamState() == WritableStream.State.WRITABLE) {
            stream.updateBackpressure(cx, scope, getBackpressure());
          }
          advanceQueueIfNeeded(cx, scope);
        },
        (lcx, ls, v) -> {
          if (stream.getStreamState() == WritableStream.State.WRITABLE) {
            clearAlgorithms();
          }
          stream.finishInFlightWriteWithError(cx, scope, v);
        });
  }

  // WritableStreamDefaultControllerErrorIfNeeded
  private void errorIfNeeded(Context cx, Scriptable scope, Object err) {
    if (stream.getStreamState() == WritableStream.State.WRITABLE) {
      doError(cx, scope, err);
    }
  }
}

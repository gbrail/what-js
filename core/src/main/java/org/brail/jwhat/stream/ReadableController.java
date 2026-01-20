package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseWrapper;
import org.brail.jwhat.core.impl.Properties;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ReadableController extends AbstractReadableController {
  private ReadableStream stream;
  private Scriptable source;
  private Callable cancelAlgorithm;
  private Callable pullAlgorithm;
  private Callable sizeAlgorithm;
  private boolean closeRequested;
  private boolean pullAgain;
  private boolean pulling;
  private boolean started;
  private double strategyHwm;
  private final QueueWithSize<Object> readQueue = new QueueWithSize<>();

  public static Constructable init(Context cx, Scriptable scope) {
    var c =
        new LambdaConstructor(
            scope, "ReadableStreamDefaultController", 0, ReadableController::constructor);
    c.definePrototypeMethod(scope, "close", 0, ReadableController::close);
    c.definePrototypeMethod(scope, "enqueue", 1, ReadableController::enqueue);
    c.definePrototypeMethod(scope, "error", 0, ReadableController::error);
    c.definePrototypeProperty(cx, "desiredSize", ReadableController::getDesiredSize);
    return c;
  }

  private ReadableController() {}

  @Override
  public String getClassName() {
    return "ReadableStreamDefaultController;";
  }

  private static ReadableController realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, ReadableController.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new ReadableController();
  }

  // SetUpReadableStreamDefaultControllerFromUnderlyingSource
  void setUpFromSource(
      Context cx,
      Scriptable scope,
      ReadableStream stream,
      Object sourceObj,
      Callable sizeStrategy,
      double highWater) {
    Callable startAlgo = AbstractOperations.returnUndefined(cx, scope);
    var undefinedPromise = AbstractOperations.returnUndefinedPromise(cx, scope);
    Callable pullAlgo = undefinedPromise;
    Callable cancelAlgo = undefinedPromise;
    if (sourceObj != null) {
      if (!(sourceObj instanceof Scriptable s)) {
        throw ScriptRuntime.typeError("Invalid source type");
      }
      this.source = s;
      if (s.has("type", s)) {
        throw ScriptRuntime.rangeError("Invalid source");
      }
      var sc = Properties.getOptionalCallable(s, "start");
      if (sc != null) {
        startAlgo = sc;
      }
      var pc = Properties.getOptionalCallable(s, "pull");
      if (pc != null) {
        pullAlgo = pc;
      }
      var cc = Properties.getOptionalCallable(s, "cancel");
      if (cc != null) {
        cancelAlgo = cc;
      }
    }
    setUp(cx, scope, stream, startAlgo, pullAlgo, cancelAlgo, sizeStrategy, highWater);
  }

  // SetUpReadableStreamDefaultController
  private void setUp(
      Context cx,
      Scriptable scope,
      ReadableStream stream,
      Callable startAlgo,
      Callable pullAlgo,
      Callable cancelAlgo,
      Callable sizeStrategy,
      double highWater) {
    this.stream = stream;
    this.readQueue.reset();
    this.started = false;
    this.closeRequested = false;
    this.pullAgain = false;
    this.pulling = false;
    this.sizeAlgorithm = sizeStrategy;
    this.strategyHwm = highWater;
    this.pullAlgorithm = pullAlgo;
    this.cancelAlgorithm = cancelAlgo;
    stream.controller = this;
    var sa = PromiseWrapper.wrap(cx, scope, startAlgo.call(cx, scope, source, new Object[] {this}));
    sa.then(
        cx,
        scope,
        (lcx, ls, val) -> {
          started = true;
          pullIfNeeded(cx, scope);
        },
        this::controllerError);
  }

  // ReadableStreamDefaultControllerClearAlgorithms
  private void clearAlgorithms() {
    pullAlgorithm = null;
    cancelAlgorithm = null;
    sizeAlgorithm = null;
  }

  private static Object getDesiredSize(Scriptable thisObj) {
    return realThis(thisObj).getDesiredSize();
  }

  private static Object close(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (!self.canCloseOrEnqueue()) {
      throw ScriptRuntime.typeError("cannot close");
    }
    self.controllerClose(cx, scope);
    return Undefined.instance;
  }

  private static Object enqueue(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (!self.canCloseOrEnqueue()) {
      throw ScriptRuntime.typeError("cannot enqueue");
    }
    return self.controllerEnqueue(cx, scope, args.length > 0 ? args[0] : null);
  }

  private static Object error(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    self.controllerError(cx, scope, args.length > 0 ? args[0] : null);
    return Undefined.instance;
  }

  // ReadableStreamDefaultControllerError
  private void controllerError(Context cx, Scriptable scope, Object err) {
    if (stream.state == ReadableStream.State.READABLE) {
      readQueue.reset();
      clearAlgorithms();
      stream.streamError(cx, scope, err);
    }
  }

  // ReadableStreamDefaultControllerEnqueue
  private Object controllerEnqueue(Context cx, Scriptable scope, Object chunk) {
    if (!canCloseOrEnqueue()) {
      return Undefined.instance;
    }
    if (stream.isLocked() && stream.getNumReadRequests() > 0) {
      stream.fulfillReadRequest(cx, scope, chunk, false);
    } else {
      try {
        var sv =
            sizeAlgorithm.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] {chunk});
        var size = ScriptRuntime.toNumber(sv);
        readQueue.enqueue(cx, scope, chunk, size);
      } catch (JavaScriptException e) {
        controllerError(cx, scope, e.getValue());
        return Undefined.instance;
      }
    }
    pullIfNeeded(cx, scope);
    return Undefined.instance;
  }

  // ReadableStreamDefaultControllerCallPullIfNeeded
  private void pullIfNeeded(Context cx, Scriptable scope) {
    if (shouldCallPull()) {
      if (pulling) {
        pullAgain = true;
        return;
      }
      pulling = true;
      var pp = PromiseWrapper.wrapCall(cx, scope, source, new Object[] {this}, pullAlgorithm);
      pp.then(
          cx,
          scope,
          (lcx, ls, val) -> {
            pulling = false;
            if (pullAgain) {
              pullAgain = false;
              pullIfNeeded(lcx, ls);
            }
          },
          this::controllerError);
    }
  }

  private boolean shouldCallPull() {
    if (!canCloseOrEnqueue() || !started) {
      return false;
    }
    if (stream.isLocked() && stream.getNumReadRequests() > 0) {
      return true;
    }
    return getDesiredSize() > 0.0;
  }

  private boolean canCloseOrEnqueue() {
    return closeRequested && stream.state == ReadableStream.State.READABLE;
  }

  private double getDesiredSize() {
    return switch (stream.state) {
      case READABLE -> strategyHwm - readQueue.getTotalSize();
      case ERRORED -> Double.NaN;
      case CLOSED -> 0.0;
    };
  }

  // ReadableStreamDefaultControllerClose
  private void controllerClose(Context cx, Scriptable scope) {
    if (canCloseOrEnqueue()) {
      closeRequested = true;
      if (readQueue.isEmpty()) {
        clearAlgorithms();
        stream.streamClose(cx, scope);
      }
    }
  }

  // Steps that may differ between controllers
  @Override
  PromiseWrapper cancelSteps(Context cx, Scriptable scope, Object reason) {
    readQueue.reset();
    var cp = PromiseWrapper.wrapCall(cx, scope, source, new Object[] {reason}, cancelAlgorithm);
    clearAlgorithms();
    return cp;
  }

  @Override
  void pullSteps(Context cx, Scriptable scope, DefaultReader.ReadRequest rr) {
    if (!readQueue.isEmpty()) {
      var chunk = readQueue.dequeue();
      if (closeRequested && readQueue.isEmpty()) {
        clearAlgorithms();
        stream.streamClose(cx, scope);
      } else {
        pullIfNeeded(cx, scope);
      }
      rr.getChunkSteps().handle(cx, scope, chunk);
    } else {
      stream.reader.readRequests.push(rr);
    }
  }

  @Override
  void releaseSteps() {
    // Nothing to do for now
  }
}

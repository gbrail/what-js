package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.Errors;
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
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;

import java.util.ArrayDeque;

public class ReadableByteStreamController extends AbstractReadableController {
  private ReadableStream stream;
  private Scriptable source;
  private Callable cancelAlgorithm;
  private Callable pullAlgorithm;
  private int autoAllocateChunkSize;
  private boolean closeRequested;
  private boolean pullAgain;
  private boolean pulling;
  private boolean started;
  private double strategyHwm;
  private BYOBRequest byobRequest;
  private final QueueWithSize<Object> readQueue = new QueueWithSize<>();
  private final ArrayDeque<PullDescriptor> pendingPullIntos = new ArrayDeque<>();

  public static Constructable init(Context cx, Scriptable scope) {
    var c =
        new LambdaConstructor(
            scope, "ReadableByteStreamController", 0, ReadableByteStreamController::constructor);
    c.definePrototypeMethod(scope, "close", 0, ReadableByteStreamController::close);
    c.definePrototypeMethod(scope, "enqueue", 1, ReadableByteStreamController::enqueue);
    c.definePrototypeMethod(scope, "error", 0, ReadableByteStreamController::error);
    c.definePrototypeProperty(cx, "desiredSize", ReadableByteStreamController::getDesiredSize);
    return c;
  }

  private ReadableByteStreamController() {}

  @Override
  public String getClassName() {
    return "ReadableStreamDefaultController;";
  }

  private static ReadableByteStreamController realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, ReadableByteStreamController.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new ReadableByteStreamController();
  }

  // SetUpReadableByteStreamControllerFromUnderlyingSource
  void setUpFromSource(
      Context cx, Scriptable scope, ReadableStream stream, Object sourceObj, double highWater) {
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

  // SetUpReadableByteStreamController
  private void setUp(
      Context cx,
      Scriptable scope,
      ReadableStream stream,
      Callable startAlgo,
      Callable pullAlgo,
      Callable cancelAlgo,
      double highWater,
      Object autoChunkSize) {
    this.stream = stream;
    this.readQueue.reset();
    this.started = false;
    this.closeRequested = false;
    this.pullAgain = false;
    this.pulling = false;
    this.strategyHwm = highWater;
    this.pullAlgorithm = pullAlgo;
    this.cancelAlgorithm = cancelAlgo;

    if (!Undefined.isUndefined(autoChunkSize)) {
      int cs = ScriptRuntime.toInt32(autoChunkSize);
      if (cs < 0 || ScriptRuntime.toNumber(autoChunkSize) != cs) {
        // Not an integer or negative
        throw ScriptRuntime.typeError("Invalid chunk size");
      }
    }

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

  // ReadableByteStreamControllerError
  private void controllerError(Context cx, Scriptable scope, Object err) {
    if (stream.state == ReadableStream.State.READABLE) {
      clearPendingPullIntos();
      readQueue.reset();
      clearAlgorithms();
      stream.streamError(cx, scope, err);
    }
  }

  // ReadableByteStreamControllerClearPendingPullIntos
  private void clearPendingPullIntos() {
    invalidateBYOBRequest();
    pendingPullIntos.clear();
  }

  // ReadableByteStreamInvalidateBYOBRequest
  private void invalidateBYOBRequest() {
    if (byobRequest != null) {
      byobRequest.controller = null;
      byobRequest.view = null;
      byobRequest = null;
    }
  }

  // ReadableByteStreamControllerEnqueue
  private Object controllerEnqueue(Context cx, Scriptable scope, NativeArrayBuffer chunk) {
    if (closeRequested || stream.state != ReadableStream.State.READABLE) {
      return Undefined.instance;
    }
    if (chunk.isDetached()) {
      throw ScriptRuntime.typeError("Chunk is detached");
    }
    var newBuf = transferBuffer(cx, scope, chunk);
    if (!pendingPullIntos.isEmpty()) {
      var pp = pendingPullIntos.getFirst();
      if (pp.buffer.isDetached()) {
        throw ScriptRuntime.typeError("Pending chunk is detached");
      }
      invalidateBYOBRequest();
      TODO TODO this one is complicated
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

  // ReadableByteStreamControllerCallPullIfNeeded
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
    if (stream.state != ReadableStream.State.READABLE || closeRequested || !started) {
      return false;
    }

    if ((stream.hasDefaultReader() && stream.getNumReadRequests() > 0)
        || (stream.hasBYOBReader() && stream.getNumReadIntoRequests() > 0)) {
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

  // ReadableByteStreamControllerClose
  private void controllerClose(Context cx, Scriptable scope) {
    if (closeRequested && stream.state == ReadableStream.State.READABLE) {
      if (!readQueue.isEmpty()) {
        closeRequested = true;
        return;
      }
      if (!pendingPullIntos.isEmpty()) {
        var first = pendingPullIntos.getFirst();
        if (first.filled % first.eltSize != 0) {
          var err = Errors.newTypeError(cx, scope, "Incomplete");
          controllerError(cx, scope, err);
          throw new JavaScriptException(err);
        }
      }
      clearAlgorithms();
      stream.streamClose(cx, scope);
    }
  }

  // ReadableByteStreamControllerCommitPullIntoDescriptor
  private void commitPullInto(PullDescriptor pd) {
    boolean done = false;
    if (stream.state == ReadableStream.State.CLOSED) {
      done = true;
    }
  }

  // ReadableByteStreamControllerConvertPullIntoDescriptor
  private Scriptable convertPullDescriptor(Context cx, Scriptable scope, PullDescriptor pd) {
    var newBuf = transferBuffer(cx, scope, pd.buffer);
    return pd.constructor.construct(
        cx, scope, new Object[] {newBuf, pd.offset, pd.filled + pd.eltSize});
  }

  private static NativeArrayBuffer transferBuffer(Context cx, Scriptable scope, NativeArrayBuffer b) {
    var transfer = (Callable) b.get("transfer", b);
    // TODO transfer makes a copy, we can do better than that
    return (NativeArrayBuffer) transfer.call(cx, scope, null, ScriptRuntime.emptyArgs);
  }

  // Steps that may differ between controllers
  PromiseWrapper cancelSteps(Context cx, Scriptable scope, Object reason) {
    readQueue.reset();
    var cp = PromiseWrapper.wrapCall(cx, scope, source, new Object[] {reason}, cancelAlgorithm);
    clearAlgorithms();
    return cp;
  }

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

  void releaseSteps() {
    // Nothing to do for now
  }

  private enum ReaderType {
    DEFAULT,
    BYOB,
    NONE
  };

  private static class PullDescriptor {
    NativeArrayBuffer buffer;
    int length;
    int offset;
    int filled;
    int minFill;
    int eltSize;
    Constructable constructor;
    ReaderType readerType;
  }
}

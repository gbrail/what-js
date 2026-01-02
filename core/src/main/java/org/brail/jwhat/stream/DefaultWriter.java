package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseAdapter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

class DefaultWriter extends ScriptableObject {
  private WritableStream stream;
  PromiseAdapter closed;
  PromiseAdapter ready;

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

  // SetUpWritableStreamDefaultWriter
  void setUp(Context cx, Scriptable scope, WritableStream stream) {
    if (stream.isLocked()) {
      throw ScriptRuntime.typeError("Stream already locked");
    }
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
        ready = PromiseAdapter.resolved(cx, scope, Undefined.instance);
        closed = PromiseAdapter.resolved(cx, scope, Undefined.instance);
        break;
    }
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    if (args.length < 1 || !(args[0] instanceof WritableStream stream)) {
      throw ScriptRuntime.typeError("Stream required");
    }
    if (stream.writer != null) {
      throw ScriptRuntime.typeError("Stream is locked");
    }
    var w = new DefaultWriter();
    w.setUp(cx, scope, stream);
    stream.writer = w;
    return w;
  }

  private static Object abort(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream == null) {
      throw ScriptRuntime.typeError("Cannot abort: no stream");
    }
    var reason = args.length > 0 ? args[0] : Undefined.instance;
    return self.abort(cx, scope, reason);
  }

  // WritableStreamDefaultWriterAbort
  private Object abort(Context cx, Scriptable scope, Object reason) {
    return stream.abort(cx, scope, reason);
  }

  private static Object close(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream == null) {
      throw ScriptRuntime.typeError("Cannot close: no stream");
    }
    if (self.stream.isCloseQueuedOrInFlight()) {
      return PromiseAdapter.rejected(cx, scope, ScriptRuntime.typeError("Close in flight"));
    }
    return self.close(cx, scope);
  }

  // WritableStreamDefaultWriterClose
  private Object close(Context cx, Scriptable scope) {
    return stream.doClose(cx, scope);
  }

  // WriteableStreamDefaultWriterCloseWithErrorPropagation
  Object closeWithErrorPropagation(Context cx, Scriptable scope) {
    if (stream.isCloseQueuedOrInFlight() || stream.state == WritableStream.State.CLOSED) {
      return PromiseAdapter.resolved(cx, scope, Undefined.instance).getPromise();
    }
    if (stream.state == WritableStream.State.ERRORED) {
      return PromiseAdapter.resolved(cx, scope, stream.getError()).getPromise();
    }
    return close(cx, scope);
  }

  // WritableStreamDefaultWriterRelease
  private static Object releaseLock(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    var err = ScriptRuntime.typeError("Stream released");
    self.ensureReadyRejected(cx, scope, err);
    self.ensureCloseRejected(cx, scope, err);
    self.stream.setWriter(null);
    self.stream = null;
    return Undefined.instance;
  }

  private static Object write(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream == null) {
      throw ScriptRuntime.typeError("Cannot write: no stream");
    }
    Object chunk = args.length > 0 ? args[0] : Undefined.instance;
    return self.doWrite(cx, scope, chunk);
  }

  // WritableStreamDefaultWriterWrite
  private Object doWrite(Context cx, Scriptable scope, Object chunk) {
    var chunkSize = stream.getController().getChunkSize(cx, scope, chunk);
    switch (stream.getStreamState()) {
      case ERRORED:
      case ERRORING:
        return PromiseAdapter.rejected(cx, scope, stream.getError()).getPromise();
      case CLOSED:
        return PromiseAdapter.rejected(
                cx, scope, ScriptRuntime.typeError("Stream closing or closed"))
            .getPromise();
      case WRITABLE:
        var p = stream.addWriteRequest(cx, scope);
        stream.getController().doWrite(cx, scope, chunk, chunkSize);
        return p.getPromise();
      default:
        throw new IllegalStateException();
    }
  }

  private static Object getClosed(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.closed == null ? Undefined.instance : self.closed.getPromise();
  }

  private static Object getReady(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.ready == null ? Undefined.instance : self.ready.getPromise();
  }

  // WritableStreamDefaultWriterGetDesiredSize
  private static Object getDesiredSize(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.stream == null) {
      throw ScriptRuntime.typeError("Stream closed");
    }
    return switch (self.stream.getStreamState()) {
      case ERRORED, ERRORING -> null;
      case CLOSED -> 0;
      default -> self.stream.getController().getDesiredSize();
    };
  }

  void ensureReadyRejected(Context cx, Scriptable scope, Object val) {
    if (ready.isPending()) {
      ready.reject(cx, scope, val);
    }
  }

  void ensureCloseRejected(Context cx, Scriptable scope, Object val) {
    if (closed.isPending()) {
      closed.reject(cx, scope, val);
    }
  }
}

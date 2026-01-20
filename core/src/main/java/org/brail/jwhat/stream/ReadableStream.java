package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.Errors;
import org.brail.jwhat.core.impl.PromiseAdapter;
import org.brail.jwhat.core.impl.Properties;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ReadableStream extends ScriptableObject {
  enum State {
    READABLE,
    CLOSED,
    ERRORED,
  }

  private Constructable readerConstructor;
  State state;
  AbstractReadableController controller;
  boolean disturbed;
  AbstractReader reader;
  Object error;

  public static void init(Context cx, Scriptable scope) {
    var readerConstructor = DefaultReader.init(cx, scope);
    var dfltController = ReadableController.init(cx, scope);
    var byteController = ReadableByteStreamController.init(cx, scope);
    var c =
        new LambdaConstructor(
            scope,
            "ReadableStream",
            2,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            (lcx, ls, args) ->
                constructor(lcx, ls, args, readerConstructor, dfltController));
    c.definePrototypeMethod(scope, "from", 1, ReadableStream::from);
    c.definePrototypeMethod(scope, "cancel", 1, ReadableStream::cancel);
    c.definePrototypeMethod(scope, "getReader", 1, ReadableStream::getReader);
    c.definePrototypeMethod(scope, "pipeThrough", 2, ReadableStream::pipeThrough);
    c.definePrototypeMethod(scope, "pipeTo", 2, ReadableStream::pipeTo);
    c.definePrototypeMethod(scope, "tee", 0, ReadableStream::tee);
    c.definePrototypeProperty(cx, "locked", ReadableStream::locked);
    c.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
    ScriptableObject.defineProperty(scope, "ReadableStream", c, DONTENUM);
    ScriptableObject.defineProperty(
        scope, "ReadableStreamDefaultReader", readerConstructor, DONTENUM);
    ScriptableObject.defineProperty(
        scope, "ReadableStreamDefaultController", dfltController, DONTENUM);
    ScriptableObject.defineProperty(
            scope, "ReadableByteStreamController", byteController, DONTENUM);
    )
  }

  private ReadableStream() {}

  @Override
  public String getClassName() {
    return "ReadableStream";
  }

  private static ReadableStream realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, ReadableStream.class);
  }

  private static Scriptable constructor(
      Context cx,
      Scriptable scope,
      Object[] args,
      Constructable readerCons,
      Constructable dfltController,
      Constructable bytesController) {
    Object source = args.length > 0 ? args[0] : null;
    Object strategy = args.length > 1 ? args[1] : null;
    var rs = new ReadableStream();
    rs.initialize();
    rs.readerConstructor = readerCons;
    if (source instanceof Scriptable ss) {
      if ("bytes".equals(Properties.getOptionalValue(ss, "type", ""))) {
        var controller =
                (ReadableByteStreamController) bytesController.construct(cx, scope, ScriptRuntime.emptyArgs);
        controller.setUpFromSource(cx,
                scope,
                rs,
                source,
                AbstractOperations.getHighWaterStrategy(scope, 1.0));
        rs.controller = controller;
        return rs;
      }
    }

    // Fall through for default case
    var controller =
        (ReadableController) dfltController.construct(cx, scope, ScriptRuntime.emptyArgs);
    controller.setUpFromSource(
        cx,
        scope,
        rs,
        source,
        AbstractOperations.getSizeStrategy(scope, strategy),
        AbstractOperations.getHighWaterStrategy(scope, 1.0));
    rs.controller = controller;
    return rs;
  }

  // InitializeReadableStream
  private void initialize() {
    state = State.READABLE;
    reader = null;
    error = null;
    disturbed = false;
  }

  private static Object from(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError();
  }

  private static Object cancel(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.isLocked()) {
      return PromiseAdapter.rejected(cx, scope, Errors.newTypeError(cx, scope, "stream is locked"));
    }
    return self.streamCancel(cx, scope, args.length > 0 ? args[0] : Undefined.instance);
  }

  // ReadableStreamCancel
  Object streamCancel(Context cx, Scriptable scope, Object err) {
    disturbed = true;
    if (state == State.CLOSED) {
      return PromiseAdapter.resolved(cx, scope, Undefined.instance);
    }
    if (state == State.ERRORED) {
      return PromiseAdapter.rejected(cx, scope, error);
    }
    streamClose(cx, scope);
    // TODO if reader is a BYOB reader
    var cp = controller.cancelSteps(cx, scope, err);
    return cp.then(cx, scope, (lcx, ls, la) -> {}, (lcx, ls, la) -> {});
  }

  private static Object getReader(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var options = args.length > 0 ? args[0] : null;
    var self = realThis(thisObj);
    if (!(options instanceof Scriptable s) || !s.has("mode", s)) {
      var rdr =
          (DefaultReader) self.readerConstructor.construct(cx, scope, ScriptRuntime.emptyArgs);
      rdr.setUp(cx, scope, self);
      return rdr;
    }
    // TODO BYOB reader
    throw new AssertionError("BYOB not implemented");
  }

  private static Object pipeThrough(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError("pipes not implemented");
  }

  private static Object pipeTo(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError("pipes not implemented");
  }

  private static Object tee(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError("pipes not implemented");
  }

  private static Object locked(Scriptable thisObj) {
    return realThis(thisObj).isLocked();
  }

  boolean hasDefaultReader() {
    return reader instanceof DefaultReader;
  }

  boolean hasBYOBReader() {
    return reader instanceof BYOBReader;
  }

  // IsReadableStreamLocked
  boolean isLocked() {
    return reader != null;
  }

  // ReadableStreamGetNumReadRequests
  int getNumReadRequests() {
    assert hasDefaultReader();
    return ((DefaultReader)reader).getNumReadRequests();
  }

  int getNumReadIntoRequests() {
    assert hasBYOBReader();
    return ((BYOBReader)reader).getNumReadIntoRequests();
  }

  // ReadableStreamFulfillReadRequest
  void fulfillReadRequest(Context cx, Scriptable scope, Object chunk, boolean done) {
    assert reader instanceof DefaultReader;
    var dr = (DefaultReader) reader;
    // check that it's the default reader?
    assert !dr.readRequests.isEmpty();
    var rr = dr.readRequests.pop();
    if (done) {
      rr.getCloseSteps().handle(cx, scope);
    } else {
      rr.getChunkSteps().handle(cx, scope, chunk);
    }
  }

  // ReadableStreamError
  void streamError(Context cx, Scriptable scope, Object err) {
    state = State.ERRORED;
    error = err;
    if (reader != null) {
      reader.closed.reject(cx, scope, err);
      if (reader instanceof DefaultReader dr) {
        dr.errorReadRequests(cx, scope, err);
      } else {
        throw new AssertionError("BYOBReader not implemented yet");
      }
    }
  }

  // ReadableStreamClose
  void streamClose(Context cx, Scriptable scope) {
    state = State.CLOSED;
    if (reader != null) {
      reader.closed.fulfill(cx, scope, Undefined.instance);
      if (reader instanceof DefaultReader dr) {
        DefaultReader.ReadRequest rr;
        while ((rr = dr.readRequests.poll()) != null) {
          rr.getCloseSteps().handle(cx, scope);
        }
      }
    }
  }
}

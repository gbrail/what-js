package org.brail.jwhat.stream;

import java.util.ArrayList;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
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
    var constructor =
        new LambdaConstructor(
            scope,
            "WritableStream",
            2,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            WritableStream::constructor);
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

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new WritableStream();
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
                Context.getCurrentContext(), scope, ScriptRuntime.emptyArgs);
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

  private static final class PendingAbort {}
}

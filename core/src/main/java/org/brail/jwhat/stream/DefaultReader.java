package org.brail.jwhat.stream;

import java.util.ArrayDeque;
import org.brail.jwhat.core.impl.Errors;
import org.brail.jwhat.core.impl.PromiseAdapter;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class DefaultReader extends AbstractReader {
  ReadableStream stream;
  final ArrayDeque<ReadRequest> readRequests = new ArrayDeque<>();

  public static Constructable init(Context cx, Scriptable scope) {
    var c =
        new LambdaConstructor(
            scope,
            "ReadableStreamDefaultReader",
            1,
            LambdaConstructor.CONSTRUCTOR_DEFAULT,
            DefaultReader::constructor);
    c.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
    c.definePrototypeMethod(scope, "cancel", 1, DefaultReader::cancel);
    c.definePrototypeProperty(cx, "closed", DefaultReader::closed);
    c.definePrototypeMethod(scope, "read", 0, DefaultReader::read);
    c.definePrototypeMethod(scope, "releaseLock", 0, DefaultReader::releaseLock);
    return c;
  }

  private DefaultReader() {}

  @Override
  public String getClassName() {
    return "ReadableStreamDefaultReader";
  }

  private static DefaultReader realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, DefaultReader.class);
  }

  void setUp(Context cx, Scriptable scope, ReadableStream stream) {
    if (stream.isLocked()) {
      throw ScriptRuntime.typeError("stream is locked");
    }
    readRequests.clear();
    genericInitialize(cx, scope, stream);
  }

  // ReadableStreamReaderGenericInitialize
  private void genericInitialize(Context cx, Scriptable scope, ReadableStream stream) {
    this.stream = stream;
    stream.reader = this;
    closed =
        switch (stream.state) {
          case READABLE -> PromiseAdapter.uninitialized(cx, scope);
          case CLOSED -> PromiseAdapter.resolved(cx, scope, Undefined.instance);
          case ERRORED -> PromiseAdapter.rejected(cx, scope, stream.error);
        };
  }

  // ReadableStreamReaderGenericRelease
  private void genericRelease(Context cx, Scriptable scope) {
    if (stream.state == ReadableStream.State.READABLE) {
      closed.reject(cx, scope, Errors.newTypeError(cx, scope, "stream is still readable"));
    } else {
      closed =
          PromiseAdapter.rejected(cx, scope, Errors.newTypeError(cx, scope, "stream released"));
    }
    stream.controller.releaseSteps();
    stream.reader = null;
    stream = null;
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new DefaultReader();
  }

  private static Object cancel(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream == null) {
      return PromiseAdapter.rejected(cx, scope, Errors.newTypeError(cx, scope, "not locked"));
    }
    return self.stream.streamCancel(cx, scope, args.length > 0 ? args[0] : Undefined.instance);
  }

  private static Object read(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream == null) {
      return PromiseAdapter.rejected(cx, scope, Errors.newTypeError(cx, scope, "not locked"));
    }
    var p = PromiseAdapter.uninitialized(cx, scope);
    ValueHandler chunk =
        (lcx, ls, c) -> {
          var r = lcx.newObject(ls);
          r.put("value", r, c);
          r.put("done", r, false);
          p.fulfill(lcx, ls, r);
        };
    CloseHandler close =
        (lcx, ls) -> {
          var r = lcx.newObject(ls);
          r.put("value", r, Undefined.instance);
          r.put("done", r, true);
          p.fulfill(lcx, ls, r);
        };
    ValueHandler err = p::reject;
    var rr = new ReadRequest(chunk, close, err);
    self.readerRead(cx, scope, rr);
    return p;
  }

  private static Object releaseLock(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    if (self.stream != null) {
      self.genericRelease(cx, scope);
      var err = Errors.newTypeError(cx, scope, "reader is released");
      ReadRequest rr;
      while ((rr = self.readRequests.poll()) != null) {
        rr.errorSteps.handle(cx, scope, err);
      }
    }
    return Undefined.instance;
  }

  private static Object closed(Scriptable thisObj) {
    return realThis(thisObj).closed;
  }

  int getNumReadRequests() {
    return readRequests.size();
  }

  // ReadableStreamDefaultReaderRead
  private void readerRead(Context cx, Scriptable scope, ReadRequest rr) {
    stream.disturbed = true;
    switch (stream.state) {
      case CLOSED -> rr.closeSteps.handle(cx, scope);
      case ERRORED -> rr.errorSteps.handle(cx, scope, stream.error);
      case READABLE -> stream.controller.pullSteps(cx, scope, rr);
    }
  }

  // ReadableStreamDefaultReaderErrorReadRequests
  void errorReadRequests(Context cx, Scriptable scope, Object err) {
    ReadRequest rr;
    while ((rr = readRequests.poll()) != null) {
      rr.errorSteps.handle(cx, scope, err);
    }
  }

  interface ValueHandler {
    void handle(Context cx, Scriptable scope, Object val);
  }

  interface CloseHandler {
    void handle(Context cx, Scriptable scope);
  }

  record ReadRequest(ValueHandler chunkSteps, CloseHandler closeSteps, ValueHandler errorSteps) {
    ValueHandler getChunkSteps() {
      return chunkSteps;
    }

    CloseHandler getCloseSteps() {
      return closeSteps;
    }

    ValueHandler getErrorSteps() {
      return chunkSteps;
    }
  }
}

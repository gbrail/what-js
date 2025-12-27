package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseAdapter;
import org.brail.jwhat.events.AbortController;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.util.ArrayDeque;

class WritableController extends ScriptableObject {
  private WritableStream stream;
  private Object abortAlgorithm;
  private Object closeAlgorithm;
  private Callable startCb;
  private Object writeAlgorithm;
  private Callable getSizeCb;
  private Scriptable sink;
  // TODO controller class
  private AbortController abortController;
  private final ArrayDeque<Object> chunks = new ArrayDeque<>();
  private long queueSize;
  private double strategyHwm;
  private boolean started;

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

  private static WritableController realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, WritableController.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new WritableController();
  }

  private static Object error(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return Undefined.instance;
  }

  private static Object getSignal(Scriptable thisObj) {
    return Undefined.instance;
  }

  void setUp(Context cx, Scriptable scope, Object sinkObj, WritableStream stream,
             Callable sizeCb, double hwm) {
    this.stream = stream;
    this.abortController = (AbortController)cx.newObject(scope, "AbortController");
    this.getSizeCb = sizeCb;
    this.strategyHwm = hwm;
    // Works because queue should be empty now...
    stream.setBackpressure(hwm <= 0.0);
    setUpSink(cx, scope, sinkObj);
    var sa = PromiseAdapter.wrap(cx, scope,
            startCb.call(cx, scope, sink, new Object[] { this }));
    sa.then(cx, scope, (lcx, ls, val) -> {
      started = true;
      // TODO AdvanceQueueIfNeeded
    },
            (lcx, ls, val) -> {
      started = true;
      // TODO DealWithRejection
            });
  }

  private void setUpSink(Context cx, Scriptable scope, Object sinkObj) {
    var returnUndefined = new LambdaFunction(scope, "default", 0,
            (lcx, ls, to, args) -> Undefined.instance);
    this.startCb = returnUndefined;
    if (sinkObj == null) {
      return;
    }
    if (!(sinkObj instanceof Scriptable s)) {
      throw ScriptRuntime.typeError("Invalid sink type");
    }
    this.sink = s;
    if (s.has("type", s)) {
      throw ScriptRuntime.rangeError("Invalid sink");
    }
    if (ScriptableObject.getProperty(s, "start") instanceof Callable start) {
      this.startCb = start;
    }
    // TODO other three CBs
  }
}

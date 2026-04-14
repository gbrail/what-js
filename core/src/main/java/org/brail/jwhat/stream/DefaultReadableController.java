package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseWrapper;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.VarScope;
import org.mozilla.javascript.ScriptableObject;

public class DefaultReadableController extends ScriptableObject {
  private ReadableStream stream;
  private Callable cancelAlgorithm;
  private Callable pullAlgorithm;
  private Callable sizeAlgorithm;
  private boolean closeRequested;
  private boolean pullAgain;
  private boolean pulling;
  private boolean started;
  private double strategyHwm;
  private final QueueWithSize<Object> readQueue = new QueueWithSize<>();

  public static Constructable init(Context cx, VarScope scope) {
    var c =
        new LambdaConstructor(
            scope, "ReadableStreamDefaultController", 0, DefaultReadableController::constructor);
    c.definePrototypeMethod(scope, "close", 0, DefaultReadableController::close);
    c.definePrototypeMethod(scope, "enqueue", 1, DefaultReadableController::enqueue);
    c.definePrototypeMethod(scope, "error", 0, DefaultReadableController::error);
    c.definePrototypeProperty(cx, "desiredSize", DefaultReadableController::getDesiredSize);
    return c;
  }

  private DefaultReadableController() {}

  @Override
  public String getClassName() {
    return "ReadableStreamDefaultController;";
  }

  private static DefaultReadableController realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, DefaultReadableController.class);
  }

  private static Scriptable constructor(Context cx, VarScope scope, Object[] args) {
    return new DefaultReadableController();
  }

  private static Object getDesiredSize(Scriptable thisObj) {
    throw new AssertionError("Not implemented");
  }

  private static Object close(Context cx, VarScope scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError("Not implemented");
  }

  private static Object enqueue(Context cx, VarScope scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError("Not implemented");
  }

  private static Object error(Context cx, VarScope scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError("Not implemented");
  }

  // SetUpReadableStreamDefaultController
  void setUp(
    Context cx, VarScope scope,
    ReadableStream stream,
    Callable startAlgo,
    Callable pullAlgo,
    Callable cancelAlgo,
    double highWater,
    Callable sizeAlgo) {
      this.stream = stream;
      this.readQueue.reset();
      this.started = false;
      this.closeRequested = false;
      this.pullAgain = false;
      this.pulling = false;
      this.sizeAlgorithm = sizeAlgo;
      this.strategyHwm = highWater;
      this.pullAlgorithm = pullAlgo;
      this.cancelAlgorithm = cancelAlgo;
      stream.controller = this;
      var sa = PromiseWrapper.wrap(cx, scope, startAlgo.call(cx, scope, sink, ScriptRuntime.emptyArgs));
      sa.then(cx, scope,
        (lcx, ls, val) -> {
          started = true;
          pullIfNeeded(cx, scope);
        },
        (lcx, ls, val) -> {
          controllerError(cx, scope, val);
        }
      );
  }
}

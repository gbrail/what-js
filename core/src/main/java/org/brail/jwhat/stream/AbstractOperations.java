package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseAdapter;
import org.brail.jwhat.core.impl.Properties;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

class AbstractOperations {
  static Callable returnUndefinedPromise(Context cx, Scriptable scope) {
    return new LambdaFunction(
        scope,
        "default",
        0,
        (lcx, ls, to, args) -> PromiseAdapter.resolved(cx, scope, Undefined.instance));
  }

  static Callable returnUndefined(Context cx, Scriptable scope) {
    return new LambdaFunction(scope, "default", 0, (lcx, ls, to, args) -> Undefined.instance);
  }

  static Callable getSizeStrategy(Scriptable scope, Object stratObj) {
    if (stratObj instanceof Scriptable strategy) {
      var sizeFunc = Properties.getOptionalCallable(strategy, "size");
      if (sizeFunc != null) {
        return sizeFunc;
      }
    }
    return new LambdaFunction(scope, "getSize", 0, (cx1, scope1, thisObj, args) -> 1.0);
  }

  static double getHighWaterStrategy(Object stratObj, double dflt) {
    if (stratObj instanceof Scriptable strategy) {
      var hwmVal = Properties.getOptionalNumber(strategy, "highWaterMark", dflt);
      if (Double.isNaN(hwmVal) || hwmVal < 0.0) {
        throw ScriptRuntime.rangeError("Invalid HWM");
      }
      return hwmVal;
    }
    return dflt;
  }
}

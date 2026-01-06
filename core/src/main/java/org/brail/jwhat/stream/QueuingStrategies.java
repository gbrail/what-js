package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.Properties;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class QueuingStrategies extends ScriptableObject {
  private final double highWaterMark;

  public static void init(Context cx, Scriptable scope) {
    var cqs =
        new LambdaConstructor(scope, "CountQueuingStrategy", 1, QueuingStrategies::constructor);
    cqs.definePrototypeProperty(cx, "highWaterMark", QueuingStrategies::getHwm, DONTENUM);
    cqs.definePrototypeMethod(scope, "size", 0, QueuingStrategies::countSize);
    ScriptableObject.defineProperty(scope, "CountQueuingStrategy", cqs, DONTENUM);

    var bqs =
        new LambdaConstructor(
            scope, "ByteLengthQueuingStrategy", 1, QueuingStrategies::constructor);
    bqs.definePrototypeProperty(cx, "highWaterMark", QueuingStrategies::getHwm, DONTENUM);
    bqs.definePrototypeMethod(scope, "size", 1, QueuingStrategies::byteSize);
    ScriptableObject.defineProperty(scope, "ByteLengthQueuingStrategy", bqs, DONTENUM);
  }

  private QueuingStrategies(double hwm) {
    this.highWaterMark = hwm;
  }

  @Override
  public String getClassName() {
    return "QueuingStrategy";
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    // high water value is evaluated later, and is required, so default to NaN
    double hwm = Double.NaN;
    if (args.length > 0 && args[0] instanceof Scriptable s) {
      hwm = Properties.getOptionalNumber(s, "highWaterMark", Double.NaN);
    }
    return new QueuingStrategies(hwm);
  }

  private static Object getHwm(Scriptable thisObj) {
    var self = LambdaConstructor.convertThisObject(thisObj, QueuingStrategies.class);
    return self.highWaterMark;
  }

  private static Object countSize(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return 1;
  }

  private static Object byteSize(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    // In the spec, the caller validates this, so we can default to undefined
    if (args.length > 0 && args[0] instanceof Scriptable s) {
      var bl = ScriptableObject.getProperty(s, "byteLength");
      if (bl != Scriptable.NOT_FOUND) {
        return bl;
      }
    }
    return Undefined.instance;
  }
}

package org.brail.jwhat.core.impl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.VarScope;

public class Errors {
  public static Scriptable newTypeError(Context cx, VarScope scope, String msg) {
    return cx.newObject(scope, "TypeError", new Object[] {msg});
  }

  public static Scriptable newRangeError(Context cx, VarScope scope, String msg) {
    return cx.newObject(scope, "RangeError", new Object[] {msg});
  }
}

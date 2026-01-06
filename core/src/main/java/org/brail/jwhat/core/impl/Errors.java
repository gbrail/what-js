package org.brail.jwhat.core.impl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Errors {
  public static Scriptable newTypeError(Context cx, Scriptable scope, String msg) {
    return cx.newObject(scope, "TypeError", new Object[] {msg});
  }

  public static Scriptable newRangeError(Context cx, Scriptable scope, String msg) {
    return cx.newObject(scope, "RangeError", new Object[] {msg});
  }
}

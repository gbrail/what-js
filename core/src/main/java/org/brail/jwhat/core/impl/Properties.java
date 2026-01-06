package org.brail.jwhat.core.impl;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class Properties {
  /**
   * If "name" is not undefined, and a Callable, return it. If it is defined but not a Callable,
   * throw TypeError.
   */
  public static Callable getOptionalCallable(Scriptable s, String name) {
    var o = ScriptableObject.getProperty(s, name);
    if (o == Scriptable.NOT_FOUND || Undefined.isUndefined(o)) {
      return null;
    }
    if (!(o instanceof Callable c)) {
      throw ScriptRuntime.typeError("not a function");
    }
    return c;
  }

  /** If "name" is not undefined, convert to a number using toNumber. */
  public static double getOptionalNumber(Scriptable s, String name, double dflt) {
    var o = ScriptableObject.getProperty(s, name);
    if (o == null || o == Scriptable.NOT_FOUND || Undefined.isUndefined(o)) {
      return dflt;
    }
    return ScriptRuntime.toNumber(o);
  }
}

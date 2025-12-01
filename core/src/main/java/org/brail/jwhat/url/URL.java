package org.brail.jwhat.url;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class URL {
  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "URL", 1, URL::constructor);
    ScriptableObject.defineProperty(scope, "URL", c, ScriptableObject.DONTENUM);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return cx.newObject(scope);
  }
}

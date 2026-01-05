package org.brail.jwhat.framework;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class TestUtils extends ScriptableObject {
  public static void init(Context cx, Scriptable scope) {
    var to = cx.newObject(scope);
    ScriptableObject.defineProperty(
        to, "gc", new LambdaFunction(scope, "gc", 0, TestUtils::gc), DONTENUM);
    ScriptableObject.defineProperty(scope, "TestUtils", to, DONTENUM);
  }

  private static Object gc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    System.gc();
    return Undefined.instance;
  }

  @Override
  public String getClassName() {
    return "TestUtils";
  }
}

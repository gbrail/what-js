package org.brail.jwhat.stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class ReadableStream extends ScriptableObject {
  public static void init(Context cx, Scriptable scope) {
    // TODO call DefaultReader and ReadableController init
    throw new AssertionError("Not implemented");
  }

  @Override
  public String getClassName() {
    return "ReadableStream";
  }
}

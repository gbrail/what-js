package org.brail.jwhat.stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.VarScope;

public class ReadableStream extends ScriptableObject {
  DefaultReadableController controller;

  public static void init(Context cx, VarScope scope) {
    // TODO call DefaultReader and ReadableController init
    throw new AssertionError("Not implemented");
  }

  @Override
  public String getClassName() {
    return "ReadableStream";
  }
}

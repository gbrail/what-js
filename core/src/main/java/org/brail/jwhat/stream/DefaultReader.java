package org.brail.jwhat.stream;

import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class DefaultReader extends ScriptableObject {
  public static Constructable init(Context cx, Scriptable scope) {
    throw new AssertionError("Not implemented");
  }

  @Override
  public String getClassName() {
    return "ReadableStreamDefaultReader";
  }
}

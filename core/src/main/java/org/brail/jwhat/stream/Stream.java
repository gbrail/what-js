package org.brail.jwhat.stream;

import java.io.IOException;
import org.brail.jwhat.core.impl.Utils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Stream {
  public static void init(Context cx, Scriptable scope) {
    loadResource(cx, scope, "org/brail/jwhat/stream/stream.js");
    WritableStream.init(cx, scope);
  }

  private static void loadResource(Context cx, Scriptable scope, String path) {
    try {
      Utils.evaluateResource(cx, scope, path);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}

package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.Utils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;

public class Stream {
  public static void init(Context cx, Scriptable scope) {
    try {
      Utils.evaluateResource(cx, scope, "org/brail/jwhat/stream/stream.js");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

package org.brail.jwhat.stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Stream {
  public static void init(Context cx, Scriptable scope) {
    ReadableStream.init(cx, scope);
    WritableStream.init(cx, scope);
    QueuingStrategies.init(cx, scope);
  }
}

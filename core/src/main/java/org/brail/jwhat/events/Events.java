package org.brail.jwhat.events;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.VarScope;

public class Events {
  public static void init(Context cx, VarScope scope) {
    Event.init(cx, scope);
    EventTarget.init(cx, scope);
    AbortSignal.init(cx, scope);
    AbortController.init(cx, scope);
  }
}

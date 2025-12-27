package org.brail.jwhat.events;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Events {
  public static void init(Context cx, Scriptable scope) {
    Event.init(cx, scope);
    EventTarget.init(cx, scope);
    AbortSignal.init(cx, scope);
    AbortController.init(cx, scope);
  }
}

package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseWrapper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public abstract class AbstractReadableController extends ScriptableObject {
  abstract PromiseWrapper cancelSteps(Context cx, Scriptable scope, Object reason);
  abstract void pullSteps(Context cx, Scriptable scope, DefaultReader.ReadRequest rr);
  abstract void releaseSteps();
}

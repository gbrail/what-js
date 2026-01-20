package org.brail.jwhat.stream;

import org.brail.jwhat.core.impl.PromiseAdapter;
import org.mozilla.javascript.ScriptableObject;

public abstract class AbstractReader extends ScriptableObject {
  protected PromiseAdapter closed;
}

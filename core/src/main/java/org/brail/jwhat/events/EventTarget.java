package org.brail.jwhat.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.brail.jwhat.core.impl.Properties;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class EventTarget extends ScriptableObject {
  private final HashMap<String, List<Listener>> listeners = new HashMap<>();

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "EventTarget", 0, EventTarget::constructor);
    c.definePrototypeMethod(scope, "addEventListener", 2, EventTarget::addListener);
    c.definePrototypeMethod(scope, "removeEventListener", 2, EventTarget::removeListener);
    c.definePrototypeMethod(scope, "dispatchEvent", 1, EventTarget::dispatch);
    ScriptableObject.defineProperty(scope, "EventTarget", c, DONTENUM);
  }

  private EventTarget() {}

  @Override
  public String getClassName() {
    return "Event";
  }

  private static EventTarget realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, EventTarget.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new EventTarget();
  }

  private static Object addListener(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length < 1) {
      return Undefined.instance;
    }
    String type = ScriptRuntime.toString(args[0]);
    Object cb = args.length > 1 ? args[1] : null;
    Object opts = args.length > 2 ? args[2] : null;
    var options = flattenMoreOptions(opts);

    AbortSignal abortSignal = null;
    if (options.signal instanceof AbortSignal as) {
      abortSignal = as;
    }
    if (abortSignal != null && abortSignal.isAborted()) {
      return Undefined.instance;
    }
    if (!(cb instanceof Scriptable callback)) {
      return Undefined.instance;
    }
    // Default passive value, but we don't have windows
    Object passive = options.passive == null ? false : options.passive;

    var listener = new Listener();
    listener.type = type;
    listener.callback = callback;
    listener.capture = options.capture;
    listener.passive = options.passive;
    listener.once = options.once;
    listener.signal = abortSignal;

    var self = realThis(thisObj);
    self.listeners.compute(
        type,
        (k, l) -> {
          ArrayList<Listener> listeners;
          if (l == null) {
            listeners = new ArrayList<>();
          } else {
            listeners = (ArrayList<Listener>) l;
          }
          if (!listeners.contains(listener)) {
            listeners.add(listener);
          }
          return listeners;
        });

    if (abortSignal != null) {
      abortSignal.addStep(
          (lcx, ls, to, la) -> {
            self.removeListener(listener);
            return Undefined.instance;
          });
    }

    return Undefined.instance;
  }

  private void removeListener(Listener l) {
    var typeListener = listeners.get(l.type);
    if (typeListener != null) {
      // TODO probably have to set up more stuff or use == to compare here
      typeListener.remove(l);
    }
  }

  private static Object removeListener(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length < 1) {
      return Undefined.instance;
    }
    String type = ScriptRuntime.toString(args[0]);
    Object cb = args.length > 1 ? args[1] : null;
    if (!(cb instanceof ScriptableObject callback)) {
      return Undefined.instance;
    }
    Object opts = args.length > 2 ? args[2] : null;
    boolean capture = flattenOptions(opts);

    var listener = new Listener();
    listener.type = type;
    listener.callback = callback;
    listener.capture = capture;

    var listeners = realThis(thisObj).listeners.get(type);
    if (listeners != null) {
      listeners.remove(listener);
    }
    return Undefined.instance;
  }

  private static Object dispatch(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length < 1) {
      return false;
    }
    if (!(args[0] instanceof Scriptable event)) {
      return false;
    }
    Object typeObj = ScriptableObject.getProperty(event, "type");
    if (typeObj == Scriptable.NOT_FOUND) {
      return false;
    }
    String type = ScriptRuntime.toString(typeObj);
    List<Listener> listeners = realThis(thisObj).listeners.get(type);
    if (listeners == null || listeners.isEmpty()) {
      return false;
    }
    throw new AssertionError("TODO: implement Dispatching Events section 2.9");
    /*
    for (var l : listeners) {
      l.callback.call(cx, scope, l.target, new Object[] {event});
    }
    return true;
     */
  }

  private static MoreOptions flattenMoreOptions(Object o) {
    boolean capture = flattenOptions(o);
    boolean once = false;
    Object passive = null;
    Object signal = null;
    if (o instanceof Scriptable s) {
      once = Properties.getOptionalBoolean(s, "once", false);
      passive = Properties.getOptionalValue(s, "passive", null);
      signal = Properties.getOptionalValue(s, "signal", null);
    }
    return new MoreOptions(capture, once, passive, signal);
  }

  private static boolean flattenOptions(Object o) {
    if (o instanceof Boolean bo) {
      return bo;
    }
    if (o instanceof Scriptable s) {
      return Properties.getOptionalBoolean(s, "capture", false);
    }
    return false;
  }

  private static final class Listener {
    String type;
    Scriptable callback;
    boolean capture;
    Object passive;
    boolean once;
    AbortSignal signal;
    boolean removed;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Listener listener = (Listener) o;
      return Objects.equals(type, listener.type)
          && callback == listener.callback
          && capture == listener.capture;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, callback, capture);
    }
  }

  private record MoreOptions(boolean capture, boolean once, Object passive, Object signal) {}
}

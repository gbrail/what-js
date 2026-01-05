package org.brail.jwhat.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.mozilla.javascript.Callable;
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
    if (args.length < 2) {
      return Undefined.instance;
    }
    String type = ScriptRuntime.toString(args[0]);
    if (!(args[1] instanceof Scriptable target)) {
      return Undefined.instance;
    }
    var o = ScriptableObject.getProperty(target, "handleEvent");
    if (!(o instanceof Callable tb)) {
      return Undefined.instance;
    }
    var listener = new Listener(type, target, tb);
    realThis(thisObj)
        .listeners
        .compute(
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
    return Undefined.instance;
  }

  private static Object removeListener(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length < 2) {
      return Undefined.instance;
    }
    String type = ScriptRuntime.toString(args[0]);
    if (!(args[1] instanceof Scriptable target)) {
      return Undefined.instance;
    }
    var o = ScriptableObject.getProperty(target, "handleEvent");
    if (!(o instanceof Callable tb)) {
      return Undefined.instance;
    }
    var listener = new Listener(type, target, tb);
    var listeners = realThis(thisObj).listeners.get(type);
    listeners.remove(listener);
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
    for (var l : listeners) {
      l.callback.call(cx, scope, l.target, new Object[] {event});
    }
    return true;
  }

  private static final class Listener {
    final String type;
    final Scriptable target;
    final Callable callback;

    Listener(String type, Scriptable target, Callable callback) {
      this.type = type;
      this.target = target;
      this.callback = callback;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Listener listener = (Listener) o;
      return Objects.equals(type, listener.type) && target == listener.target;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, target);
    }
  }
}

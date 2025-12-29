package org.brail.jwhat.events;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Event extends ScriptableObject {
  private final String type;
  private EventTarget target;

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "Event", 1, Event::constructor);
    c.definePrototypeProperty(cx, "type", Event::getType);
    c.definePrototypeProperty(cx, "target", Event::getTarget);
    ScriptableObject.defineProperty(scope, "Event", c, DONTENUM);
  }

  @Override
  public String getClassName() {
    return "Event";
  }

  private Event(String type) {
    this.type = type;
  }

  private static Event realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, Event.class);
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    String type = args.length > 0 ? ScriptRuntime.toString(args[0]) : "";
    return new Event(type);
  }

  private static Object getType(Scriptable thisObj) {
    return realThis(thisObj).type;
  }

  private static Object getTarget(Scriptable thisObj) {
    return realThis(thisObj).target;
  }
}

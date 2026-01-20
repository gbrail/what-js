package org.brail.jwhat.stream;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.typedarrays.NativeArrayBufferView;

public class BYOBRequest extends ScriptableObject {
  ReadableByteStreamController controller;
  NativeArrayBufferView view;

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "ReadableStreamBYOBRequest", 0, BYOBRequest::constructor);
    c.definePrototypeProperty(cx, "view", BYOBRequest::getView);
    c.definePrototypeMethod(scope, "respond", 1, BYOBRequest::respond);
    c.definePrototypeMethod(scope, "respondWithNewView", 1, BYOBRequest::respondWithNewView);
    ScriptableObject.defineProperty(scope, "ReadableStreamBYOBRequest", c, DONTENUM);
  }

  @Override
  public String getClassName() {
    return "ReadableStreamBYOBRequest";
  }

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    return new BYOBRequest();
  }

  private static BYOBRequest realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, BYOBRequest.class);
  }

  private static Object getView(Scriptable thisObj) {
    return realThis(thisObj).view;
  }

  private static Object respond(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError();
  }

  private static Object respondWithNewView(
      Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    throw new AssertionError();
  }
}

package org.brail.jwhat.url;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class URLSearchParams extends ScriptableObject {
  private static final Pattern SEP = Pattern.compile("&");

  private final ArrayList<Map.Entry<String, String>> params = new ArrayList<>();

  static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "URLSearchParams", 1, URLSearchParams::constructor);
    /*c.definePrototypeProperty(cx, "size", URLSearchParams::getSize);
    c.definePrototypeMethod(scope, "append", 2, URLSearchParams::append);
    c.definePrototypeMethod(scope, "delete", 2, URLSearchParams::delete);
    c.definePrototypeMethod(scope, "get", 1, URLSearchParams::get);
    c.definePrototypeMethod(scope, "getAll", 1, URLSearchParams::getAll);
    c.definePrototypeMethod(scope, "has", 1, URLSearchParams::has);
    c.definePrototypeMethod(scope, "set", 2, URLSearchParams::set);
    c.definePrototypeMethod(scope, "sort", 0, URLSearchParams::sort);*/
    ScriptableObject.defineProperty(scope, "URLSearchParams", c, ScriptableObject.DONTENUM);
  }

  private static Scriptable constructor(Context context, Scriptable scope, Object[] args) {
    var p = new URLSearchParams();
    if (args.length > 0) {
      if (args[0] instanceof Scriptable) {
        p.loadScriptable((Scriptable) args[0]);
      } else {
        p.loadString(args[0]);
      }
    }
    return p;
  }

  private void loadScriptable(Scriptable s) {
    Object[] elts = getArrayElementsIfPossible(s);
    if (elts != null) {
      loadArray(elts);
    } else {
      for (Object k : s.getIds()) {
        String key = ScriptRuntime.toString(k);
        Object val = s.get(key, s);
        if (val != Scriptable.NOT_FOUND) {
          String valStr = ScriptRuntime.toString(val);
          params.add(new AbstractMap.SimpleEntry<>(key, valStr));
        }
      }
    }
  }

  private void loadArray(Object[] a) {
    for (Object elt : a) {
      if (elt instanceof Scriptable) {
        Object[] elts = getArrayElementsIfPossible((Scriptable) elt);
        if (elts != null && elts.length == 2) {
          params.add(
              new AbstractMap.SimpleEntry<>(
                  ScriptRuntime.toString(elts[0]), ScriptRuntime.toString(elts[1])));
        } else {
          throw ScriptRuntime.typeError("Invalid array element");
        }
      } else {
        throw ScriptRuntime.typeError("Invalid sequence");
      }
    }
  }

  private void loadString(Object o) {
    String s = ScriptRuntime.toString(o);
    for (String part : SEP.split(s)) {
      if (!part.isEmpty()) {}
    }
  }

  @Override
  public String getClassName() {
    return "URLSearchParams";
  }

  /**
   * If the scriptable has a "length" property, then attempt to treat it as an array, and otherwise
   * return null.
   */
  private static Object[] getArrayElementsIfPossible(Scriptable s) {
    Object lengthParam = s.get("length", s);
    if (lengthParam instanceof Number) {
      int len = ScriptRuntime.toInt32(ScriptRuntime.toNumber(lengthParam));
      Object[] ret = new Object[len];
      for (int i = 0; i < len; i++) {
        Object e = s.get(i, s);
        if (e == Scriptable.NOT_FOUND) {
          return null;
        }
        ret[i] = e;
      }
      return ret;
    }
    return null;
  }
}

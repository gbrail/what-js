package org.brail.jwhat.url;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.brail.jwhat.core.impl.URLUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

public class URLSearchParams extends ScriptableObject {
  private List<Map.Entry<String, String>> params;
  private URL url;

  static LambdaConstructor init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "URLSearchParams", 1, URLSearchParams::constructor);
    c.definePrototypeProperty(cx, "size", URLSearchParams::getSize);
    c.definePrototypeMethod(scope, "append", 2, URLSearchParams::append);
    c.definePrototypeMethod(scope, "delete", 2, URLSearchParams::delete);
    c.definePrototypeMethod(scope, "get", 1, URLSearchParams::get);
    c.definePrototypeMethod(scope, "getAll", 1, URLSearchParams::getAll);
    c.definePrototypeMethod(scope, "has", 1, URLSearchParams::has);
    c.definePrototypeMethod(scope, "set", 2, URLSearchParams::set);
    c.definePrototypeMethod(scope, "sort", 0, URLSearchParams::sort);
    c.definePrototypeMethod(scope, "toString", 0, URLSearchParams::stringify);
    c.definePrototypeProperty(SymbolKey.TO_STRING_TAG, "URLSearchParams", DONTENUM | READONLY);
    ScriptableObject.defineProperty(scope, "URLSearchParams", c, ScriptableObject.DONTENUM);
    return c;
  }

  @Override
  public String toString() {
    return URLUtils.encodeURLEncoded(params);
  }

  private static Object stringify(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    return self.toString();
  }

  private static Scriptable constructor(Context context, Scriptable scope, Object[] args) {
    var p = new URLSearchParams();
    if (args.length > 0) {
      Object arg = args[0];
      if (args[0] instanceof URLSearchParams) {
        var pargs = (URLSearchParams) arg;
        p.params = new ArrayList<>(pargs.params);
        return p;
      } else if (arg instanceof Scriptable) {
        p.params = new ArrayList<>();
        p.loadScriptable((Scriptable) arg);
        return p;
      } else if (arg != null && !Undefined.isUndefined(arg)) {
        p.loadString(arg);
        return p;
      }
    }
    p.params = new ArrayList<>();
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
    if (s.startsWith("?")) {
      s = s.substring(1);
    }
    params = URLUtils.decodeURLEncoded(s);
  }

  @Override
  public String getClassName() {
    return "URLSearchParams";
  }

  private static URLSearchParams realThis(Scriptable s) {
    return LambdaConstructor.convertThisObject(s, URLSearchParams.class);
  }

  void setURL(URL url) {
    this.url = url;
  }

  void updateParent() {
    if (url != null) {
      url.setQuery(toString());
    }
  }

  void reparse(String q) {
    if (q != null) {
      params = URLUtils.decodeURLEncoded(q);
    } else {
      params = new ArrayList<>();
    }
  }

  private static Object getSize(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.params.size();
  }

  private static Object append(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length > 0) {
      String name = ScriptRuntime.toString(args[0]);
      String value = null;
      if (args.length > 1) {
        value = ScriptRuntime.toString(args[1]);
      }
      var self = realThis(thisObj);
      self.params.add(new AbstractMap.SimpleEntry<>(name, value));
      self.updateParent();
    }
    return Undefined.instance;
  }

  private static Object delete(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length > 0) {
      String name = ScriptRuntime.toString(args[0]);
      String value = null;
      if (args.length > 1 && !Undefined.isUndefined(args[1])) {
        value = ScriptRuntime.toString(args[1]);
      }
      var self = realThis(thisObj);
      if (value != null) {
        String vvalue = value;
        self.params.removeIf((e) -> name.equals(e.getKey()) && vvalue.equals(e.getValue()));
      } else {
        self.params.removeIf((e) -> name.equals(e.getKey()));
      }
      self.updateParent();
    }
    return Undefined.instance;
  }

  private static Object get(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length > 0) {
      String name = ScriptRuntime.toString(args[0]);
      var self = realThis(thisObj);
      for (var e : self.params) {
        if (name.equals(e.getKey())) {
          return e.getValue();
        }
      }
    }
    return null;
  }

  private static Object getAll(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var a = new ArrayList<String>();
    if (args.length > 0) {
      String name = ScriptRuntime.toString(args[0]);
      var self = realThis(thisObj);
      for (var e : self.params) {
        if (name.equals(e.getKey())) {
          a.add(e.getValue());
        }
      }
    }
    return cx.newArray(scope, a.toArray());
  }

  private static Object has(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length > 0) {
      String name = ScriptRuntime.toString(args[0]);
      String value = null;
      if (args.length > 1 && !Undefined.isUndefined(args[1])) {
        value = ScriptRuntime.toString(args[1]);
      }
      var self = realThis(thisObj);
      for (var e : self.params) {
        if (name.equals(e.getKey()) && (value == null || value.equals(e.getValue()))) {
          return true;
        }
      }
    }
    return false;
  }

  private static Object set(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length > 1) {
      String name = ScriptRuntime.toString(args[0]);
      String value = ScriptRuntime.toString(args[1]);
      var self = realThis(thisObj);
      var it = self.params.iterator();
      boolean removed = false;
      while (it.hasNext()) {
        var e = it.next();
        if (name.equals(e.getKey())) {
          if (removed) {
            it.remove();
          } else {
            e.setValue(value);
            removed = true;
          }
        }
      }
      if (!removed) {
        self.params.add(new AbstractMap.SimpleEntry<>(name, value));
      }
      self.updateParent();
    }
    return Undefined.instance;
  }

  private static Object sort(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    var self = realThis(thisObj);
    self.params.sort((a, b) -> a.getKey().compareTo(b.getKey()));
    return Undefined.instance;
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

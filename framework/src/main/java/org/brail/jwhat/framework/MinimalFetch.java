package org.brail.jwhat.framework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class MinimalFetch {
  public static void init(Context cx, Scriptable scope) throws IOException {
    var loadFunc = new LambdaFunction(scope, "load", 1, MinimalFetch::load);
    String miniFetch = Utils.readResource("org/brail/jwhat/framework/mini-fetch.js");
    var exports = (Scriptable) cx.evaluateString(scope, miniFetch, "mini-fetch.js", 1, null);
    var loadFetch = (Function) exports.get("loadFetch", exports);
    var fetchFunc = (Function) loadFetch.call(cx, scope, null, new Object[] {loadFunc});
    ScriptableObject.defineProperty(scope, "fetch", fetchFunc, ScriptableObject.DONTENUM);
    var setBaseFunc = (Function) exports.get("setBase", exports);
    ScriptableObject.defineProperty(
        scope, "__setFetchBase", setBaseFunc, ScriptableObject.DONTENUM);
  }

  private static Object load(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args.length > 0) {
      String name = ScriptRuntime.toString(args[0]);
      Path p = Path.of(name);
      if (!p.isAbsolute() && args.length > 1) {
        String base = ScriptRuntime.toString(args[1]);
        p = Path.of(base, name);
      }
      try {
        return Files.readString(p, StandardCharsets.UTF_8);
      } catch (IOException ioe) {
        throw ScriptRuntime.constructError("Error", ioe.toString());
      }
    }
    return Undefined.instance;
  }
}

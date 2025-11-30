package org.brail.jwhat.framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class WPTTestLauncher {
  private static final String TEST_BASE = "../wpt";

  private final CharSequence testHarness;

  public static WPTTestLauncher newLauncher() throws IOException {
    // Build a test script that includes the standard harness and all of our
    // code to link it to Java.
    StringBuilder completeTestScript = new StringBuilder();

    try (InputStream in =
        WPTTestLauncher.class
            .getClassLoader()
            .getResourceAsStream("org/brail/whatjs/framework/testharness.js")) {
      if (in == null) {
        throw new IOException("Could not find testharness.js resource");
      }
      try (InputStreamReader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        char[] buffer = new char[4096];
        int bytesRead;
        while ((bytesRead = rdr.read(buffer)) != -1) {
          completeTestScript.append(buffer, 0, bytesRead);
        }
      }
    }

    completeTestScript.append(
        """
    setup(null, {
      debug: true
    });
    add_result_callback(__testResultTracker);
    add_completion_callback(__testCompletionTracker);
    """);

    return new WPTTestLauncher(completeTestScript);
  }

  private WPTTestLauncher(CharSequence script) {
    this.testHarness = script;
  }

  private Scriptable initializeScope(Context cx, ResultTracker tracker) {
    Scriptable scope = cx.initStandardObjects();
    scope.put("self", scope, scope);
    scope.put("console", scope, MinimalConsole.init(cx, scope));
    scope.put("__testResultTracker", scope, tracker.getResultCallback(scope));
    scope.put("__testCompletionTracker", scope, tracker.getCompletionCallback(scope));
    scope.put("__testDefer", scope, new LambdaFunction(scope, "defer", 1, WPTTestLauncher::defer));
    return scope;
  }

  private static Object defer(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    assert args.length >= 1;
    Function deferFunc = (Function) args[0];
    cx.enqueueMicrotask(
        () -> {
          deferFunc.call(cx, scope, null, ScriptRuntime.emptyArgs);
        });
    return Undefined.instance;
  }

  /*public void runTest(Context cx, String testPath) throws IOException {
    String testScript = Files.readString(Path.of(TEST_BASE, testPath));
    cx.evaluateString(scope, testScript, testPath, 1, null);
  }*/

  public ResultTracker runScript(Context cx, CharSequence script) {
    var tracker = new ResultTracker();
    // Run each test suite in a separate scope to prevent cross-contamination.
    Scriptable scope = initializeScope(cx, tracker);
    // For the harness to actually work, build the whole harness and our
    // test script into a big script and run it all together.

    var testScript = new StringBuilder(testHarness);
    testScript.append(script);
    cx.evaluateString(scope, testScript.toString(), "test.js", 1, null);

    return tracker;
  }
}

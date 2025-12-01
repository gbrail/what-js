package org.brail.jwhat.framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class WPTTestLauncher {
  private static final String TEST_BASE = "../wpt";

  private final CharSequence testHarness;
  private BiConsumer<Context, Scriptable> setupCallback;

  public static WPTTestLauncher newLauncher() throws IOException {
    // Build a test script that includes the standard harness and all of our
    // code to link it to Java.
    StringBuilder completeTestScript = new StringBuilder();

    try (InputStream in =
        WPTTestLauncher.class
            .getClassLoader()
            .getResourceAsStream("org/brail/jwhat/framework/testharness.js")) {
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

  public void setSetupCallback(BiConsumer<Context, Scriptable> callback) {
    this.setupCallback = callback;
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
    if (setupCallback != null) {
      setupCallback.accept(cx, scope);
    }
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

  public ResultTracker runScript(Context cx, CharSequence script) {
    var tracker = new ResultTracker();
    // Run each test suite in a separate scope to prevent cross-contamination.
    var scope = initializeScope(cx, tracker);
    // For the harness to actually work, build the whole harness and our
    // test script into a big script and run it all together.
    cx.evaluateString(scope, String.valueOf(testHarness) + script, "test.js", 1, null);
    return tracker;
  }

  public ResultTracker runFile(Context cx, String path) throws IOException {
    var tracker = new ResultTracker();
    var scope = initializeScope(cx, tracker);
    var absPath = Path.of(TEST_BASE, path);
    var script = ScriptFixer.fixScript(Files.readString(absPath));
    cx.evaluateString(scope, testHarness + script, absPath.getFileName().toString(), 1, null);
    return tracker;
  }

  public static Object[] findTests(String path) {
    return findTests(path, new String[] {});
  }

  public static Object[] findTests(String path, String[] exclusions) {
    var base = Path.of(TEST_BASE, path);
    ArrayList<Object> results = new ArrayList<>();
    try {
      Files.walkFileTree(
          base,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
              if (p.toFile().isFile()) {
                var name = p.toString();
                if (name.endsWith(".any.js") && !isExcluded(name, exclusions)) {
                  results.add(p.toString());
                }
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return results.toArray();
  }

  private static boolean isExcluded(String name, String[] exclusions) {
    for (String ex : exclusions) {
      if (name.contains(ex)) {
        return true;
      }
    }
    return false;
  }
}

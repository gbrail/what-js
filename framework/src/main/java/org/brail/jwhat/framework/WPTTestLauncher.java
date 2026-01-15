package org.brail.jwhat.framework;

import java.io.IOException;
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
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class WPTTestLauncher {
  static final String TEST_BASE = "../testcases";

  private final Script testHarness;
  private final ArrayList<Script> testResources = new ArrayList<>();
  private BiConsumer<Context, Scriptable> setupCallback;

  public static WPTTestLauncher newLauncher() throws IOException {
    // Build a test script that includes the standard harness and all of our
    // code to link it to Java.
    String harness = Utils.readResource("org/brail/jwhat/framework/testharness.js");
    StringBuilder completeTestScript = new StringBuilder(harness);

    completeTestScript.append(
        """
    setup(null, {
      debug: false
    });
    add_result_callback(__testResultTracker);
    add_completion_callback(__testCompletionTracker);
    """);

    try (Context cx = Context.enter()) {
      var cs = cx.compileString(completeTestScript.toString(), "testharness.js", 1, null);
      return new WPTTestLauncher(cs);
    }
  }

  public void setSetupCallback(BiConsumer<Context, Scriptable> callback) {
    this.setupCallback = callback;
  }

  public void addResource(String path) throws IOException {
    try (Context cx = Context.enter()) {
      try (var rdr = Utils.openResource("org/brail/jwhat/framework/" + path)) {
        var s = cx.compileReader(rdr, path, 1, null);
        testResources.add(s);
      }
    }
  }

  private WPTTestLauncher(Script s) {
    this.testHarness = s;
  }

  private Scriptable initializeScope(Context cx, ResultTracker tracker) throws IOException {
    Scriptable scope = cx.initStandardObjects();
    MinimalFetch.init(cx, scope);
    // Parts of the test framework use "self" to find the global scope
    scope.put("self", scope, scope);
    // Some tests use the console
    scope.put("console", scope, MinimalConsole.init(cx, scope));
    // Tools used by the test framework to hook into our stuff
    TestUtils.init(cx, scope);
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

  public ResultTracker runScript(Context cx, CharSequence script) throws IOException {
    var tracker = new ResultTracker();
    // Run each test suite in a separate scope to prevent cross-contamination.
    var scope = initializeScope(cx, tracker);
    // The framework expects promises to not be resolved until everything is run
    // as if it were one big giant script.
    cx.suspendMicrotaskProcessing();
    try {
      testHarness.exec(cx, scope, null);
      cx.evaluateString(scope, script.toString(), "test.js", 1, null);
    } finally {
      cx.resumeMicrotaskProcessing();
    }
    return tracker;
  }

  public ResultTracker runFile(Context cx, String path) throws IOException {
    var tracker = new ResultTracker();
    var scope = initializeScope(cx, tracker);
    var testPath = Path.of(path);
    var script = Files.readString(testPath);
    var setBase = (Function) scope.get("__setFetchBase", scope);
    var pp = testPath.getParent();
    if (pp.getNameCount() <= 2) {
      throw new AssertionError("too short: " + pp);
    }
    setBase.call(cx, scope, null, new Object[] {testPath.getParent().toString()});
    cx.suspendMicrotaskProcessing();
    try {
      testHarness.exec(cx, scope, null);
      for (var r : testResources) {
        r.exec(cx, scope, null);
      }
      HelperLoader.loadHelpers(cx, scope, script, testPath.getParent());
      cx.evaluateString(scope, script, testPath.getFileName().toString(), 1, null);
    } catch (Throwable t) {
      tracker.trackException(t);
    } finally {
      cx.resumeMicrotaskProcessing();
    }
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
    if (name.contains("idlharness")) {
      return true;
    }
    for (String ex : exclusions) {
      if (name.contains(ex)) {
        return true;
      }
    }
    return false;
  }
}

package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.brail.jwhat.console.Console;
import org.brail.jwhat.core.impl.PromiseAdapter;
import org.brail.jwhat.core.impl.PromiseWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class PromiseAdapterTests {
  private Context cx;
  private Scriptable scope;

  @BeforeEach
  public void init() {
    cx = Context.enter();
    scope = cx.initStandardObjects();
    Console.builder().install(cx, scope);
  }

  @AfterEach
  public void cleanup() {
    Context.exit();
  }

  @Test
  public void testResolve() {
    var p = PromiseAdapter.uninitialized(cx, scope);
    scope.put("p", scope, p.getPromise());
    scope.put("resolved", scope, false);
    scope.put("rejected", scope, false);
    cx.evaluateString(
        scope,
        """
        p.then((v) => { resolved = v; });
        p.catch((e) => { rejected = e; });
        """,
        "test.js",
        1,
        null);
    p.fulfill(cx, scope, "Hello");
    cx.processMicrotasks();
    assertEquals("Hello", scope.get("resolved", scope));
    assertEquals(false, scope.get("rejected", scope));
  }

  @Test
  public void testReject() {
    var p = PromiseAdapter.uninitialized(cx, scope);
    scope.put("p", scope, p.getPromise());
    scope.put("resolved", scope, false);
    scope.put("rejected", scope, false);
    cx.evaluateString(
        scope,
        """
            p.then((v) => { resolved = v; });
            p.catch((e) => { rejected = e; });
            """,
        "test.js",
        1,
        null);
    p.reject(cx, scope, "Goodbye");
    cx.processMicrotasks();
    assertEquals(false, scope.get("resolved", scope));
    assertEquals("Goodbye", scope.get("rejected", scope));
  }

  @Test
  public void testResolved() {
    var p = PromiseAdapter.resolved(cx, scope, "Yay");
    scope.put("p", scope, p.getPromise());
    scope.put("resolved", scope, false);
    scope.put("rejected", scope, false);
    cx.evaluateString(
        scope,
        """
            p.then((v) => { resolved = v; });
            p.catch((e) => { rejected = e; });
            """,
        "test.js",
        1,
        null);
    assertEquals("Yay", scope.get("resolved", scope));
    assertEquals(false, scope.get("rejected", scope));
  }

  @Test
  public void testRejected() {
    var p = PromiseAdapter.rejected(cx, scope, "Nay");
    scope.put("p", scope, p.getPromise());
    scope.put("resolved", scope, false);
    scope.put("rejected", scope, false);
    cx.evaluateString(
        scope,
        """
            p.then((v) => { resolved = v; });
            p.catch((e) => { rejected = e; });
            """,
        "test.js",
        1,
        null);
    assertEquals(false, scope.get("resolved", scope));
    assertEquals("Nay", scope.get("rejected", scope));
  }

  @Test
  public void testThenCallback() {
    scope.put("ResolveFunc", scope, null);
    scope.put("p", scope, null);
    cx.evaluateString(
        scope,
        """
                p = new Promise((resolve, reject) => { ResolveFunc = resolve; });
                """,
        "test.js",
        1,
        null);
    var p = scope.get("p", scope);
    var pa = PromiseWrapper.wrap(cx, scope, p);
    AtomicBoolean done = new AtomicBoolean();
    pa.then(cx, scope, (lcx, ls, val) -> done.set(true));
    cx.evaluateString(scope, "ResolveFunc('Done');", "test.js", 1, null);
    assertTrue(done.get());
  }

  @Test
  public void testThenCallbackReject() {
    scope.put("RejectFund", scope, null);
    scope.put("p", scope, null);
    cx.evaluateString(
        scope,
        """
                p = new Promise((resolve, reject) => { RejectFunc = reject; });
                """,
        "test.js",
        1,
        null);
    var p = scope.get("p", scope);
    var pa = PromiseWrapper.wrap(cx, scope, p);
    AtomicBoolean resolved = new AtomicBoolean();
    AtomicBoolean rejected = new AtomicBoolean();
    pa.then(cx, scope, (lcx, ls, val) -> resolved.set(true), (lcx, ls, val) -> rejected.set(true));
    cx.evaluateString(scope, "RejectFunc('Done');", "test.js", 1, null);
    assertTrue(rejected.get());
    assertFalse(resolved.get());
  }
}

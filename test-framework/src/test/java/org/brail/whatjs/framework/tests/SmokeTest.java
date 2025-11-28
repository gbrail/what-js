package org.brail.whatjs.framework.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.brail.whatjs.framework.WPTTestLauncher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

public class SmokeTest {
  private static Context cx;
  private static WPTTestLauncher launcher;

  @BeforeAll
  public static void init() throws IOException {
    cx = Context.enter();
    Scriptable scope = cx.initStandardObjects();
    launcher = WPTTestLauncher.newLauncher(cx, scope);
  }

  @Test
  public void basicTest() {
    launcher.runScript(
        cx,
        """
      test(() => {
        assert_true(true);
      }, "Working test function");
      """);
  }

  @Test
  public void failingTest() {
    assertThrows(
        JavaScriptException.class,
        () -> {
          launcher.runScript(
              cx,
              """
                    test(() => {
                      assert_true(false);
                    }, "Failing test function");
                    """);
        });
  }

  @Test
  public void basicPromiseTest() {
    launcher.runScript(
        cx,
        """
      promise_test(() => {
        return new Promise((resolve) => {
          resolve();
        });
      }, "Working promise test function");
      """);
  }

  @Test
  public void failingPromiseTest() {
    launcher.runScript(
        cx,
        """
      promise_test(() => {
        return new Promise((resolve, reject) => {
          reject('promise not kept');
        });
      }, "Working promise test function");
      """);
  }
}

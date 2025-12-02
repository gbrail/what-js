package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.brail.jwhat.console.Console;
import org.brail.jwhat.framework.WPTTestLauncher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ConsoleTests {
  private static Context cx;
  private static WPTTestLauncher launcher;

  @BeforeAll
  public static void init() throws IOException {
    cx = Context.enter();
    launcher = WPTTestLauncher.newLauncher();
    launcher.setSetupCallback(
        (Context cx, Scriptable scope) -> Console.builder().install(cx, scope));
  }

  @AfterAll
  public static void cleanup() {
    Context.exit();
  }

  public static Object[] getTestScripts() {
    var t = WPTTestLauncher.findTests("console", new String[] {"large-array"});
    System.out.println("Found " + t.length + " scripts");
    return t;
  }

  @ParameterizedTest
  @MethodSource("getTestScripts")
  public void wptTest(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }
}

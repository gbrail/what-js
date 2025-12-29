package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.brail.jwhat.events.Events;
import org.brail.jwhat.framework.WPTTestLauncher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;

public class AbortTests {
  private static Context cx;
  private static WPTTestLauncher launcher;

  @BeforeAll
  public static void init() throws IOException {
    cx = Context.enter();
    launcher = WPTTestLauncher.newLauncher();
    launcher.setSetupCallback(Events::init);
  }

  @AfterAll
  public static void cleanup() {
    Context.exit();
  }

  public static Object[] getTestScripts() {
    return WPTTestLauncher.findTests("dom/abort");
  }

  @ParameterizedTest
  @MethodSource("getTestScripts")
  public void abortTest(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }
}

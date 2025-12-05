package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.brail.jwhat.framework.WPTTestLauncher;
import org.brail.jwhat.url.URL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;

public class URLTests {
  private static Context cx;
  private static WPTTestLauncher launcher;

  @BeforeAll
  public static void init() throws IOException {
    cx = Context.enter();
    launcher = WPTTestLauncher.newLauncher();
    launcher.setSetupCallback(URL::init);
    launcher.addResource("subset-tests-by-key.js");
  }

  @AfterAll
  public static void cleanup() {
    Context.exit();
  }

  public static Object[] getTestScripts() {
    return WPTTestLauncher.findTests("url");
  }

  @ParameterizedTest
  @MethodSource("getTestScripts")
  public void wptTest(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }
}

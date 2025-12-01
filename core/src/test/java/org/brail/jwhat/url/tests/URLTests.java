package org.brail.jwhat.url.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.brail.jwhat.framework.WPTTestLauncher;
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
  }

  @AfterAll
  public static void cleanup() {
    Context.exit();
  }

  public static Object[] getTestScripts() {
    return new Object[] {
      "url/historical.any.js",
      "url/url-constructor.any.js",
      "url/url-origin.any.js",
      "url/url-searchparams.any.js",
      "url/url-setters.any.js",
      "url/url-setters-stripping.any.js",
      "url/url-statics-canparse.any.js",
      "url/url-statics-parse.any.js",
      "url/url-tojson.any.js",
    };
  }

  @ParameterizedTest
  @MethodSource("getTestScripts")
  public void wptTest(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }
}

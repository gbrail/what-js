package org.brail.jwhat.core.tests;

import org.brail.jwhat.framework.WPTTestLauncher;
import org.brail.jwhat.url.URL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StreamTests {
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

  public static Object[] readableStreamTests() {
    return WPTTestLauncher.findTests("streams/readable-streams");
  }

  @ParameterizedTest
  @MethodSource("readableStreamTests")
  public void readableStreams(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }

  public static Object[] readableByteStreamTests() {
    return WPTTestLauncher.findTests("streams/readable-byte-streams");
  }

  @ParameterizedTest
  @MethodSource("readableByteStreamTests")
  public void readableByteStreams(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }
}

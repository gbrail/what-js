package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.brail.jwhat.framework.WPTTestLauncher;
import org.brail.jwhat.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;

public class StreamTests {
  private static Context cx;
  private static WPTTestLauncher launcher;

  @BeforeAll
  public static void init() throws IOException {
    cx = Context.enter();
    launcher = WPTTestLauncher.newLauncher();
    launcher.setSetupCallback(Stream::init);
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

  public static Object[] writableStreamTests() {
    return WPTTestLauncher.findTests("streams/writable-streams");
  }

  @ParameterizedTest
  @MethodSource("writableStreamTests")
  public void writableStreams(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }

  public static Object[] transformStreamTests() {
    return WPTTestLauncher.findTests("streams/transform-streams");
  }

  @ParameterizedTest
  @MethodSource("transformStreamTests")
  public void transformStreams(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }

  public static Object[] pipingTests() {
    return WPTTestLauncher.findTests("streams/piping");
  }

  @ParameterizedTest
  @MethodSource("pipingTests")
  public void piping(String fileName) throws IOException {
    var results = launcher.runFile(cx, fileName);
    assertTrue(results.success(), results.getFailureReason());
  }
}

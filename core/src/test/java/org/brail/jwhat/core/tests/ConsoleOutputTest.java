package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.brail.jwhat.console.Console;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class ConsoleOutputTest {
  private static Context cx;
  private static Scriptable scope;
  private static String lastLine;

  @BeforeAll
  public static void init() {
    cx = Context.enter();
    scope = cx.initStandardObjects();
    Console.builder()
        .printer(
            (level, kind, message, args) -> {
              lastLine = message;
              // Non optimal but don't really care
              for (Object a : args) {
                lastLine += " " + a;
              }
            })
        .install(cx, scope);
  }

  @AfterAll
  public static void cleanup() {
    Context.exit();
  }

  @BeforeEach
  public void setup() {
    lastLine = "";
  }

  @Test
  public void testLogging() {
    run("console.log('foo');");
    assertEquals("foo", lastLine);
    run("console.log('%d + %d = %d', 2, 2, 4)");
    assertEquals("2 + 2 = 4", lastLine);
  }

  @Test
  public void testCount() {
    run("console.count('c');");
    assertEquals("c: 1", lastLine);
    run("console.count('c');");
    assertEquals("c: 2", lastLine);
    lastLine = "";
    run("console.countReset('c');");
    assertEquals("", lastLine);
    run("console.count('c');");
    assertEquals("c: 1", lastLine);
    run("console.countReset('c');");
    run("console.countReset('c');");
    assertEquals("c: not found", lastLine);
  }

  @Test
  public void testTime() {
    run("console.timeLog('t');");
    assertEquals("t: not found", lastLine);
    lastLine = "";
    run("console.time('t');");
    assertEquals("", lastLine);
    run("console.timeLog('t');");
    assertTrue(lastLine.startsWith("t: "));
    run("console.timeLog('t');");
    assertTrue(lastLine.startsWith("t: "));
    run("console.timeEnd('t');");
    assertTrue(lastLine.startsWith("t: "));
    run("console.timeEnd('t');");
    assertEquals("t: not found", lastLine);
  }

  @Test
  public void testAssert() {
    lastLine = "";
    run("console.assert(true);");
    assertEquals("", lastLine);
    run("console.assert(2+2==4);");
    assertEquals("", lastLine);
    run("console.assert(false);");
    assertEquals("assertion failed", lastLine);
    run("console.assert(2+2==5);");
    assertEquals("assertion failed", lastLine);
    // Test various processing of more lines
    run("console.assert(false, 'foo');");
    assertEquals("assertion failed: foo", lastLine);
    run("console.assert(false, 'foo %d', 2);");
    assertEquals("assertion failed: foo 2", lastLine);
    run("console.assert(false, 1);");
    assertEquals("assertion failed 1", lastLine);
  }

  private void run(String line) {
    cx.evaluateString(scope, line, "test.js", 1, null);
  }
}

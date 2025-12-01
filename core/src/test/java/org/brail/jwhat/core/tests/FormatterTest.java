package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.brail.jwhat.console.Formatter;
import org.junit.jupiter.api.Test;

public class FormatterTest {
  @Test
  public void testFormatting() {
    // no args
    var r = Formatter.format(new Object[] {"foo"});
    assertEquals("foo", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    // no formatting
    r = Formatter.format(new Object[] {"foo", 1, 2, 3});
    assertEquals("foo", r.getFormatted());
    assertEquals(3, r.getRemaining().length);
    assertEquals(1, r.getRemaining()[0]);
    assertEquals(2, r.getRemaining()[1]);
    assertEquals(3, r.getRemaining()[2]);
    // Just args
    r = Formatter.format(new Object[] {"%d", 1});
    assertEquals("1", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    // Combinations of start and end
    r = Formatter.format(new Object[] {"%dfoo", 1});
    assertEquals("1foo", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    r = Formatter.format(new Object[] {"foo%d", 1});
    assertEquals("foo1", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    r = Formatter.format(new Object[] {"foo%dbar", 1});
    assertEquals("foo1bar", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    // Multiple args
    r = Formatter.format(new Object[] {"foo%dbar%d", 1, 2});
    assertEquals("foo1bar2", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    r = Formatter.format(new Object[] {"foo%dbar%dbaz", 1, 2});
    assertEquals("foo1bar2baz", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    r = Formatter.format(new Object[] {"%d%d", 1, 2});
    assertEquals("12", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    // Arg mismatch
    r = Formatter.format(new Object[] {"foo%dbar%dbaz%d", 1, 2});
    assertEquals("foo1bar2baz%d", r.getFormatted());
    assertEquals(0, r.getRemaining().length);
    r = Formatter.format(new Object[] {"foo%dbar%d", 1, 2, 3, 4});
    assertEquals("foo1bar2", r.getFormatted());
    assertEquals(2, r.getRemaining().length);
    assertEquals(3, r.getRemaining()[0]);
    assertEquals(4, r.getRemaining()[1]);
  }

  @Test
  public void testSpecifiers() {
    assertEquals("1", doFormat("%d", 1));
    assertEquals("1", doFormat("%i", 1));
    assertEquals("123456", doFormat("%d", 123456));
    assertEquals("-12", doFormat("%d", -12));
    assertEquals("3", doFormat("%d", 3.14));
    assertEquals("NaN", doFormat("%d", Double.NaN));
    assertEquals("Infinity", doFormat("%d", Double.POSITIVE_INFINITY));
    assertEquals("1", doFormat("%f", 1));
    assertEquals("123456", doFormat("%f", 123456));
    assertEquals("-12", doFormat("%f", -12));
    assertEquals("3.14", doFormat("%f", 3.14));
    assertEquals("NaN", doFormat("%f", Double.NaN));
    assertEquals("Infinity", doFormat("%f", Double.POSITIVE_INFINITY));
  }

  private String doFormat(String f, Object a) {
    var r = Formatter.format(new Object[] {f, a});
    assertEquals(0, r.getRemaining().length);
    return r.getFormatted();
  }
}

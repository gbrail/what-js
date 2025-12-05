package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.brail.jwhat.core.impl.URLUtils;
import org.junit.jupiter.api.Test;

public class EncodingTests {
  @Test
  public void testURLEncode() {
    assertEquals(
        "%23",
        URLUtils.percentEncode(new String(new char[] {0x23}), URLUtils::isFormPEncode, false));
    assertEquals(
        "%7F",
        URLUtils.percentEncode(new String(new char[] {0x7f}), URLUtils::isControlPEncode, false));
    assertEquals("%20", URLUtils.percentEncode(" ", URLUtils::isUserinfoPEncode, false));
    assertEquals("%E2%89%A1", URLUtils.percentEncode("≡", URLUtils::isUserinfoPEncode, false));
    assertEquals(
        "Say%20what%E2%80%BD",
        URLUtils.percentEncode("Say what‽", URLUtils::isUserinfoPEncode, false));
  }

  @Test
  public void testURLDecode() {
    assertEquals(" ", URLUtils.percentDecode("%20"));
    assertEquals("≡", URLUtils.percentDecode("%E2%89%A1"));
    assertEquals("Say what‽", URLUtils.percentDecode("Say%20what%E2%80%BD"));
  }
}

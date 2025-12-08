package org.brail.jwhat.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.brail.jwhat.core.impl.AddressUtils;
import org.junit.jupiter.api.Test;

public class AddressTests {
  private static final Object[][] VALID_IPV4 = {
    {"0.0.0.0", "0.0.0.0"},
    {"255.255.255.255", "255.255.255.255"},
    {"127.0.0.1", "127.0.0.1"},
    {"192.168.1.100", "192.168.1.100"},
    {"172.16.255.1", "172.16.255.1"},
    {"10.5.10.5", "10.5.10.5"},
    // Octal (Leading Zeroes)
    {"01.02.03.04", "1.2.3.4"},
    {"192.168.010.01", "192.168.8.1"},
    // Hexadecimal (Standard Java/C notation for octet parsing)
    {"0x8.0xA.0xb.0xC", "8.10.11.12"},
    {"0xAB.0xCD.0xEF.0x10", "171.205.239.16"},
  };

  @Test
  public void testValidIPv4() throws UnknownHostException {
    for (Object[] o : VALID_IPV4) {
      String a = (String) o[0];
      var r = AddressUtils.parseIPv4Address((String) a);
      if (r.isFailure()) {
        System.out.println(a + ": " + r.error());
      }
      assertTrue(r.isSuccess(), a + " should succeed");
      var ia = InetAddress.getByName((String) o[1]);
      assertEquals(ia, r.get());
    }
  }

  private static final String[] INVALID_IPV4 = {
    // Value out of range
    "256.0.0.1",
    "192.168.1.256",
    "0x100.0.0.0",
    // Structure issues
    "1.2.3",
    "1.2.3.4.5",
    "1.1.1..1",
    "1.1.1.1.",
    ".1.1.1.1",
    // Invalid characters/formatting
    "192.168.1.A",
    "-1.0.0.0",
    "192 168 1 1"
  };

  @Test
  public void testInvalidIPv4() {
    for (String a : INVALID_IPV4) {
      assertTrue(AddressUtils.parseIPv4Address(a).isFailure(), a + " should fail");
    }
  }

  private static final String[] VALID_IPV6 = {
    // Full
    "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
    // Compressed
    "2001:db8:85a3::8a2e:370:7334",
    "fe80::1",
    // Loopback/Unspecified
    "::1",
    "::",
    // Maximum hex digits
    "abcd:ef01:2345:6789:abcd:ef01:2345:6789",
    // IPv4 Mapped
    "::ffff:192.168.1.1"
  };

  private static final String[] INVALID_IPV6 = {
    // Multiple '::' compression
    "2001::db8::1",
    // Group too long (> 4 hex digits)
    "2001:db8:85a3:00000:8a2e:370:7334",
    // Invalid hex character
    "2001:db8:85a3:G::1",
    // Too many groups (> 8)
    "2001:db8:85a3:0:0:8a2e:0370:7334:5",
    // Trailing colon
    "2001:db8::1:",
    // Empty group between colons where '::' isn't intended
    "2001:db8:::1"
  };
}

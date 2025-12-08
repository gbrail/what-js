package org.brail.jwhat.core.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AddressUtils {
  public static Result<Integer> parseIPv4Number(String s) {
    if (s.isEmpty()) {
      return Result.failure("empty");
    }
    try {
      if (s.length() >= 2 && s.charAt(0) == '0' && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
        String is = s.substring(2);
        if (is.isEmpty()) {
          return Result.success(0);
        }
        return Result.success(Integer.parseInt(is, 16));
      }
      if (s.charAt(0) == '0') {
        String is = s.substring(1);
        if (is.isEmpty()) {
          return Result.success(0);
        }
        return Result.success(Integer.parseInt(is, 8));
      }
      return Result.success(Integer.parseInt(s, 10));
    } catch (NumberFormatException e) {
      return Result.failure("number-format");
    }
  }

  public static Result<InetAddress> parseIPv4Address(String s) {
    String[] parts = URLUtils.DOT.split(s, -1);
    if (parts.length == 0) {
      return Result.failure("IPV4-empty");
    }
    if (parts[parts.length - 1].isEmpty()) {
      return Result.failure("IPv4-empty-part");
    }
    if (parts.length > 4) {
      return Result.failure("IPv4-too-many-parts");
    }
    byte[] addr = new byte[parts.length];
    for (int i = 0; i < parts.length; i++) {
      var num = parseIPv4Number(parts[i]);
      if (num.isFailure()) {
        return Result.failure("IPv4-non-numeric-part");
      }
      int n = num.get();
      if (n < 0 || n > 255) {
        return Result.failure("IPv4-non-numeric-part");
      }
      addr[i] = (byte) n;
    }
    try {
      return Result.success(InetAddress.getByAddress(addr));
    } catch (UnknownHostException e) {
      return Result.failure("IPv4-out-of-range");
    }
  }

  public static Result<InetAddress> parseIPv6Address(String s) {
    return Result.failure("not-implemented");
  }
}

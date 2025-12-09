package org.brail.jwhat.core.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class AddressUtils {
  private static final char EOF = Character.MAX_VALUE;
  static final Pattern DOT = Pattern.compile("\\.");

  public static Result<String> decodeIPv4Address(String s) {
    var r = parseIPv4Address(s);
    if (r.isSuccess()) {
      return Result.success(r.get().toString());
    }
    return Result.continueError(r);
  }

  public static Result<String> decodeIPv6Address(String s) {
    var r = parseIPv6Address(s);
    if (r.isSuccess()) {
      return Result.success(r.get().toString());
    }
    return Result.continueError(r);
  }

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
    String[] parts = DOT.split(s, -1);
    if (parts.length == 0) {
      return Result.failure("IPV4-empty");
    }
    if (parts[parts.length - 1].isEmpty()) {
      return Result.failure("IPv4-empty-part");
    }
    if (parts.length > 4) {
      return Result.failure("IPv4-too-many-parts");
    }
    byte[] addr = new byte[4];
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

  public static Result<InetAddress> parseIPv6Address(String input) {
    int[] addr = new int[8];
    int pieceIndex = 0;
    int compress = -1;

    if (input.isEmpty()) {
      return Result.failure("IPv6-empty");
    }
    int p = 0;
    if (input.charAt(0) == ':') {
      if ((input.length() < 2) || (input.charAt(1) != ':')) {
        return Result.failure("IPv6-invalid-compression");
      }
      p += 2;
      pieceIndex++;
      compress = pieceIndex;
    }

    char c = p < input.length() ? 0 : EOF;
    while (c != EOF) {
      if (pieceIndex == 8) {
        return Result.failure("IPv6-too-many-pieces");
      }
      c = getChar(input, p);
      if (c == ':') {
        if (compress >= 0) {
          return Result.failure("IPv6-multiple-compression");
        }
        p++;
        pieceIndex++;
        compress = pieceIndex;
        continue;
      }
      int value = 0;
      int length = 0;
      while (p < input.length() && length < 4 && URLUtils.isHexDigit(c)) {
        value = (value << 4) + Integer.parseInt(String.valueOf(c), 16);
        p++;
        length++;
        c = getChar(input, p);
      }
      if (c == '.') {
        if (length == 0) {
          return Result.failure("IPv4-in-IPv6-invalid-code-point");
        }
        if (pieceIndex > 6) {
          return Result.failure("IPv4-in-IPv6-too-many-pieces");
        }
        p -= length;
        return parseIPv4IPv6Address(input, p, addr, pieceIndex);
      }
      if (c == ':') {
        p++;
        if (p == input.length()) {
          return Result.failure("IPv6-invalid-code-point");
        }
      } else if (c != EOF) {
        return Result.failure("IPv6-invalid-code-point");
      }
      addr[pieceIndex++] = value;
    }
    if (compress >= 0) {
      int swaps = pieceIndex - compress;
      pieceIndex = 7;
      while (pieceIndex > 0 && swaps > 0) {
        int t = addr[pieceIndex];
        addr[pieceIndex] = addr[compress + swaps - 1];
        addr[compress + swaps - 1] = t;
        pieceIndex--;
        swaps--;
      }
    }
    if (compress == 0 && pieceIndex != 8) {
      return Result.failure("IPv6-too-few-pieces");
    }
    return makeIPv6Address(addr);
  }

  private static Result<InetAddress> parseIPv4IPv6Address(
      String input, int p, int[] addr, int pieceIndex) {
    int numbersSeen = 0;
    char c = p < input.length() ? 0 : EOF;
    while (c != EOF) {
      c = getChar(input, p);
      int ipV4Piece = -1;
      if (numbersSeen > 0) {
        if (c == '.' && numbersSeen < 4) {
          p++;
          c = getChar(input, p);
        } else {
          return Result.failure("IPv4-in-IPv6-invalid-code-point");
        }
      }
      if (!URLUtils.isDigit(c)) {
        return Result.failure("IPv4-in-IPv6-invalid-code-point");
      }
      while (URLUtils.isDigit(c)) {
        int num = Integer.parseInt(String.valueOf(c), 10);
        if (ipV4Piece < 0) {
          ipV4Piece = num;
        } else if (ipV4Piece == 0) {
          return Result.failure("IPv4-in-IPv6-invalid-code-point");
        } else {
          ipV4Piece = (ipV4Piece * 10) + num;
        }
        if (ipV4Piece > 255) {
          return Result.failure("IPv4-in-IPv6-out-of-range-part");
        }
        p++;
        c = getChar(input, p);
      }
      addr[pieceIndex] = (addr[pieceIndex] << 8) + ipV4Piece;
      numbersSeen++;
      if (numbersSeen == 2 || numbersSeen == 4) {
        pieceIndex++;
      }
    }
    if (numbersSeen != 4) {
      return Result.failure("IPv4-in-IPv6-too-few-parts");
    }
    return makeIPv6Address(addr);
  }

  public static Result<InetAddress> makeIPv6Address(int[] ia) {
    assert ia.length == 8;
    byte[] a = new byte[16];
    for (int i = 0; i < 8; i++) {
      int v = ia[i];
      assert v <= 65536;
      // Big-endian
      a[i * 2] = (byte) (v >>> 8);
      a[(i * 2) + 1] = (byte) (v & 0xff);
    }
    try {
      return Result.success(InetAddress.getByAddress(a));
    } catch (UnknownHostException e) {
      return Result.failure("IPv6-out-of-range");
    }
  }

  public static boolean endsInNumber(CharSequence s) {
    String[] parts = DOT.split(s);
    if (parts.length > 0) {
      String last = parts[parts.length - 1];
      if (last.isEmpty()) {
        if (parts.length == 1) {
          return false;
        }
        last = parts[parts.length - 2];
        if (URLUtils.isOnlyDigits(last)) {
          return true;
        }
        if (AddressUtils.parseIPv4Number(last).isSuccess()) {
          return true;
        }
      }
    }
    return false;
  }

  private static char getChar(String s, int p) {
    if (p < s.length()) {
      return s.charAt(p);
    }
    return EOF;
  }
}

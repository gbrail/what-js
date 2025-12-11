package org.brail.jwhat.core.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.brail.jwhat.url.URLFormatException;

public class AddressUtils {
  private static final char EOF = Character.MAX_VALUE;
  static final Pattern DOT = Pattern.compile("\\.");

  public static String decodeIPv4Address(CharSequence s) throws URLFormatException {
    var a = parseIPv4Address(s);
    return a.toString();
  }

  public static String decodeIPv6Address(CharSequence s) throws URLFormatException {
    var a = parseIPv6Address(s);
    return a.toString();
  }

  public static int parseIPv4Number(CharSequence s) throws URLFormatException {
    if (s.isEmpty()) {
      throw new URLFormatException("empty");
    }
    try {
      if (s.length() >= 2 && s.charAt(0) == '0' && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
        var is = s.subSequence(2, s.length());
        if (is.isEmpty()) {
          return 0;
        }
        return Integer.parseInt(is.toString(), 16);
      }
      if (s.charAt(0) == '0') {
        var is = s.subSequence(1, s.length());
        if (is.isEmpty()) {
          return 0;
        }
        return Integer.parseInt(is.toString(), 8);
      }
      return Integer.parseInt(s.toString(), 10);
    } catch (NumberFormatException e) {
      throw new URLFormatException("number-format");
    }
  }

  public static InetAddress parseIPv4Address(CharSequence s) throws URLFormatException {
    String[] parts = DOT.split(s, -1);
    if (parts.length == 0) {
      throw new URLFormatException("IPV4-empty");
    }
    if (parts[parts.length - 1].isEmpty()) {
      throw new URLFormatException("IPv4-empty-part");
    }
    if (parts.length > 4) {
      throw new URLFormatException("IPv4-too-many-parts");
    }
    byte[] addr = new byte[4];
    for (int i = 0; i < parts.length; i++) {
      var n = parseIPv4Number(parts[i]);
      if (n < 0 || n > 255) {
        throw new URLFormatException("IPv4-non-numeric-part");
      }
      addr[i] = (byte) n;
    }
    try {
      return InetAddress.getByAddress(addr);
    } catch (UnknownHostException e) {
      throw new URLFormatException("IPv4-out-of-range", e);
    }
  }

  public static InetAddress parseIPv6Address(CharSequence input) throws URLFormatException {
    int[] addr = new int[8];
    int pieceIndex = 0;
    int compress = -1;

    if (input.isEmpty()) {
      throw new URLFormatException("IPv6-empty");
    }
    int p = 0;
    if (input.charAt(0) == ':') {
      if ((input.length() < 2) || (input.charAt(1) != ':')) {
        throw new URLFormatException("IPv6-invalid-compression");
      }
      p += 2;
      pieceIndex++;
      compress = pieceIndex;
    }

    char c = p < input.length() ? 0 : EOF;
    while (c != EOF) {
      if (pieceIndex == 8) {
        throw new URLFormatException("IPv6-too-many-pieces");
      }
      c = getChar(input, p);
      if (c == ':') {
        if (compress >= 0) {
          throw new URLFormatException("IPv6-multiple-compression");
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
          throw new URLFormatException("IPv4-in-IPv6-invalid-code-point");
        }
        if (pieceIndex > 6) {
          throw new URLFormatException("IPv4-in-IPv6-too-many-pieces");
        }
        p -= length;
        return parseIPv4IPv6Address(input, p, addr, pieceIndex);
      }
      if (c == ':') {
        p++;
        if (p == input.length()) {
          throw new URLFormatException("IPv6-invalid-code-point");
        }
      } else if (c != EOF) {
        throw new URLFormatException("IPv6-invalid-code-point");
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
      throw new URLFormatException("IPv6-too-few-pieces");
    }
    return makeIPv6Address(addr);
  }

  private static InetAddress parseIPv4IPv6Address(
      CharSequence input, int p, int[] addr, int pieceIndex) throws URLFormatException {
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
          throw new URLFormatException("IPv4-in-IPv6-invalid-code-point");
        }
      }
      if (!URLUtils.isDigit(c)) {
        throw new URLFormatException("IPv4-in-IPv6-invalid-code-point");
      }
      while (URLUtils.isDigit(c)) {
        int num = Integer.parseInt(String.valueOf(c), 10);
        if (ipV4Piece < 0) {
          ipV4Piece = num;
        } else if (ipV4Piece == 0) {
          throw new URLFormatException("IPv4-in-IPv6-invalid-code-point");
        } else {
          ipV4Piece = (ipV4Piece * 10) + num;
        }
        if (ipV4Piece > 255) {
          throw new URLFormatException("IPv4-in-IPv6-out-of-range-part");
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
      throw new URLFormatException("IPv4-in-IPv6-too-few-parts");
    }
    return makeIPv6Address(addr);
  }

  public static InetAddress makeIPv6Address(int[] ia) throws URLFormatException {
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
      return InetAddress.getByAddress(a);
    } catch (UnknownHostException e) {
      throw new URLFormatException("IPv6-out-of-range", e);
    }
  }

  public static boolean endsInNumber(CharSequence s) {
    String[] parts = DOT.split(s, -1);
    if (parts.length > 0) {
      var last = parts[parts.length - 1];
      if (last.isEmpty()) {
        if (parts.length == 1) {
          return false;
        }
        last = parts[parts.length - 2];
        if (URLUtils.isOnlyDigits(last)) {
          return true;
        }
        try {
          AddressUtils.parseIPv4Number(last);
          return true;
        } catch (URLFormatException e) {
          return false;
        }
      }
    }
    return false;
  }

  private static char getChar(CharSequence s, int p) {
    if (p < s.length()) {
      return s.charAt(p);
    }
    return EOF;
  }
}

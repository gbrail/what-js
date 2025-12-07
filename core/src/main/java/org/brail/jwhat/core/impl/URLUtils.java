package org.brail.jwhat.core.impl;

import java.nio.charset.StandardCharsets;

public class URLUtils {
  private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

  public interface Classifier {
    boolean shouldEncode(byte b);
  }

  public static String percentEncode(CharSequence s, Classifier k, boolean spaceAsPlus) {
    StringBuilder sb = new StringBuilder();
    byte[] d = s.toString().getBytes(StandardCharsets.UTF_8);
    for (byte b : d) {
      if (b == ' ' && spaceAsPlus) {
        sb.append('+');
      } else if (k.shouldEncode(b)) {
        appendEncoded(b, sb);
      } else {
        sb.append((char) b);
      }
    }
    return sb.toString();
  }

  public static String percentDecode(CharSequence s) {
    // We can guarantee that the decoded byte array will have fewer bytes than characters
    byte[] b = new byte[s.length()];
    int p = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '%' && i < s.length() - 2) {
        var is = new String(new char[] {s.charAt(i + 1), s.charAt(i + 2)});
        try {
          var num = Integer.parseInt(is, 16);
          assert num <= 0xff;
          b[p++] = (byte) num;
          i += 2;
        } catch (NumberFormatException nfe) {
          // TODO what to do on error?
        }
      } else {
        // TODO what do to on error?
        if (c <= 0x7f) {
          b[p++] = (byte) c;
        }
      }
    }
    return new String(b, 0, p, StandardCharsets.UTF_8);
  }

  public static void percentEncode(char c, Classifier k, StringBuilder sb) {
    if (c <= 0x7F) {
      byte b = (byte) c;
      if (k.shouldEncode(b)) {
        appendEncoded(b, sb);
      } else {
        sb.append(c);
      }
    } else if (c <= 0x7FF) {
      appendEncoded(0xC0 | (c >> 6), sb);
      appendEncoded(0x80 | (c & 0x3F), sb);
    } else {
      appendEncoded(0xE0 | (c >> 12), sb);
      appendEncoded(0x80 | ((c >> 6) & 0x3F), sb);
      appendEncoded(0x80 | (c & 0x3F), sb);
    }
  }

  private static void appendEncoded(byte b, StringBuilder sb) {
    appendEncoded(b & 0xff, sb);
  }

  private static void appendEncoded(int ub, StringBuilder sb) {
    assert ub <= Character.MAX_VALUE;
    sb.append('%');
    sb.append(HEX_DIGITS[ub >>> 4]);
    sb.append(HEX_DIGITS[ub & 0x0f]);
  }

  // ASCII stuff

  public static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  public static boolean isUpperHexDigit(char c) {
    return isDigit(c) || (c >= 'A' && c <= 'F');
  }

  public static boolean isLowerHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f');
  }

  public static boolean isHexDigit(char c) {
    return isUpperHexDigit(c) || isLowerHexDigit(c);
  }

  public static boolean isUpperAlpha(char c) {
    return c >= 'A' && c <= 'Z';
  }

  public static boolean isLowerAlpha(char c) {
    return c >= 'a' && c <= 'z';
  }

  public static boolean isAlpha(char c) {
    return isUpperAlpha(c) || isLowerAlpha(c);
  }

  public static boolean isAlphanumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  public static boolean isControl(char c) {
    return c <= 0x1f;
  }

  public static boolean isTabOrNewline(char c) {
    return switch (c) {
      case '\t', '\r', '\n' -> true;
      default -> false;
    };
  }

  // Byte sets for URL encoding and decoding

  public static boolean isControlPEncode(byte b) {
    return b <= 0x1f || b >= 0x7f;
  }

  public static boolean isFragmentPEncode(byte b) {
    if (isControlPEncode(b)) {
      return true;
    }
    return switch (b) {
      case ' ', '"', '<', '>', '`' -> true;
      default -> false;
    };
  }

  public static boolean isQueryPEncode(byte b) {
    if (isControlPEncode(b)) {
      return true;
    }
    return switch (b) {
      case ' ', '"', '#', '<', '>' -> true;
      default -> false;
    };
  }

  public static boolean isSpecialQueryPEncode(byte b) {
    return isQueryPEncode(b) || b == '\'';
  }

  public static boolean isPathPEncode(byte b) {
    if (isQueryPEncode(b)) {
      return true;
    }
    return switch (b) {
      case '?', '^', '`', '{', '}' -> true;
      default -> false;
    };
  }

  public static boolean isUserinfoPEncode(byte b) {
    if (isPathPEncode(b)) {
      return true;
    }
    if (b >= '[' && b <= ']') {
      return true;
    }
    return switch (b) {
      case '/', ':', ';', '=', '@', '|' -> true;
      default -> false;
    };
  }

  public static boolean isComponentPEncode(byte b) {
    if (isUserinfoPEncode(b)) {
      return true;
    }
    if (b >= '$' && b <= '&') {
      return true;
    }
    return switch (b) {
      case '+', ',' -> true;
      default -> false;
    };
  }

  public static boolean isFormPEncode(byte b) {
    if (isComponentPEncode(b)) {
      return true;
    }
    if (b >= '\'' && b <= ')') {
      return true;
    }
    return switch (b) {
      case '!', '~' -> true;
      default -> false;
    };
  }

  public static boolean isSpecialScheme(CharSequence s) {
    return switch (s.toString()) {
      case "ftp", "file", "http", "https", "ws", "wss" -> true;
      default -> false;
    };
  }

  public static boolean isDefaultPort(String p, String scheme) {
    try {
      int port = Integer.parseInt(p);
      return isDefaultPort(port, scheme);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static boolean isDefaultPort(int p, String scheme) {
    return switch (scheme) {
      case "ftp" -> p == 21;
      case "http", "ws" -> p == 80;
      case "https", "wss" -> p == 443;
      default -> false;
    };
  }

  public static boolean isWindowsDriveLetter(CharSequence s) {
    if (s.length() == 2) {
      return isAlpha(s.charAt(0)) && (s.charAt(1) == ':' || s.charAt(1) == '|');
    }
    return false;
  }

  public static boolean isNormalizedWindowsDriveLetter(CharSequence s) {
    if (s.length() == 2) {
      return isAlpha(s.charAt(0)) && s.charAt(1) == ':';
    }
    return false;
  }

  public static boolean schemeHasOrigin(String scheme) {
    if (scheme == null) {
      return false;
    }
    return switch (scheme) {
      case "ftp", "http", "https", "ws", "wss" -> true;
      default -> false;
    };
  }

  public static boolean isURLCodePoint(char c) {
    // Does not account for UTF-8, but we go one char at a time
    if (isAlphanumeric(c)) {
      return true;
    }
    return switch (c) {
      case '!',
              '$',
              '&',
              '\'',
              '(',
              ')',
              '*',
              '+',
              ',',
              '-',
              '.',
              '/',
              ':',
              ';',
              '=',
              '?',
              '@',
              '_',
              '~' ->
          true;
      default -> false;
    };
  }
}

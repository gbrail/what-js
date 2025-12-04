package org.brail.jwhat.url;

import java.util.Optional;
import java.util.regex.Pattern;

class URLParser {
  private static final Pattern TAB_NEWLINE = Pattern.compile("\\t\\r\\n");
  private static final Pattern COLON = Pattern.compile(":");

  private String scheme;
  private String path;
  private String base;
  private String query;
  private String fragment;
  private String username;
  private String password;
  private String host;
  private String port;

  private String input;
  private StringBuilder buf;
  private int p;
  private boolean atSignSeen;
  private boolean insideBrackets;
  private String failure;

  private enum ParseState {
    SCHEME_START,
    SCHEME,
    NO_SCHEME,
    FILE,
    SPECIAL_REL_OR_AUTHORITY,
    SPECIAL_AUTHORITY_SLASHES,
    SPECIAL_AUTHORITY_IGNORE_SLASHES,
    PATH_OR_AUTHORITY,
    OPAQUE_PATH,
    RELATIVE,
    FRAGMENT,
    AUTHORITY,
    PATH,
    RELATIVE_SLASH,
    QUERY,
    HOST,
    HOSTNAME,
    PATH_START,
    PORT,
    FILE_SLASH,
    FILE_HOST,
    FAILURE
  };

  /** Implement the "basic URL parser" from WhatWG Section 4. */
  void parse(String in, URLParser base) {
    if (in == null || in.isEmpty()) {
      return;
    }
    input = in.trim();
    input = TAB_NEWLINE.matcher(input).replaceAll("");
    buf = new StringBuilder();
    ParseState state = ParseState.SCHEME_START;

    for (int p = 0; p < input.length(); p++) {
      char c = input.charAt(p);
      switch (state) {
        case SCHEME_START:
          if (isAlpha(c)) {
            buf.append(Character.toLowerCase(c));
            state = ParseState.SCHEME;
          } else {
            state = ParseState.NO_SCHEME;
            p--;
          }
          break;
        case SCHEME:
          state = schemeState(c, base);
          break;
        case NO_SCHEME:
          state = noSchemeState(c, base);
          break;
        case SPECIAL_REL_OR_AUTHORITY:
          if (c == '/' && remainingChar(input, p) == '/') {
            state = ParseState.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            p--;
          } else {
            state = ParseState.RELATIVE;
            p--;
          }
          break;
        case PATH_OR_AUTHORITY:
          if (c == '/') {
            state = ParseState.AUTHORITY;
          } else {
            state = ParseState.PATH;
            p--;
          }
          break;
        case RELATIVE:
          state = relativeState(c, base);
          break;
        case RELATIVE_SLASH:
          state = relativeSlashState(c, base);
          break;
        case SPECIAL_AUTHORITY_SLASHES:
          state = specialAuthoritySlashesState(c);
          break;
        case SPECIAL_AUTHORITY_IGNORE_SLASHES:
          state = specialAuthorityIgnoreSlashesState(c);
          break;
        case AUTHORITY:
          state = authorityState(c);
          break;
        case HOST:
        case HOSTNAME:
          state = hostState(c, state);
          break;
        case PORT:
          state = portState(c);
          break;
        case FILE:
          state = fileState(c, base);
          break;
        case FILE_SLASH:
          state = fileSlashState(c, base);
          break;
        case FILE_HOST:
          state = fileHostState(c);
          break;
        default:
          assert false;
          // TODO TODO TODO Pick up from "file state"
      }
    }
  }

  private ParseState noSchemeState(char c, URLParser base) {
    if (base == null) {
      // TODO "if base has an opaque path and c != '#'
      failure = "Missing scheme, non-relative URL";
      return ParseState.FAILURE;
    }
    if (!"file".equals(base.scheme)) {
      // TODO "if base has an opaque path and c == '#'
      p--;
      return ParseState.RELATIVE;
    }
    p--;
    return ParseState.FILE;
  }

  private ParseState schemeState(char c, URLParser base) {
    if (isAlpha(c) || c == '+' || c == '-' || c == '.') {
      buf.append(Character.toLowerCase(c));
      return ParseState.SCHEME;
    }

    if (c == ':') {
      scheme = buf.toString();
      buf = new StringBuilder();
      if ("file".equals(scheme)) {
        return ParseState.FILE;
      }
      if (isSpecialScheme(scheme)) {
        if (base != null && scheme.equals(base.scheme)) {
          return ParseState.SPECIAL_REL_OR_AUTHORITY;
        } else {
          return ParseState.SPECIAL_AUTHORITY_SLASHES;
        }
      }

      if (remainingChar(input, p) == '/') {
        p++;
        return ParseState.PATH_OR_AUTHORITY;
      } else {
        path = "";
        return ParseState.OPAQUE_PATH;
      }
    }

    buf = new StringBuilder();
    p = 0;
    return ParseState.NO_SCHEME;
  }

  private ParseState relativeState(char c, URLParser base) {
    assert !"file".equals(scheme);
    assert base != null;
    scheme = base.scheme;
    if (c == '/') {
      return ParseState.RELATIVE_SLASH;
    } else if (isSpecialScheme(scheme) && c == '/') {
      return ParseState.RELATIVE_SLASH;
    } else {
      username = base.username;
      password = base.password;
      host = base.host;
      port = base.port;
      path = base.path;
      query = base.query;
      if (c == '?') {
        query = "";
        return ParseState.QUERY;
      } else if (c == '#') {
        fragment = "";
        return ParseState.FRAGMENT;
      } else {
        query = null;
        // "Shorten Url's path"
        p--;
        return ParseState.PATH;
      }
    }
  }

  private ParseState relativeSlashState(char c, URLParser base) {
    if (isSpecialScheme(scheme) && (c == '/' || c == '\\')) {
      return ParseState.SPECIAL_AUTHORITY_SLASHES;
    }
    if (c == '/') {
      return ParseState.AUTHORITY;
    }
    username = base.username;
    password = base.password;
    host = base.host;
    port = base.port;
    p--;
    return ParseState.PATH;
  }

  private ParseState specialAuthoritySlashesState(char c) {
    if (c == '/' && remainingChar(input, p) == '/') {
      p++;
      return ParseState.SPECIAL_AUTHORITY_IGNORE_SLASHES;
    } else {
      p--;
      return ParseState.SPECIAL_AUTHORITY_IGNORE_SLASHES;
    }
  }

  private ParseState specialAuthorityIgnoreSlashesState(char c) {
    if (c != '/' && c != '\\') {
      p--;
      return ParseState.AUTHORITY;
    }
    // One of many non-terminating validation errors here
    return ParseState.SPECIAL_AUTHORITY_SLASHES;
  }

  private ParseState authorityState(char c) {
    if (c == '@') {
      if (atSignSeen) {
        buf.insert(0, "%40");
      }
      atSignSeen = true;
      decodePassword(buf);
      buf = new StringBuilder();
    } else if ((c == '/' || c == '?' || c == '#') || (isSpecialScheme(scheme) && c == '/')) {
      if (atSignSeen && buf.isEmpty()) {
        failure = "host missing";
        return ParseState.FAILURE;
      } else {
        p -= (buf.length() + 1);
        buf = new StringBuilder();
        return ParseState.HOST;
      }
    } else {
      buf.append(c);
    }
    return ParseState.AUTHORITY;
  }

  private ParseState hostState(char c, ParseState initState) {
    if (c == ':' && !insideBrackets) {
      if (buf.isEmpty()) {
        failure = "host missing";
        return ParseState.FAILURE;
      }
      var h = decodeHost(buf, !isSpecialScheme(scheme));
      if (h.isEmpty()) {
        failure = "invalid host";
        return ParseState.FAILURE;
      } else {
        host = h.get();
        buf = new StringBuilder();
        return ParseState.PORT;
      }
      // TODO "EOF code point"
    }

    if ((c == '/' || c == '?' || c == '#') || (isSpecialScheme(scheme) && c == '/')) {
      p--;
      if (isSpecialScheme(scheme) && buf.isEmpty()) {
        failure = "host missing";
        return ParseState.FAILURE;
      } else {
        var h = decodeHost(buf, !isSpecialScheme(scheme));
        if (h.isEmpty()) {
          failure = "invalid host";
          return ParseState.FAILURE;
        } else {
          host = h.get();
          buf = new StringBuilder();
          return ParseState.PATH_START;
        }
      }
    }
    if (c == '[') {
      insideBrackets = true;
    } else if (c == ']') {
      insideBrackets = false;
    }
    return initState;
  }

  private ParseState portState(char c) {
    if (isDigit(c)) {
      buf.append(c);
      return ParseState.PORT;
    }

    if ((c == '/' || c == '?' || c == '#') || (isSpecialScheme(scheme) && c == '/')) {
      var dp = decodePort(buf);
      if (dp.isEmpty()) {
        failure = "invalid port";
        return ParseState.FAILURE;
      } else {
        if (isDefaultPort(dp.get())) {
          port = null;
        } else {
          port = String.valueOf(dp.get());
        }
        buf = new StringBuilder();
        p--;
        return ParseState.PATH;
      }
    }

    failure = "invalid port";
    return ParseState.FAILURE;
  }

  private ParseState fileState(char c, URLParser base) {
    scheme = "file";
    host = "";
    if (c == '/' || c == '\\') {
      // validation error
      return ParseState.FILE_SLASH;
    }
    if (base != null && "file".equals(base.scheme)) {
      host = base.host;
      path = base.path;
      query = base.query;
      if (c == '?') {
        query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        fragment = "";
        return ParseState.FRAGMENT;
      }
      if (!isEof()) {
        query = null;
        // TODO Check optional Windows thing
        p--;
        return ParseState.PATH;
      }
    }
    p--;
    return ParseState.PATH;
  }

  private ParseState fileSlashState(char c, URLParser base) {
    if (c == '/' || c == '\\') {
      // optional validation error
      return ParseState.FILE_HOST;
    }
    if (base != null && "file".equals(base.scheme)) {
      host = base.host;
      // TODO windows check
    }
    p--;
    return ParseState.PATH;
  }

  private ParseState fileHostState(char c) {
    if (isEof() || c == '/' || c == '\\' || c == '?' || c == '#') {
      p--;
      // Check windows thing
      if (buf.isEmpty()) {
        host = "";
        return ParseState.PATH_START;
      }
      var r = decodeHost(buf, !isSpecialScheme(scheme));
      if (r.isEmpty()) {
        failure = "invalid host";
        return ParseState.FAILURE;
      }
      if ("localhost".equals(r.get())) {
        host = "";
      } else {
        host = r.get();
      }
      buf = new StringBuilder();
      return ParseState.PATH_START;
    }
    buf.append(c);
    return ParseState.FILE_HOST;
  }

  private boolean isEof() {
    return (p == input.length() - 1);
  }

  private static char remainingChar(String input, int p) {
    if (p < input.length() - 1) {
      return input.charAt(p + 1);
    }
    return 0;
  }

  private void decodePassword(CharSequence b) {
    String[] p = COLON.split(b, 2);
    // TODO doesn't do percent-encoding as per "authority state" section
    username = p[0];
    password = p.length > 1 ? p[1] : null;
  }

  private Optional<String> decodeHost(CharSequence s, boolean isOpaque) {
    // TODO host parsing in various ways
    return Optional.of("HOST_NAME_HERE");
  }

  private Optional<Integer> decodePort(CharSequence s) {
    try {
      int port = Integer.parseInt(s.toString());
      if (port >= 0 && port <= 65536) {
        return Optional.of(port);
      }
    } catch (NumberFormatException e) {
      // Fall through
    }
    return Optional.empty();
  }

  private boolean isDefaultPort(int p) {
    return switch (scheme) {
      case "ftp" -> p == 21;
      case "http", "ws" -> p == 80;
      case "https", "wss" -> p == 443;
      default -> false;
    };
  }

  // ASCII stuff

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isUpperHexDigit(char c) {
    return isDigit(c) || (c >= 'A' && c <= 'F');
  }

  private static boolean isLowerHexDigit(char c) {
    return isDigit(c) || (c >= 'a' && c <= 'f');
  }

  private static boolean isHexDigit(char c) {
    return isUpperHexDigit(c) || isLowerHexDigit(c);
  }

  private static boolean isUpperAlpha(char c) {
    return c >= 'A' && c <= 'Z';
  }

  private static boolean isLowerAlpha(char c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isAlpha(char c) {
    return isUpperAlpha(c) || isLowerAlpha(c);
  }

  private static boolean isAlphanumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private static boolean isSpecialScheme(String s) {
    return switch (s) {
      case "ftp", "file", "http", "https", "ws", "wss" -> true;
      default -> false;
    };
  }
}

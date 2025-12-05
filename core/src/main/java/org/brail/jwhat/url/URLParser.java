package org.brail.jwhat.url;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

class URLParser {
  private static final Pattern TAB_NEWLINE = Pattern.compile("\\t\\r\\n");
  private static final Pattern COLON = Pattern.compile(":");
  private static final char EOF = Character.MAX_VALUE;

  String scheme;
  String path;
  String query;
  String fragment;
  String username;
  String password;
  String host;
  String port;

  private CharSequence input;
  private StringBuilder buf;
  private int p;
  private boolean atSignSeen;
  private boolean insideBrackets;
  private List<String> errors;
  private boolean special;
  private boolean opaque;
  private boolean failed;

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
  URLParser(CharSequence in, URLParser base) {
    assert base == null || !base.isFailure();
    if (in == null || in.isEmpty()) {
      return;
    }
    input = in.toString().trim();
    input = TAB_NEWLINE.matcher(input).replaceAll("");
    resetBuffer();
    ParseState state = ParseState.SCHEME_START;

    for (int p = 0; p <= input.length(); p++) {
      // Iterate one character at a time, but with an extra round-trip
      // with a special EOF "character" to support special processing.
      char c;
      if (p < input.length()) {
        c = input.charAt(p);
      } else {
        c = EOF;
      }

      switch (state) {
        case SCHEME_START:
          state = schemeStartState(c);
          break;
        case SCHEME:
          state = schemeState(c, base);
          break;
        case NO_SCHEME:
          state = noSchemeState(c, base);
          break;
        case SPECIAL_REL_OR_AUTHORITY:
          state = specialRelativeOrAuthorityState(c);
          break;
        case PATH_OR_AUTHORITY:
          state = pathOrAuthorityState(c);
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
        case PATH_START:
          state = pathStartState(c);
          break;
        case PATH:
          state = pathState(c);
          break;
        case OPAQUE_PATH:
          state = opaquePathState(c);
          break;
        case QUERY:
          state = queryState(c);
          break;
        case FRAGMENT:
          state = fragmentState(c);
          break;
        case FAILURE:
          failed = true;
          return;
        default:
          assert false;
      }
    }
  }

  /** Return if parsing failed and this URL can't be used. */
  boolean isFailure() {
    return failed;
  }

  /**
   * Return the errors encountered during parsing. This may return errors if "isFailure" is false,
   * which means that the errors are non-fatal.
   */
  Optional<String> getErrors() {
    if (errors == null || errors.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(String.join("; ", errors));
  }

  private ParseState schemeStartState(char c) {
    if (isAlpha(c)) {
      buf.append(Character.toLowerCase(c));
      return ParseState.SCHEME;
    }

    p--;
    return ParseState.NO_SCHEME;
  }

  private ParseState schemeState(char c, URLParser base) {
    if (isAlpha(c) || c == '+' || c == '-' || c == '.') {
      buf.append(Character.toLowerCase(c));
      return ParseState.SCHEME;
    }

    if (c == ':') {
      scheme = buf.toString();
      resetBuffer();
      special = isSpecialScheme(scheme);
      if ("file".equals(scheme)) {
        if (nextChar() != '/' || nextNextChar() != '/') {
          addError("special-scheme-missing-following-solidus");
        }
        return ParseState.FILE;
      }
      if (special) {
        if (base != null && scheme.equals(base.scheme)) {
          assert base.special;
          return ParseState.SPECIAL_REL_OR_AUTHORITY;
        } else {
          return ParseState.SPECIAL_AUTHORITY_SLASHES;
        }
      }
      if (nextChar() == '/') {
        p++;
        return ParseState.PATH_OR_AUTHORITY;
      } else {
        path = "";
        return ParseState.OPAQUE_PATH;
      }
    }

    // Otherwise, tart over with no scheme...
    resetBuffer();
    p = -1;
    return ParseState.NO_SCHEME;
  }

  private ParseState noSchemeState(char c, URLParser base) {
    if (base == null || (base.opaque && c != '#')) {
      addError("missing-scheme-non-relative-URL");
      return ParseState.FAILURE;
    }
    if (!"file".equals(base.scheme)) {
      p--;
      return ParseState.RELATIVE;
    }
    p--;
    return ParseState.FILE;
  }

  private ParseState specialRelativeOrAuthorityState(char c) {
    if (c == '/' && nextChar() == '/') {
      p++;
      return ParseState.SPECIAL_AUTHORITY_IGNORE_SLASHES;
    }
    addError("special-scheme-missing-following-solidus");
    p--;
    return ParseState.RELATIVE;
  }

  private ParseState pathOrAuthorityState(char c) {
    if (c == '/') {
      return ParseState.AUTHORITY;
    }
    p--;
    return ParseState.PATH;
  }

  private ParseState relativeState(char c, URLParser base) {
    assert !"file".equals(scheme);
    assert base != null;
    scheme = base.scheme;
    if (c == '/') {
      return ParseState.RELATIVE_SLASH;
    } else if (special && c == '\\') {
      addError("invalid-reverse-solidus");
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
      }
      if (c == '#') {
        fragment = "";
        return ParseState.FRAGMENT;
      }
      query = null;
      // TODO: Shorten path, requires some parsing
      p--;
      return ParseState.PATH;
    }
  }

  private ParseState relativeSlashState(char c, URLParser base) {
    if (special && (c == '/' || c == '\\')) {
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
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
    if (c == '/' && nextChar() == '/') {
      p++;
    } else {
      addError("special-scheme-missing-following-solidus");
      p--;
    }
    return ParseState.SPECIAL_AUTHORITY_IGNORE_SLASHES;
  }

  private ParseState specialAuthorityIgnoreSlashesState(char c) {
    if (c != '/' && c != '\\') {
      p--;
      return ParseState.AUTHORITY;
    }
    addError("special-scheme-missing-following-solidus");
    return ParseState.SPECIAL_AUTHORITY_IGNORE_SLASHES;
  }

  private ParseState authorityState(char c) {
    if (c == '@') {
      addError("invalid-credentials");
      if (atSignSeen) {
        buf.insert(0, "%40");
      }
      atSignSeen = true;
      decodePassword(buf);
      resetBuffer();
    } else if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\')) {
      if (atSignSeen && buf.isEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      p -= (buf.length() + 1);
      resetBuffer();
      return ParseState.HOST;
    } else {
      buf.append(c);
    }
    return ParseState.AUTHORITY;
  }

  private ParseState hostState(char c, ParseState initState) {
    if (c == ':' && !insideBrackets) {
      if (buf.isEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      var h = decodeHost(buf, !special);
      if (h.isEmpty()) {
        addError("invalid-host");
        return ParseState.FAILURE;
      }
      host = h.get();
      buf = new StringBuilder();
      return ParseState.PORT;
    }

    if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\')) {
      p--;
      if (special && buf.isEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      var h = decodeHost(buf, !isSpecialScheme(scheme));
      if (h.isEmpty()) {
        addError("invalid-host");
        return ParseState.FAILURE;
      }
      host = h.get();
      buf = new StringBuilder();
      return ParseState.PATH_START;
    }

    if (c == '[') {
      insideBrackets = true;
    } else if (c == ']') {
      insideBrackets = false;
    }
    buf.append(c);
    return initState;
  }

  private ParseState portState(char c) {
    if (isDigit(c)) {
      buf.append(c);
      return ParseState.PORT;
    }

    if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\')) {
      if (!buf.isEmpty()) {
        var dp = decodePort(buf);
        if (dp.isEmpty()) {
          addError("port-out-of-range");
          return ParseState.FAILURE;
        }
        if (isDefaultPort(dp.get())) {
          port = null;
        } else {
          port = String.valueOf(dp.get());
        }
        resetBuffer();
      }
      p--;
      return ParseState.PATH;
    }

    addError("port-invalid");
    return ParseState.FAILURE;
  }

  private ParseState fileState(char c, URLParser base) {
    scheme = "file";
    host = "";
    if (c == '/' || c == '\\') {
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
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
      if (c != EOF) {
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
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
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
    if (c == EOF || c == '/' || c == '\\' || c == '?' || c == '#') {
      p--;
      // Check windows thing
      if (buf.isEmpty()) {
        host = "";
        return ParseState.PATH_START;
      }
      var r = decodeHost(buf, !special);
      if (r.isEmpty()) {
        addError("host-invalid");
        return ParseState.FAILURE;
      }
      if ("localhost".equals(r.get())) {
        host = "";
      } else {
        host = r.get();
      }
      resetBuffer();
      return ParseState.PATH_START;
    }
    buf.append(c);
    return ParseState.FILE_HOST;
  }

  private ParseState pathStartState(char c) {
    if (special) {
      if (c != '/' && c != '\\') {
        if (c == '\\') {
          addError("invalid-reverse-solidus");
        }
        p--;
      }
      return ParseState.PATH;
    }
    if (c == '?') {
      query = "";
      return ParseState.QUERY;
    }
    if (c == '#') {
      fragment = "";
      return ParseState.FRAGMENT;
    }
    if (c != EOF && c != '/') {
      p--;
    }
    return ParseState.PATH_START;
  }

  private ParseState pathState(char c) {
    if (c == EOF || c == '/' || c == '?' || c == '#' || (special && c == '\\')) {
      if (special && c == '\\') {
        addError("invalid-reverse-solidus");
      }
      if ("..".contentEquals(buf)) {
        // TODO shorten path
        path = buf.toString();
      } else if (".".contentEquals(buf) && c != '/' && !(special && c == '\\')) {
        path = "";
      } else {
        if (path == null) {
          path = buf.toString();
        } else {
          path = path + buf.toString();
        }
      }
      // TODO windows drive letter
      resetBuffer();
      if (c == '?') {
        query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        fragment = "";
        return ParseState.FRAGMENT;
      }
    }

    if (!isURLCodePoint(c) && c != '%') {
      addError("invalid-url-unit");
    }
    if (c == '%' && (!isHexDigit(nextChar()) || !isHexDigit(nextNextChar()))) {
      addError("invalid-url-unit");
    }
    // TODO percent-encoding
    buf.append(c);
    return ParseState.PATH;
  }

  private ParseState opaquePathState(char c) {
    opaque = true;
    if (c == '?') {
      query = "";
      return ParseState.QUERY;
    }
    if (c == '#') {
      fragment = "";
      return ParseState.FRAGMENT;
    }
    if (c == ' ') {
      if (nextChar() == '?' || nextChar() == '#') {
        path += "%20";
      } else {
        path += ' ';
      }
    } else if (c != EOF) {
      if (!isURLCodePoint(c) && c != '%') {
        addError("invalid-url-unit");
      }
      if (c == '%' && (!isHexDigit(nextChar()) || !isHexDigit(nextNextChar()))) {
        addError("invalid-url-unit");
      }
      // TODO percent-encoding
      path += c;
    }
    return ParseState.OPAQUE_PATH;
  }

  private ParseState queryState(char c) {
    if (c == '#' || c == EOF) {
      // TODO percent-encode the query
      query = buf.toString();
      resetBuffer();
      if (c == '#') {
        fragment = "";
        return ParseState.FRAGMENT;
      }
    }
    if (c != EOF) {
      if (!isURLCodePoint(c) && c != '%') {
        addError("invalid-url-unit");
      }
      if (c == '%' && (!isHexDigit(nextChar()) || !isHexDigit(nextNextChar()))) {
        addError("invalid-url-unit");
      }
      // TODO percent-encoding
      buf.append(c);
    }
    return ParseState.QUERY;
  }

  private ParseState fragmentState(char c) {
    if (!isURLCodePoint(c) && c != '%') {
      addError("invalid-url-unit");
    }
    if (c == '%' && (!isHexDigit(nextChar()) || !isHexDigit(nextNextChar()))) {
      addError("invalid-url-unit");
    }
    // TODO percent-encoding
    fragment += c;
    return ParseState.FRAGMENT;
  }

  private char nextChar() {
    if (p >= 0 && p < input.length() - 1) {
      return input.charAt(p + 1);
    }
    return EOF;
  }

  private char nextNextChar() {
    if (p >= 0 && p < input.length() - 2) {
      return input.charAt(p + 2);
    }
    return EOF;
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

  private void resetBuffer() {
    buf = new StringBuilder();
  }

  private void addError(String msg) {
    if (errors == null) {
      errors = new ArrayList<>();
    }
    errors.add(msg);
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

  private static boolean isControl(char c) {
    return c <= 0x1f;
  }

  private static boolean isSpecialScheme(String s) {
    return switch (s) {
      case "ftp", "file", "http", "https", "ws", "wss" -> true;
      default -> false;
    };
  }

  boolean schemeHasOrigin() {
    if (scheme == null) {
      return false;
    }
    return switch (scheme) {
      case "ftp", "http", "https", "ws", "wss" -> true;
      default -> false;
    };
  }

  private static boolean isURLCodePoint(char c) {
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

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: URLParser <url> [base]");
      System.exit(2);
    }

    String url = args[0];
    String base = args.length > 1 ? args[1] : null;

    URLParser bp = null;
    if (base != null) {
      bp = new URLParser(base, null);
      var e = bp.getErrors();
      if (bp.isFailure()) {
        assert e.isPresent();
        System.err.println("FAILURE: " + e.get());
        System.exit(1);
      } else e.ifPresent(s -> System.err.println("WARNING: " + s));
    }

    var p = new URLParser(url, bp);
    var e = p.getErrors();
    if (p.isFailure()) {
      assert e.isPresent();
      System.err.println("FAILURE: " + e.get());
      System.exit(1);
    } else e.ifPresent(s -> System.err.println("WARNING: " + s));
  }
}

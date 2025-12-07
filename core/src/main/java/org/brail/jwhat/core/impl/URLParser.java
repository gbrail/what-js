package org.brail.jwhat.core.impl;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class URLParser {
  private static final Pattern TAB_NEWLINE = Pattern.compile("\\t\\r\\n");
  private static final Pattern COLON = Pattern.compile(":");
  private static final char EOF = Character.MAX_VALUE;

  public String scheme = "";
  public List<String> path = new ArrayList<>();
  public StringBuilder opaquePath;
  public String query;
  public StringBuilder fragment;
  public String username = "";
  public String password = "";
  public String host;
  public String port;

  private CharSequence input;
  private StringBuilder buf;
  private int p;
  private int bufferStart;
  private boolean atSignSeen;
  private boolean insideBrackets;
  private List<String> errors;
  private boolean special;
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
  public URLParser(CharSequence in, URLParser base) {
    assert base == null || !base.isFailure();
    if (in == null || (in.isEmpty() && base == null)) {
      return;
    }
    // TODO do this without toString()
    input = in.toString().trim();
    ParseState state = ParseState.SCHEME_START;

    for (p = 0; p <= input.length(); p++) {
      // Iterate one character at a time, but with an extra round-trip
      // with a special EOF "character" to support special processing.
      char c;
      if (p < input.length()) {
        c = input.charAt(p);
      } else {
        c = EOF;
      }
      if (URLUtils.isTabOrNewline(c)) {
        continue;
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
  public boolean isFailure() {
    return failed;
  }

  /**
   * Return the errors encountered during parsing. This may return errors if "isFailure" is false,
   * which means that the errors are non-fatal.
   */
  public Optional<String> getErrors() {
    if (errors == null || errors.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(String.join("; ", errors));
  }

  private ParseState schemeStartState(char c) {
    if (URLUtils.isAlpha(c)) {
      addBuffer(Character.toLowerCase(c));
      return ParseState.SCHEME;
    }

    p--;
    return ParseState.NO_SCHEME;
  }

  private ParseState schemeState(char c, URLParser base) {
    if (URLUtils.isAlpha(c) || c == '+' || c == '-' || c == '.') {
      addBuffer(Character.toLowerCase(c));
      return ParseState.SCHEME;
    }

    if (c == ':') {
      scheme = consumeBuffer();
      special = URLUtils.isSpecialScheme(scheme);
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
        path = new ArrayList<>();
        return ParseState.OPAQUE_PATH;
      }
    }

    // Otherwise, start over with no scheme...
    resetBuffer();
    p = -1;
    return ParseState.NO_SCHEME;
  }

  private ParseState noSchemeState(char c, URLParser base) {
    if (base == null || (base.opaquePath != null && c != '#')) {
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
      path = new ArrayList<>(base.path);
      query = base.query;
      if (c == '?') {
        query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        fragment = new StringBuilder();
        return ParseState.FRAGMENT;
      }
      if (c != EOF) {
        query = null;
        shortenPath();
        p--;
      }
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
        prependBuffer("%40");
      }
      atSignSeen = true;
      decodePassword(consumeBuffer());
    } else if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\')) {
      if (atSignSeen && bufferEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      // Rewind to start of buffer, including characters we skipped
      p -= rewindBuffer() + 1;
      return ParseState.HOST;
    } else {
      addBuffer(c);
    }
    return ParseState.AUTHORITY;
  }

  private ParseState hostState(char c, ParseState initState) {
    if (c == ':' && !insideBrackets) {
      if (bufferEmpty()) {
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
      if (special && bufferEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      var h = decodeHost(consumeBuffer(), !URLUtils.isSpecialScheme(scheme));
      if (h.isEmpty()) {
        addError("invalid-host");
        return ParseState.FAILURE;
      }
      host = h.get();
      return ParseState.PATH_START;
    }

    if (c == '[') {
      insideBrackets = true;
    } else if (c == ']') {
      insideBrackets = false;
    }
    addBuffer(c);
    return initState;
  }

  private ParseState portState(char c) {
    if (URLUtils.isDigit(c)) {
      addBuffer(c);
      return ParseState.PORT;
    }

    if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\')) {
      if (!bufferEmpty()) {
        var dp = decodePort(consumeBuffer());
        if (dp.isEmpty()) {
          addError("port-out-of-range");
          return ParseState.FAILURE;
        }
        if (isDefaultPort(dp.get())) {
          port = null;
        } else {
          port = String.valueOf(dp.get());
        }
      }
      p--;
      return ParseState.PATH_START;
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
      path = new ArrayList<>(base.path);
      query = base.query;
      if (c == '?') {
        query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        fragment = new StringBuilder();
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
      // TODO weird Windows path thing
    }
    p--;
    return ParseState.PATH;
  }

  private ParseState fileHostState(char c) {
    if (c == EOF || c == '/' || c == '\\' || c == '?' || c == '#') {
      p--;
      // Check windows thing
      if (bufferEmpty()) {
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
    addBuffer(c);
    return ParseState.FILE_HOST;
  }

  private ParseState pathStartState(char c) {
    if (special) {
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
      if (c != '/' && c != '\\') {
        p--;
      }
      return ParseState.PATH;
    }
    if (c == '?') {
      query = "";
      return ParseState.QUERY;
    }
    if (c == '#') {
      fragment = new StringBuilder();
      return ParseState.FRAGMENT;
    }
    if (c != EOF) {
      if (c != '/') {
        p--;
      }
      return ParseState.PATH;
    }
    return ParseState.PATH_START;
  }

  private ParseState pathState(char c) {
    if (c == EOF || c == '/' || c == '?' || c == '#' || (special && c == '\\')) {
      if (special && c == '\\') {
        addError("invalid-reverse-solidus");
      }
      if (bufferEquals("..")) {
        shortenPath();
        if (c != '/' && !(special && c == '\\')) {
          path.add("");
        }
      } else if (bufferEquals(".") && c != '/' && !(special && c == '\\')) {
        path.add("");
      } else if (!bufferEquals(".")) {
        String pb = consumeBuffer();
        if ("file".equals(scheme) && path.isEmpty() && URLUtils.isWindowsDriveLetter(pb)) {
          assert pb.length() == 2;
          pb = new String(new char[] {pb.charAt(0), ':'});
        }
        path.add(pb);
      }
      resetBuffer();
      if (c == '?') {
        query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        fragment = new StringBuilder();
        return ParseState.FRAGMENT;
      }
      return ParseState.PATH;
    }

    if (!URLUtils.isURLCodePoint(c) && c != '%') {
      addError("invalid-url-unit");
    }
    if (c == '%' && (!URLUtils.isHexDigit(nextChar()) || !URLUtils.isHexDigit(nextNextChar()))) {
      addError("invalid-url-unit");
    }
    addBufferEncoded(c, URLUtils::isPathPEncode);
    return ParseState.PATH;
  }

  private ParseState opaquePathState(char c) {
    if (c == '?') {
      query = "";
      return ParseState.QUERY;
    }
    if (c == '#') {
      fragment = new StringBuilder();
      return ParseState.FRAGMENT;
    }
    if (opaquePath == null) {
      opaquePath = new StringBuilder();
    }
    if (c == ' ') {
      if (nextChar() == '?' || nextChar() == '#') {
        opaquePath.append("%20");
      } else {
        opaquePath.append(' ');
      }
    } else if (c != EOF) {
      if (!URLUtils.isURLCodePoint(c) && c != '%') {
        addError("invalid-url-unit");
      }
      if (c == '%' && (!URLUtils.isHexDigit(nextChar()) || !URLUtils.isHexDigit(nextNextChar()))) {
        addError("invalid-url-unit");
      }
      URLUtils.percentEncode(c, URLUtils::isControlPEncode, opaquePath);
    }
    return ParseState.OPAQUE_PATH;
  }

  private ParseState queryState(char c) {
    if (c == '#' || c == EOF) {
      String q = consumeBuffer();
      if (special) {
        query = URLUtils.percentEncode(q, URLUtils::isSpecialQueryPEncode, false);
      } else {
        query = URLUtils.percentEncode(q, URLUtils::isQueryPEncode, false);
      }
      if (c == '#') {
        fragment = new StringBuilder();
        return ParseState.FRAGMENT;
      }
    }
    if (c != EOF) {
      if (!URLUtils.isURLCodePoint(c) && c != '%') {
        addError("invalid-url-unit");
      }
      if (c == '%' && (URLUtils.isHexDigit(nextChar()) || !URLUtils.isHexDigit(nextNextChar()))) {
        addError("invalid-url-unit");
      }
      // Percent-encoding happens later, see the spec about JIS
      addBuffer(c);
    }
    return ParseState.QUERY;
  }

  private ParseState fragmentState(char c) {
    if (!URLUtils.isURLCodePoint(c) && c != '%') {
      addError("invalid-url-unit");
    }
    if (c == '%' && (!URLUtils.isHexDigit(nextChar()) || !URLUtils.isHexDigit(nextNextChar()))) {
      addError("invalid-url-unit");
    }
    // The spec says not to use the buffer, which makes this inefficient,
    // not sure why.
    if (c != EOF) {
      URLUtils.percentEncode(c, URLUtils::isFragmentPEncode, fragment);
    }
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
    password = p.length > 1 ? p[1] : "";
  }

  private Optional<String> decodeHost(CharSequence s, boolean isOpaque) {
    if (s.length() > 1 && s.charAt(0) == '[') {
      if (s.charAt(s.length() - 1) != ']') {
        addError("IPv6-unclosed-validation");
        return Optional.empty();
      }
      return decodeIPv6(s.subSequence(1, s.length() - 1));
    }
    if (isOpaque) {
      return decodeOpaqueHost(s);
    }
    String dec = URLUtils.percentDecode(s);
    // TODO IPv4 address parsing
    return Optional.of(dec);
  }

  private Optional<String> decodeIPv6(CharSequence s) {
    // TODO replace with the real algorithm, since this may do a lookup
    try {
      var as = s.toString();
      InetAddress addr = InetAddress.getByName(as);
      if (addr instanceof Inet6Address && as.contains(":")) {
        return Optional.of(addr.getHostAddress());
      } else {
        addError("IPv6-validation-failed");
        return Optional.empty();
      }
    } catch (UnknownHostException e) {
      addError("IPv6-validation-failed");
      return Optional.empty();
    }
  }

  private Optional<String> decodeOpaqueHost(CharSequence s) {
    // TODO validate character set as shown in spec
    // Perhaps add a "fail" mode to the URL decoder for this purpose
    return Optional.of(URLUtils.percentEncode(s, URLUtils::isControlPEncode, false));
  }

  private static Optional<Integer> decodePort(CharSequence s) {
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

  private boolean bufferEmpty() {
    return buf == null || buf.isEmpty();
  }

  private int bufferLength() {
    return buf == null ? 0 : buf.length();
  }

  private boolean bufferEquals(String s) {
    return (buf != null && s.contentEquals(buf));
  }

  private void resetBuffer() {
    buf = null;
  }

  private String getBuffer() {
    if (buf != null) {
      return buf.toString();
    }
    return "";
  }

  private String consumeBuffer() {
    if (buf != null) {
      String s = buf.toString();
      resetBuffer();
      return s;
    }
    return "";
  }

  private void addBuffer(char c) {
    if (buf == null) {
      bufferStart = p;
      buf = new StringBuilder();
    }
    buf.append(c);
  }

  private void addBufferEncoded(char c, URLUtils.Classifier k) {
    if (buf == null) {
      bufferStart = p;
      buf = new StringBuilder();
    }
    URLUtils.percentEncode(c, k, buf);
  }

  private void prependBuffer(String s) {
    if (buf == null) {
      bufferStart = p;
      buf = new StringBuilder();
    }
    buf.insert(0, s);
  }

  private int rewindBuffer() {
    if (buf != null) {
      resetBuffer();
      return p - bufferStart;
    }
    return 0;
  }

  private void addError(String msg) {
    if (errors == null) {
      errors = new ArrayList<>();
    }
    errors.add(msg);
  }

  private void shortenPath() {
    // assert path is not opaque (by length?)
    if (path.size() == 1
        && "file".equals(scheme)
        && URLUtils.isNormalizedWindowsDriveLetter(path.get(0))) {
      return;
    }
    if (!path.isEmpty()) {
      path.remove(path.size() - 1);
    }
  }

  public boolean schemeHasOrigin() {
    return URLUtils.schemeHasOrigin(scheme);
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
      } else {
        e.ifPresent(s -> System.err.println("WARNING: " + s));
      }
    }

    var p = new URLParser(url, bp);
    var e = p.getErrors();
    if (p.isFailure()) {
      assert e.isPresent();
      System.err.println("FAILURE: " + e.get());
      System.exit(1);
    } else {
      e.ifPresent(s -> System.err.println("WARNING: " + s));
    }
  }
}

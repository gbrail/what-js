package org.brail.jwhat.url;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.brail.jwhat.core.impl.URLUtils;

public class URLParser {
  private static final Pattern COLON = Pattern.compile(":");
  private static final char EOF = Character.MAX_VALUE;

  private final URL t;
  private final URL base;

  private CharSequence input;
  private StringBuilder buf;
  private int p;
  private int bufferStart;
  private boolean atSignSeen;
  private boolean insideBrackets;
  private List<String> errors;
  private boolean special;
  private boolean failed;
  private final boolean overrideState;

  enum ParseState {
    NONE,
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
    FAILURE,
    FINISHED
  };

  /** Implement the "basic URL parser" from WhatWG Section 4. */
  URLParser(CharSequence in, URL target, URL base, ParseState start) {
    this.t = target;
    this.base = base;
    ParseState state = start;
    if (state == ParseState.NONE) {
      state = ParseState.SCHEME_START;
      overrideState = false;
    } else {
      overrideState = true;
    }

    if (in == null || (in.isEmpty() && base == null)) {
      return;
    }
    // TODO do this without toString()
    input = in.toString().trim();

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
          state = schemeState(c);
          break;
        case NO_SCHEME:
          state = noSchemeState(c);
          break;
        case SPECIAL_REL_OR_AUTHORITY:
          state = specialRelativeOrAuthorityState(c);
          break;
        case PATH_OR_AUTHORITY:
          state = pathOrAuthorityState(c);
          break;
        case RELATIVE:
          state = relativeState(c);
          break;
        case RELATIVE_SLASH:
          state = relativeSlashState(c);
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
          state = fileState(c);
          break;
        case FILE_SLASH:
          state = fileSlashState(c);
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
        case FINISHED:
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

  private ParseState schemeState(char c) {
    if (URLUtils.isAlpha(c) || c == '+' || c == '-' || c == '.') {
      addBuffer(Character.toLowerCase(c));
      return ParseState.SCHEME;
    }

    if (c == ':') {
      if (overrideState) {
        if ((URLUtils.isSpecialScheme(t.scheme) && !URLUtils.isSpecialScheme(getBuffer()))
            || (!URLUtils.isSpecialScheme(t.scheme) && URLUtils.isSpecialScheme(getBuffer()))) {
          return ParseState.FINISHED;
        }
        if ((!t.username.isEmpty() || !t.password.isEmpty() || t.port != null)
            && "file".equals(getBuffer())) {
          return ParseState.FINISHED;
        }
        if ("file".equals(t.scheme) && (t.host == null || t.host.isEmpty())) {
          return ParseState.FINISHED;
        }
      }
      t.scheme = consumeBuffer();
      special = URLUtils.isSpecialScheme(t.scheme);
      if (overrideState && special && URLUtils.isDefaultPort(t.port, t.scheme)) {
        t.port = null;
      }

      if ("file".equals(t.scheme)) {
        if (nextChar() != '/' || nextNextChar() != '/') {
          addError("special-scheme-missing-following-solidus");
        }
        return ParseState.FILE;
      }
      if (special) {
        if (base != null && t.scheme.equals(base.scheme)) {
          return ParseState.SPECIAL_REL_OR_AUTHORITY;
        } else {
          return ParseState.SPECIAL_AUTHORITY_SLASHES;
        }
      }
      if (nextChar() == '/') {
        p++;
        return ParseState.PATH_OR_AUTHORITY;
      } else {
        t.path = new ArrayList<>();
        return ParseState.OPAQUE_PATH;
      }
    }

    if (!overrideState) {
      // Otherwise, start over with no scheme...
      resetBuffer();
      p = -1;
      return ParseState.NO_SCHEME;
    }
    return ParseState.FAILURE;
  }

  private ParseState noSchemeState(char c) {
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

  private ParseState relativeState(char c) {
    assert !"file".equals(t.scheme);
    assert base != null;
    t.scheme = base.scheme;
    if (c == '/') {
      return ParseState.RELATIVE_SLASH;
    } else if (special && c == '\\') {
      addError("invalid-reverse-solidus");
      return ParseState.RELATIVE_SLASH;
    } else {
      t.username = base.username;
      t.password = base.password;
      t.host = base.host;
      t.port = base.port;
      t.path = new ArrayList<>(base.path);
      t.query = base.query;
      if (c == '?') {
        t.query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        t.fragment = new StringBuilder();
        return ParseState.FRAGMENT;
      }
      if (c != EOF) {
        t.query = null;
        shortenPath();
        p--;
      }
      return ParseState.PATH;
    }
  }

  private ParseState relativeSlashState(char c) {
    if (special && (c == '/' || c == '\\')) {
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
      return ParseState.SPECIAL_AUTHORITY_SLASHES;
    }
    if (c == '/') {
      return ParseState.AUTHORITY;
    }
    t.username = base.username;
    t.password = base.password;
    t.host = base.host;
    t.port = base.port;
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
    if (overrideState && "file".equals(t.scheme)) {
      p--;
      return ParseState.FILE_HOST;
    }
    if (c == ':' && !insideBrackets) {
      if (bufferEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      if (overrideState && initState == ParseState.HOSTNAME) {
        return ParseState.FAILURE;
      }
      var h = decodeHost(buf, !special);
      if (h.isEmpty()) {
        addError("invalid-host");
        return ParseState.FAILURE;
      }
      t.host = h.get();
      buf = new StringBuilder();
      return ParseState.PORT;
    }

    if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\')) {
      p--;
      if (special && bufferEmpty()) {
        addError("host-missing");
        return ParseState.FAILURE;
      }
      if (overrideState
          && bufferEmpty()
          && ((!t.username.isEmpty() || !t.password.isEmpty() || t.port != null))) {
        return ParseState.FAILURE;
      }
      var h = decodeHost(consumeBuffer(), !URLUtils.isSpecialScheme(t.scheme));
      if (h.isEmpty()) {
        addError("invalid-host");
        return ParseState.FAILURE;
      }
      t.host = h.get();
      if (overrideState) {
        return ParseState.FINISHED;
      }
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

    if ((c == EOF || c == '/' || c == '?' || c == '#') || (special && c == '\\') || overrideState) {
      if (!bufferEmpty()) {
        var dp = decodePort(consumeBuffer());
        if (dp.isEmpty()) {
          addError("port-out-of-range");
          return ParseState.FAILURE;
        }
        if (URLUtils.isDefaultPort(dp.get(), t.scheme)) {
          t.port = null;
        } else {
          t.port = String.valueOf(dp.get());
        }
        if (overrideState) {
          return ParseState.FINISHED;
        }
      }
      if (overrideState) {
        return ParseState.FAILURE;
      }
      p--;
      return ParseState.PATH_START;
    }

    addError("port-invalid");
    return ParseState.FAILURE;
  }

  private ParseState fileState(char c) {
    t.scheme = "file";
    t.host = "";
    if (c == '/' || c == '\\') {
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
      return ParseState.FILE_SLASH;
    }
    if (base != null && "file".equals(base.scheme)) {
      t.host = base.host;
      t.path = new ArrayList<>(base.path);
      t.query = base.query;
      if (c == '?') {
        t.query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        t.fragment = new StringBuilder();
        return ParseState.FRAGMENT;
      }
      if (c != EOF) {
        t.query = null;
        // TODO Check optional Windows thing
        p--;
        return ParseState.PATH;
      }
    }
    p--;
    return ParseState.PATH;
  }

  private ParseState fileSlashState(char c) {
    if (c == '/' || c == '\\') {
      if (c == '\\') {
        addError("invalid-reverse-solidus");
      }
      return ParseState.FILE_HOST;
    }
    if (base != null && "file".equals(base.scheme)) {
      t.host = base.host;
      // TODO weird Windows path thing
    }
    p--;
    return ParseState.PATH;
  }

  private ParseState fileHostState(char c) {
    if (overrideState && URLUtils.isWindowsDriveLetter(getBuffer())) {
      addError("invalid-windows-drive-letter-host");
      return ParseState.PATH;
    }
    if (c == EOF || c == '/' || c == '\\' || c == '?' || c == '#') {
      p--;
      // Check windows thing
      if (bufferEmpty()) {
        t.host = "";
        if (overrideState) {
          return ParseState.FINISHED;
        }
        return ParseState.PATH_START;
      }
      var r = decodeHost(buf, !special);
      if (r.isEmpty()) {
        addError("host-invalid");
        return ParseState.FAILURE;
      }
      if ("localhost".equals(r.get())) {
        t.host = "";
      } else {
        t.host = r.get();
      }
      if (overrideState) {
        return ParseState.FINISHED;
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
    if (!overrideState && c == '?') {
      t.query = "";
      return ParseState.QUERY;
    }
    if (!overrideState && c == '#') {
      t.fragment = new StringBuilder();
      return ParseState.FRAGMENT;
    }
    if (c != EOF) {
      if (c != '/') {
        p--;
      }
      return ParseState.PATH;
    } else if (overrideState && t.host == null) {
      t.path.add("");
    }
    return ParseState.PATH_START;
  }

  private ParseState pathState(char c) {
    if (c == EOF
        || c == '/'
        || (!overrideState && (c == '?' || c == '#'))
        || (special && c == '\\')) {
      if (special && c == '\\') {
        addError("invalid-reverse-solidus");
      }
      if (bufferEquals("..")) {
        shortenPath();
        if (c != '/' && !(special && c == '\\')) {
          t.path.add("");
        }
      } else if (bufferEquals(".") && c != '/' && !(special && c == '\\')) {
        t.path.add("");
      } else if (!bufferEquals(".")) {
        String pb = consumeBuffer();
        if ("file".equals(t.scheme) && t.path.isEmpty() && URLUtils.isWindowsDriveLetter(pb)) {
          assert pb.length() == 2;
          pb = new String(new char[] {pb.charAt(0), ':'});
        }
        t.path.add(pb);
      }
      resetBuffer();
      if (c == '?') {
        t.query = "";
        return ParseState.QUERY;
      }
      if (c == '#') {
        t.fragment = new StringBuilder();
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
      t.query = "";
      return ParseState.QUERY;
    }
    if (c == '#') {
      t.fragment = new StringBuilder();
      return ParseState.FRAGMENT;
    }
    if (t.opaquePath == null) {
      t.opaquePath = new StringBuilder();
    }
    if (c == ' ') {
      if (nextChar() == '?' || nextChar() == '#') {
        t.opaquePath.append("%20");
      } else {
        t.opaquePath.append(' ');
      }
    } else if (c != EOF) {
      if (!URLUtils.isURLCodePoint(c) && c != '%') {
        addError("invalid-url-unit");
      }
      if (c == '%' && (!URLUtils.isHexDigit(nextChar()) || !URLUtils.isHexDigit(nextNextChar()))) {
        addError("invalid-url-unit");
      }
      URLUtils.percentEncode(c, URLUtils::isControlPEncode, t.opaquePath);
    }
    return ParseState.OPAQUE_PATH;
  }

  private ParseState queryState(char c) {
    if ((!overrideState && c == '#') || c == EOF) {
      String q = consumeBuffer();
      if (special) {
        t.query = URLUtils.percentEncode(q, URLUtils::isSpecialQueryPEncode, false);
      } else {
        t.query = URLUtils.percentEncode(q, URLUtils::isQueryPEncode, false);
      }
      if (c == '#') {
        t.fragment = new StringBuilder();
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
      if (t.fragment == null) {
        t.fragment = new StringBuilder();
      }
      URLUtils.percentEncode(c, URLUtils::isFragmentPEncode, t.fragment);
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
    t.username = p[0];
    t.password = p.length > 1 ? p[1] : "";
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

  private boolean bufferEmpty() {
    return buf == null || buf.isEmpty();
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
    if (t.path.size() == 1
        && "file".equals(t.scheme)
        && URLUtils.isNormalizedWindowsDriveLetter(t.path.get(0))) {
      return;
    }
    if (!t.path.isEmpty()) {
      t.path.remove(t.path.size() - 1);
    }
  }

  public boolean schemeHasOrigin() {
    return URLUtils.schemeHasOrigin(t.scheme);
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: URLParser <url> [base]");
      System.exit(2);
    }

    String url = args[0];
    String base = args.length > 1 ? args[1] : null;

    URL bu = null;
    if (base != null) {
      bu = URL.parseURL(base, null);
      if (bu.getFailure().isPresent()) {
        System.err.println("FAILURE: " + bu.getFailure().get());
        System.exit(1);
      }
    }

    var u = URL.parseURL(url, bu);
    if (u.getFailure().isPresent()) {
      System.err.println("FAILURE: " + u.getFailure().get());
      System.exit(1);
    }
  }
}

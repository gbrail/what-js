package org.brail.jwhat.url;

import java.util.ArrayList;
import java.util.List;
import org.brail.jwhat.core.impl.URLUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class URL extends ScriptableObject {
  String scheme = "";
  List<String> path = new ArrayList<>();
  StringBuilder opaquePath;
  String query;
  StringBuilder fragment;
  String username = "";
  String password = "";
  String host;
  String port;

  public static void init(Context cx, Scriptable scope) {
    var c = new LambdaConstructor(scope, "URL", 1, URL::constructor);
    c.defineConstructorMethod(scope, "parse", 1, URL::parse);
    c.defineConstructorMethod(scope, "canParse", 1, URL::canParse);
    c.definePrototypeProperty(cx, "hash", URL::getHash, URL::setHash);
    c.definePrototypeProperty(cx, "host", URL::getHost, URL::setHost);
    c.definePrototypeProperty(cx, "hostname", URL::getHostname, URL::setHostname);
    c.definePrototypeProperty(cx, "password", URL::getPassword, URL::setPassword);
    c.definePrototypeProperty(cx, "pathname", URL::getPathname, URL::setPathname);
    c.definePrototypeProperty(cx, "port", URL::getPort, URL::setPort);
    c.definePrototypeProperty(cx, "protocol", URL::getProtocol, URL::setProtocol);
    c.definePrototypeProperty(cx, "username", URL::getUsername, URL::setUsername);
    c.definePrototypeProperty(cx, "search", URL::getSearch, URL::setSearch);
    c.definePrototypeProperty(cx, "href", URL::getHref, URL::setHref);
    c.definePrototypeMethod(scope, "toJSON", 0, URL::toJSON);
    c.definePrototypeProperty(cx, "origin", URL::getOrigin);

    ScriptableObject.defineProperty(scope, "URL", c, ScriptableObject.DONTENUM);
    URLSearchParams.init(cx, scope);
  }

  @Override
  public String getClassName() {
    return "URL";
  }

  private URL() {}

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    String urlStr = urlArgument(args);
    String baseStr = baseArgument(args);
    try {
      return parseImpl(urlStr, baseStr);
    } catch (URLFormatException e) {
      throw ScriptRuntime.typeError(e.getMessage());
    }
  }

  private static Object parse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String urlStr = urlArgument(args);
    String baseStr = baseArgument(args);
    try {
      return parseImpl(urlStr, baseStr);
    } catch (URLFormatException e) {
      return null;
    }
  }

  private static Object canParse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String urlStr = urlArgument(args);
    String baseStr = baseArgument(args);
    if (urlStr == null && baseStr == null) {
      return false;
    }
    try {
      parseImpl(urlStr, baseStr);
      return true;
    } catch (URLFormatException e) {
      return false;
    }
  }

  private static URL parseImpl(String urlStr, String baseStr) throws URLFormatException {
    URL base = null;
    if (baseStr != null) {
      base = new URL();
      new URLParser(base, null).parse(baseStr, URLParser.ParseState.NONE);
    }
    URL u = new URL();
    new URLParser(u, base).parse(urlStr, URLParser.ParseState.NONE);
    return u;
  }

  private static String urlArgument(Object[] args) {
    if (args.length > 0) {
      Object arg = args[0];
      if (arg != null && !Undefined.isUndefined(arg)) {
        return ScriptRuntime.toString(arg);
      }
    }
    return null;
  }

  private static String baseArgument(Object[] args) {
    if (args.length > 1) {
      Object arg = args[1];
      if (arg != null && !Undefined.isUndefined(arg)) {
        if (arg instanceof URL) {
          return ((URL) arg).serialize();
        }
        return ScriptRuntime.toString(arg);
      }
    }
    return null;
  }

  private void serializePath(StringBuilder sb) {
    if (opaquePath != null) {
      sb.append(opaquePath);
    } else {
      for (var seg : path) {
        sb.append('/');
        sb.append(seg);
      }
    }
  }

  private String serialize() {
    var s = new StringBuilder();
    s.append(scheme);
    s.append(':');
    if (host != null) {
      s.append("//");
      if (!username.isEmpty() || !password.isEmpty()) {
        s.append(username);
        if (!password.isEmpty()) {
          s.append(':');
          s.append(password);
        }
        s.append('@');
      }
      // TODO serialize host & port
      s.append(host);
      if (port != null) {
        s.append(':');
        s.append(port);
      }
    } else {
      if (opaquePath == null && path.size() > 1 && path.get(0).isEmpty()) {
        s.append("/.");
      }
    }
    serializePath(s);
    if (query != null && !query.isEmpty()) {
      s.append('?');
      s.append(query);
    }
    if (fragment != null && !fragment.isEmpty()) {
      s.append('#');
      s.append(fragment);
    }
    return s.toString();
  }

  private static URL realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, URL.class);
  }

  private void reparse(String urlStr, URLParser.ParseState state) {
    try {
      new URLParser(this, null).parse(urlStr, state);
    } catch (URLFormatException e) {
      // Swallow update failure
    }
  }

  private static Object getProtocol(Scriptable thisObj) {
    return realThis(thisObj).scheme + ':';
  }

  private static void setProtocol(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    self.reparse(s + ':', URLParser.ParseState.SCHEME_START);
  }

  private static Object getHash(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.fragment == null || self.fragment.isEmpty()) {
      return "";
    }
    return '#' + self.fragment.toString();
  }

  private static void setHash(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (s.isEmpty()) {
      self.fragment = null;
      return;
    }
    if (s.startsWith("#")) {
      s = s.substring(1);
    }
    self.reparse(s, URLParser.ParseState.FRAGMENT);
  }

  private static Object getSearch(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.query == null || self.query.isEmpty()) {
      return "";
    }
    return '?' + self.query;
  }

  private static void setSearch(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (s.isEmpty()) {
      self.query = null;
      // TODO update the search object
      return;
    }
    if (s.startsWith("?")) {
      s = s.substring(1);
    }
    self.query = null;
    self.reparse(s, URLParser.ParseState.QUERY);
  }

  private static Object getHost(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.host == null) {
      return "";
    }
    if (self.port == null) {
      return self.host;
    }
    return self.host + ':' + self.port;
  }

  private static void setHost(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (self.opaquePath != null) {
      return;
    }
    self.reparse(s, URLParser.ParseState.HOST);
  }

  private static Object getHostname(Scriptable thisObj) {
    var self = realThis(thisObj);
    // TODO serialize host
    return self.host == null ? "" : self.host;
  }

  private static void setHostname(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (self.opaquePath != null) {
      return;
    }
    self.reparse(s, URLParser.ParseState.HOSTNAME);
  }

  private static Object getPassword(Scriptable thisObj) {
    return realThis(thisObj).password;
  }

  private static void setPassword(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (self.upPortNotAllowed()) {
      return;
    }
    self.password = s;
  }

  private static Object getPathname(Scriptable thisObj) {
    StringBuilder sb = new StringBuilder();
    realThis(thisObj).serializePath(sb);
    return sb.toString();
  }

  private static void setPathname(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (self.opaquePath != null) {
      return;
    }
    self.path = new ArrayList<>();
    self.reparse(s, URLParser.ParseState.PATH_START);
  }

  private static Object getPort(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.port == null ? "" : self.port;
  }

  private static void setPort(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (self.upPortNotAllowed()) {
      return;
    }
    if (s.isEmpty()) {
      self.port = null;
    }
    self.reparse(s, URLParser.ParseState.PORT);
  }

  private static Object getUsername(Scriptable thisObj) {
    return realThis(thisObj).username;
  }

  private static void setUsername(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    if (self.upPortNotAllowed()) {
      return;
    }
    self.username = s;
  }

  private static Object getOrigin(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (URLUtils.schemeHasOrigin(self.scheme)) {
      Context cx = Context.getCurrentContext();
      return cx.newArray(
          self,
          new Object[] {
            self.scheme, self.host, self.port, null,
          });
    }
    return null;
  }

  private static Object getHref(Scriptable scriptable) {
    return realThis(scriptable).serialize();
  }

  private static void setHref(Scriptable thisObj, Object val) {
    String s = ScriptRuntime.toString(val);
    var self = realThis(thisObj);
    var nu = new URL();
    // TODO re-parse everything!
  }

  private static Object toJSON(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return realThis(thisObj).serialize();
  }

  private boolean upPortNotAllowed() {
    return (host == null || host.isEmpty() || "file".equals(scheme));
  }
}

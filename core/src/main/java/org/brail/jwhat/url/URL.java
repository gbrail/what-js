package org.brail.jwhat.url;

import java.util.ArrayList;
import java.util.List;
import org.brail.jwhat.core.impl.URLUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.RhinoException;
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

  URL() {}

  private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
    String url = requiredArg(args, 0, "url");
    String base = optionalArg(args, 1);
    return parseURL(url, base);
  }

  private static Object parse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String url = requiredArg(args, 0, "url");
    String base = optionalArg(args, 1);
    var u = parseURL(url, base);
    // TODO set prototype?
    u.setParentScope(scope);
    return u;
  }

  private static URL parseURL(String url, String base) {
    URL bu = null;
    if (base != null) {
      bu = new URL();
      var baseParser = new URLParser(base, bu, null);
      if (baseParser.isFailure()) {
        throw getParseFailure(baseParser);
      }
    }

    URL u = new URL();
    URLParser parser = new URLParser(url, u, bu);
    if (parser.isFailure()) {
      throw getParseFailure(parser);
    }
    return u;
  }

  private static RhinoException getParseFailure(URLParser parser) {
    assert parser.getErrors().isPresent();
    throw ScriptRuntime.typeError("Invalid URL: " + parser.getErrors().get());
  }

  private static Object canParse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String url = optionalArg(args, 0);
    String base = optionalArg(args, 1);
    if (url == null && base == null) {
      return false;
    }

    URL bu = null;
    if (base != null) {
      bu = new URL();
      var baseParser = new URLParser(base, bu, null);
      if (baseParser.isFailure()) {
        return false;
      }
    }

    if (url != null) {
      var u = new URL();
      URLParser parser = new URLParser(url, u, bu);
      if (parser.isFailure()) {
        return false;
      }
    }
    return true;
  }

  private static String requiredArg(Object[] args, int p, String name) {
    String val = optionalArg(args, p);
    if (val == null) {
      throw ScriptRuntime.typeError("The \"" + name + "\" argument must be supplied");
    }
    return val;
  }

  private static String optionalArg(Object[] args, int p) {
    if (args.length > p) {
      Object arg = args[p];
      if (arg != null && !Undefined.isUndefined(arg)) {
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

  private static Object getProtocol(Scriptable thisObj) {
    return realThis(thisObj).scheme + ':';
  }

  private static void setProtocol(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getHash(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.fragment == null || self.fragment.isEmpty()) {
      return "";
    }
    return '#' + self.fragment.toString();
  }

  private static void setHash(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getSearch(Scriptable thisObj) {
    var self = realThis(thisObj);
    if (self.query == null || self.query.isEmpty()) {
      return "";
    }
    return '?' + self.query;
  }

  private static void setSearch(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
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

  private static void setHost(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getHostname(Scriptable thisObj) {
    var self = realThis(thisObj);
    // TODO serialize host
    return self.host == null ? "" : self.host;
  }

  private static void setHostname(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getPassword(Scriptable thisObj) {
    return realThis(thisObj).password;
  }

  private static void setPassword(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getPathname(Scriptable thisObj) {
    StringBuilder sb = new StringBuilder();
    realThis(thisObj).serializePath(sb);
    return sb.toString();
  }

  private static void setPathname(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getPort(Scriptable thisObj) {
    var self = realThis(thisObj);
    return self.port == null ? "" : self.port;
  }

  private static void setPort(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getUsername(Scriptable thisObj) {
    return realThis(thisObj).username;
  }

  private static void setUsername(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
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

  private static void setHref(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object toJSON(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return realThis(thisObj).serialize();
  }
}

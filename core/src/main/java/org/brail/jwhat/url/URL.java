package org.brail.jwhat.url;

import org.brail.jwhat.core.impl.URLParser;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class URL extends ScriptableObject {
  private Object origin;
  private String protocol;
  private String username;
  private String password;
  private String host;
  private String port;
  private String pathname;
  private String search;
  private String hash;
  private String href;

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
    String url = requiredArg(args, 0, "url");
    String base = optionalArg(args, 1);
    var parser = parseURL(url, base);
    var u = new URL();
    u.fillState(cx, scope, parser);
    return u;
  }

  private static Object parse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String url = requiredArg(args, 0, "url");
    String base = optionalArg(args, 1);
    var parser = parseURL(url, base);
    var u = new URL();
    // TODO set prototype?
    u.setParentScope(scope);
    u.fillState(cx, scope, parser);
    return u;
  }

  private static URLParser parseURL(String url, String base) {
    URLParser baseParser = null;
    if (base != null) {
      baseParser = new URLParser(base, null);
      if (baseParser.isFailure()) {
        throw getParseFailure(baseParser);
      }
    }

    URLParser parser = new URLParser(url, baseParser);
    if (parser.isFailure()) {
      throw getParseFailure(parser);
    }
    return parser;
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

    URLParser baseParser = null;
    if (base != null) {
      baseParser = new URLParser(base, null);
      if (baseParser.isFailure()) {
        return false;
      }
    }

    if (url != null) {
      URLParser parser = new URLParser(url, baseParser);
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

  private void fillState(Context cx, Scriptable scope, URLParser parsed) {
    // TODO href and toJSON
    if (parsed.schemeHasOrigin()) {
      origin = URL.makeTupleOrigin(cx, scope, parsed);
    } else {
      origin = null;
    }
    protocol = parsed.scheme + ':';
    username = parsed.username;
    password = parsed.password;
    if (parsed.host == null) {
      host = "";
    } else if (parsed.port == null) {
      host = parsed.host;
    } else {
      host = parsed.host + ':' + parsed.port;
    }
    if (parsed.port == null) {
      port = "";
    } else {
      port = parsed.port;
    }
    pathname = serializePath(parsed);
    if (parsed.query == null || parsed.query.isEmpty()) {
      search = "";
    } else {
      search = '?' + parsed.query;
    }
    // TODO searchParams
    if (parsed.fragment == null || parsed.fragment.isEmpty()) {
      hash = "";
    } else {
      hash = '#' + parsed.fragment.toString();
    }
    // Needs to happen after everything above is set
    href = serialize(parsed);
  }

  private static Scriptable makeTupleOrigin(Context cx, Scriptable scope, URLParser p) {
    return cx.newArray(
        scope,
        new Object[] {
          p.scheme, p.host, p.port, null,
        });
  }

  private static String serializePath(URLParser p) {
    if (p.opaquePath != null) {
      return p.opaquePath.toString();
    }
    var b = new StringBuilder();
    for (var seg : p.path) {
      b.append('/');
      b.append(seg);
    }
    return b.toString();
  }

  private String serialize(URLParser p) {
    var s = new StringBuilder();
    s.append(protocol);
    if (!host.isEmpty()) {
      s.append("//");
      if (!username.isEmpty() || !password.isEmpty()) {
        s.append(username);
        if (!password.isEmpty()) {
          s.append(':');
          s.append(password);
        }
        s.append('@');
      }
      s.append(host);
      if (!port.isEmpty()) {
        s.append(':');
        s.append(port);
      }
    } else {
      if (p.opaquePath == null && p.path.size() > 1 && p.path.get(0).isEmpty()) {
        s.append("/.");
      }
    }
    s.append(pathname);
    s.append(search);
    s.append(hash);
    return s.toString();
  }

  private static URL realThis(Scriptable thisObj) {
    return LambdaConstructor.convertThisObject(thisObj, URL.class);
  }

  private static Object getProtocol(Scriptable scriptable) {
    return realThis(scriptable).protocol;
  }

  private static void setProtocol(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getHash(Scriptable scriptable) {
    return realThis(scriptable).hash;
  }

  private static void setHash(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getSearch(Scriptable scriptable) {
    return realThis(scriptable).search;
  }

  private static void setSearch(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getHost(Scriptable scriptable) {
    return realThis(scriptable).host;
  }

  private static void setHost(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getHostname(Scriptable scriptable) {
    return realThis(scriptable).host;
  }

  private static void setHostname(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getPassword(Scriptable scriptable) {
    return realThis(scriptable).password;
  }

  private static void setPassword(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getPathname(Scriptable scriptable) {
    return realThis(scriptable).pathname;
  }

  private static void setPathname(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getPort(Scriptable scriptable) {
    return realThis(scriptable).port;
  }

  private static void setPort(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getUsername(Scriptable scriptable) {
    return realThis(scriptable).username;
  }

  private static void setUsername(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getOrigin(Scriptable scriptable) {
    return realThis(scriptable).origin;
  }

  private static Object getHref(Scriptable scriptable) {
    return realThis(scriptable).href;
  }

  private static void setHref(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object toJSON(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    return realThis(thisObj).href;
  }
}

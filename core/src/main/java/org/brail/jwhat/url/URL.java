package org.brail.jwhat.url;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class URL extends ScriptableObject {
  private String origin;
  private String protocol;
  private String username;
  private String password;
  private String host;
  private String hostname;
  private String port;
  private String pathname;
  private String hash;

  // TODO search params

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
    String url = (args.length > 0 ? ScriptRuntime.toString(args[0]) : "");
    String base = (args.length > 1 ? ScriptRuntime.toString(args[1]) : null);
    var r = parseURL(url, base);
    if (r.isEmpty()) {
      throw ScriptRuntime.typeError("Invalid URL: " + url);
    }
    var u = new URL();
    u.fillState(r.get());
    return u;
  }

  private static Object parse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String url = (args.length > 0 ? ScriptRuntime.toString(args[0]) : "");
    String base = (args.length > 1 ? ScriptRuntime.toString(args[1]) : null);
    var r = parseURL(url, base);
    if (r.isEmpty()) {
      return null;
    }
    var u = new URL();
    // TODO set prototype?
    u.setParentScope(scope);
    u.fillState(r.get());
    return u;
  }

  private static Object canParse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String url = (args.length > 0 ? ScriptRuntime.toString(args[0]) : "");
    String base = (args.length > 1 ? ScriptRuntime.toString(args[1]) : null);
    var r = parseURL(url, base);
    return r.isPresent();
  }

  private void fillState(java.net.URL parsed) {
    protocol = parsed.getProtocol() + ":";
    hash = parsed.getRef() == null ? "" : "#" + parsed.getRef();
    pathname = parsed.getPath() == null ? "" : parsed.getPath();
    port = parsed.getPort() == -1 ? "" : String.valueOf(parsed.getPort());
    hostname = parsed.getHost();
    host = hostname + (port.isEmpty() ? "" : ":" + port);
    String userInfo = parsed.getUserInfo();
    if (userInfo != null && !userInfo.isEmpty()) {
      String[] parts = userInfo.split(":", 2);
      username = parts[0];
      password = parts.length > 1 ? parts[1] : "";
    } else {
      username = "";
      password = "";
    }
    origin = parsed.getProtocol() + "://" + host;
  }

  private static Optional<java.net.URL> parseURL(String url, String base) {
    if (base != null) {
      try {
        var baseUrl = new java.net.URI(base);
        return Optional.of(baseUrl.resolve(url).toURL());
      } catch (URISyntaxException | MalformedURLException | IllegalArgumentException ue) {
        return Optional.empty();
      }
    }
    try {
      return Optional.of(new java.net.URI(url).toURL());
    } catch (URISyntaxException | MalformedURLException | IllegalArgumentException ue) {
      return Optional.empty();
    }
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

  private static Object getHost(Scriptable scriptable) {
    return realThis(scriptable).host;
  }

  private static void setHost(Scriptable scriptable, Object o) {
    // TODO rebuild the URL!
  }

  private static Object getHostname(Scriptable scriptable) {
    return realThis(scriptable).hostname;
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
}

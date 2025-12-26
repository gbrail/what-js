package org.brail.jwhat.core.impl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Utils {
  public static String readResource(String path) throws IOException {
    try (InputStream in = Utils.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IOException("Could not find " + path);
      }
      StringBuilder buf = new StringBuilder();
      try (InputStreamReader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        char[] buffer = new char[4096];
        int bytesRead;
        while ((bytesRead = rdr.read(buffer)) != -1) {
          buf.append(buffer, 0, bytesRead);
        }
      }
      return buf.toString();
    }
  }

  public static Object evaluateResource(Context cx, Scriptable scope, String path) throws IOException {
    try (InputStream in = Utils.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IOException("Could not find " + path);
      }
      try (InputStreamReader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
        return cx.evaluateReader(scope, rdr, path, 1, null);
      }
    }
  }
}

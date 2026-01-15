package org.brail.jwhat.framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class Utils {
  public static Reader openResource(String path) throws IOException {
    InputStream in = Utils.class.getClassLoader().getResourceAsStream(path);
    if (in == null) {
      throw new IOException("Could not find " + path);
    }
    return new InputStreamReader(in, StandardCharsets.UTF_8);
  }

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
}

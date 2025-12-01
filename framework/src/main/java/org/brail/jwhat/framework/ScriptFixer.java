package org.brail.jwhat.framework;

import java.util.regex.Pattern;

public class ScriptFixer {
  private static final Pattern FOR_CONST = Pattern.compile("for\\s*\\(\\s*const\\s+");

  /** Replace JavaScript constructs that Rhino cannot handle yet. */
  public static String fixScript(String script) {
    return FOR_CONST.matcher(script).replaceAll("for (var ");
  }
}

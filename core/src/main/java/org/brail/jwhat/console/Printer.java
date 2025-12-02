package org.brail.jwhat.console;

public interface Printer {
  /**
   * Output a log message.
   *
   * @param level the severity level, as suggested by the spec
   * @param kind the kind of log message, such as "log", "info", "countEnd", etc.
   * @param message The formatted log message, with all format specifiers applied that could be
   *     applied
   * @param args Any remaining arguments after formatting
   */
  void print(Console.Level level, String kind, String message, Object... args);
}

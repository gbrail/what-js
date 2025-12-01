package org.brail.jwhat.console;

public interface Printer {
  void print(Console.Level level, String message, Object... args);
}

package org.brail.jwhat.cli;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.brail.jwhat.console.Console;
import org.brail.jwhat.console.Printer;

public class ConsolePrinter implements Printer {
  private final PrintWriter writer;

  public ConsolePrinter(PrintWriter writer) {
    this.writer = writer;
  }

  @Override
  public void print(Console.Level level, String kind, String message, Object... args) {
    if (args != null && args.length > 0) {
      writer.println(
          message
              + ' '
              + Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(" ")));
    } else {
      writer.println(message);
    }
  }
}

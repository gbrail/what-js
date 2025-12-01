package org.brail.jwhat.console;

public class StdoutPrinter implements Printer {
  @Override
  public void print(Console.Level level, String message, Object... args) {
    if (args.length == 0) {
      System.out.println(message);
    } else {
      var b = new StringBuilder(message);
      boolean first = true;
      for (Object arg : args) {
        if (first) {
          first = false;
        } else {
          b.append(' ');
        }
        b.append(arg.toString());
      }
      System.out.println(b);
    }
  }
}

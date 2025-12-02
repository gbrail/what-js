package org.brail.jwhat.cli;

import java.io.IOException;
import org.brail.jwhat.console.Console;
import org.brail.jwhat.url.URL;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;

public class CLI {
  public static void main(String[] args) {
    try (Context cx = Context.enter()) {
      var terminal = TerminalBuilder.builder().system(true).build();
      var reader = LineReaderBuilder.builder().terminal(terminal).build();
      var writer = terminal.writer();

      var scope = cx.initStandardObjects();
      Console.builder().printer(new ConsolePrinter(writer)).install(cx, scope);
      URL.init(cx, scope);

      while (true) {
        String line;
        try {
          line = reader.readLine("> ");
        } catch (EndOfFileException e) {
          // Exit cleanly on control-D
          break;
        }

        if ("exit".equalsIgnoreCase(line)) {
          break;
        }

        try {
          Object result = cx.evaluateString(scope, line, "CLI", 1, null);
          writer.println(ScriptRuntime.toString(result));
        } catch (RhinoException e) {
          writer.println(e);
          writer.print(e.getScriptStackTrace());
        }
      }

      terminal.close();

    } catch (IOException e) {
      System.err.println("Error creating terminal: " + e.getMessage());
    }
  }
}

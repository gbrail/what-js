package org.brail.jwhat.cli;

import java.io.IOException;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;

public class CLI {
  public static void main(String[] args) {
    try (Context cx = Context.enter()) {
      var scope = cx.initStandardObjects();

      var terminal = TerminalBuilder.builder().system(true).build();
      var reader = LineReaderBuilder.builder().terminal(terminal).build();

      while (true) {
        String line = reader.readLine("> ");

        if ("exit".equalsIgnoreCase(line)) {
          break;
        }

        try {
          Object result = cx.evaluateString(scope, line, "CLI", 1, null);
          terminal.writer().println(ScriptRuntime.toString(result));
        } catch (RhinoException e) {
          terminal.writer().println(e);
          terminal.writer().print(e.getScriptStackTrace());
        }
      }

      terminal.writer().println("Goodbye!");
      terminal.close();

    } catch (IOException e) {
      System.err.println("Error creating terminal: " + e.getMessage());
    }
  }
}

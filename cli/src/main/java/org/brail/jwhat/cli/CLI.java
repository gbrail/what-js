package org.brail.jwhat.cli;

import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.brail.jwhat.console.Console;
import org.brail.jwhat.url.URL;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class CLI implements Closeable {
  private static final Pattern WS = Pattern.compile("\\s");
  private static final String PROMPT = "> ";

  private final Terminal term;
  private final LineReader reader;
  private final PrintWriter writer;

  private CLI() throws IOException {
    term = TerminalBuilder.builder().system(true).build();
    reader = LineReaderBuilder.builder().terminal(term).build();
    writer = term.writer();
  }

  @Override
  public void close() throws IOException {
    term.close();
  }

  private Scriptable initialize(Context cx) {
    var scope = cx.initStandardObjects();
    Console.builder().printer(new ConsolePrinter(writer)).install(cx, scope);
    URL.init(cx, scope);
    return scope;
  }

  private void run() throws IOException {
    try (Context cx = Context.enter()) {
      var scope = initialize(cx);
      while (true) {
        String line;
        try {
          line = reader.readLine(PROMPT);
        } catch (EndOfFileException e) {
          // Exit cleanly on control-D
          break;
        }

        if (line.startsWith(".")) {
          if (handleCommand(cx, scope, line)) {
            break;
          }
        } else {
          try {
            Object result = cx.evaluateString(scope, line, "CLI", 1, null);
            writer.println(ScriptRuntime.toString(result));
          } catch (RhinoException e) {
            writer.println(e);
            writer.print(e.getScriptStackTrace());
          }
        }
      }
    }
  }

  private void runScript(String fileName) throws IOException {
    try (Context cx = Context.enter()) {
      var scope = initialize(cx);
      doLoad(cx, scope, fileName);
    }
  }

  public static void main(String[] args) {
    try {
      try (var cli = new CLI()) {
        if (args.length > 0) {
          cli.runScript(args[0]);
        } else {
          cli.run();
        }
      }
    } catch (IOException e) {
      System.err.println("Error creating terminal: " + e.getMessage());
    }
  }

  private boolean handleCommand(Context cx, Scriptable scope, String line) {
    String[] parts = WS.split(line, 2);
    switch (parts[0]) {
      case ".load":
        doLoad(cx, scope, parts.length > 1 ? parts[1] : "");
        break;
      case ".help":
        doHelp();
        break;
      case ".exit":
        return true;
      default:
        doDefault();
        break;
    }
    return false;
  }

  private void doHelp() {
    writer.println("Available commands:");
    writer.println("  .load <file name>: Run the specified script file");
    writer.println("  .help: Display this message");
    writer.println("  .exit: Exit");
  }

  private void doDefault() {
    writer.println("invalid command");
  }

  private void doLoad(Context cx, Scriptable scope, String fileName) {
    var p = Path.of(fileName);
    try (var rdr = new FileReader(fileName, StandardCharsets.UTF_8)) {
      cx.evaluateReader(scope, rdr, p.getFileName().toString(), 1, null);
    } catch (IOException ioe) {
      writer.println("error loading file: " + ioe);
    } catch (RhinoException re) {
      writer.println("error in script: " + re);
      writer.println(re.getScriptStackTrace());
    }
  }
}

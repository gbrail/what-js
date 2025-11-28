package org.brail.whatjs.framework;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WPTTestLauncher {
    private static final String TEST_BASE = "../wpt";

    private final Scriptable scope;
    private final Script harness;

    public static WPTTestLauncher newLauncher(Context cx, Scriptable scope) throws IOException {
        scope.put("self", scope, scope);
        scope.put("__testLog", scope, new LambdaFunction(scope, "TEST_LOG", 1, WPTTestLauncher::testLog));
        try (InputStream in = WPTTestLauncher.class.getResourceAsStream("/testharness.js")) {
            if (in == null) {
                throw new IOException("Could not find testharness.js resource");
            }
            try (InputStreamReader rdr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Script harness = cx.compileReader(rdr, "testharness.js", 1, null);
                return new WPTTestLauncher(scope, harness);
            }
        }
    }

    private WPTTestLauncher(Scriptable scope, Script harness) {
        this.scope = scope;
        this.harness = harness;
    }

    public void runTest(Context cx, String testPath) throws IOException {
        harness.exec(cx, scope);
        String testScript = Files.readString(Path.of(TEST_BASE, testPath));
        cx.evaluateString(scope, testScript, testPath, 1, null);
    }

    public void runScript(Context cx, String script) {
        harness.exec(cx, scope);
        cx.evaluateString(scope, script, "test.js", 1, null);
    }

    private static Object testLog(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        for (Object arg : args) {
            System.out.print(Context.toString(arg) + " ");
        }
        System.out.println();
        return Undefined.instance;
    }
}

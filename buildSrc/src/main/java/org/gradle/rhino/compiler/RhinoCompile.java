package org.gradle.rhino.compiler;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.optimizer.ClassCompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class RhinoCompile extends DefaultTask {

    @Optional
    @InputDirectory
    abstract public DirectoryProperty getSourceDir();

    @OutputDirectory
    abstract public DirectoryProperty getOutputDir();

    @TaskAction
    public void compile() throws IOException {
        if (!getSourceDir().isPresent()) {
            return;
        }
        var srcFile = getSourceDir().get().getAsFile();
        if (!srcFile.exists()) {
            return;
        }
        var src = srcFile.toPath();
        var dst = getOutputDir().get().getAsFile().toPath();
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) throws IOException {
                var fileName = p.getFileName().toString();
                if (fileName.endsWith(".js")) {
                    var rel = src.relativize(p);
                    var destFile = dst.resolve(rel);
                    var destParent = destFile.getParent();
                    Files.createDirectories(destParent);
                    destFile =
                            destParent.resolve(fileName.substring(0, fileName.length()-3) + ".class");
                    System.out.println("Reading " + p);
                    System.out.println("Writing " + destFile);
                    compile(p, destFile, rel);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void compile(Path src, Path dst, Path rel) throws IOException {
        assert(rel.toString().endsWith(".js"));
        String fq = rel.toString().substring(0, rel.toString().length()-3).
                replace(File.separator, ".");
        var source = Files.readString(src);

        try (Context cx = Context.enter()) {
            var env = new CompilerEnvirons();
            env.initFromContext(cx);
            var comp = new ClassCompiler(env);
            var result = comp.compileToClassFiles(source,
                    rel.getFileName().toString(),
                    1, fq);
            for (int i = 0; i < result.length; i+=2) {
                System.out.println("Result: " + (String)result[i]);
            }
            if (result.length != 2) {
                throw new AssertionError("Did not expect more than one class");
            }
            Files.write(dst, (byte[])result[1], StandardOpenOption.CREATE);
        }
    }
}

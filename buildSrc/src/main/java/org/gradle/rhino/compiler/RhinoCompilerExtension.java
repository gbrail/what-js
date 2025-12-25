package org.gradle.rhino.compiler;

import org.gradle.api.file.DirectoryProperty;

public abstract class RhinoCompilerExtension {
    abstract public DirectoryProperty getSourceDir();
    abstract public DirectoryProperty getOutputDir();
}

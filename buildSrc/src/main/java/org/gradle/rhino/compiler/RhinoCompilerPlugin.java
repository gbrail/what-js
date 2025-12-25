package org.gradle.rhino.compiler;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

public class RhinoCompilerPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().withType(org.gradle.api.plugins.JavaPlugin.class, javaPlugin -> {
      SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
      sourceSets.all(sourceSet -> {
        String taskName = "compile" + capitalize(sourceSet.getName()) + "Rhino";
        TaskProvider<RhinoCompile> rhinoCompileTask = project.getTasks().register(taskName, RhinoCompile.class, task -> {
          String srcDir = "src/" + sourceSet.getName() + "/rhino";
          task.getSourceDir().set(project.getLayout().getProjectDirectory().dir(srcDir));
          String outputDir = "classes/rhino/" + sourceSet.getName();
          task.getOutputDir().set(project.getLayout().getBuildDirectory().dir(outputDir));
        });

        sourceSet.getJava().srcDir(rhinoCompileTask.flatMap(RhinoCompile::getOutputDir));

        project.getTasks().named(sourceSet.getCompileJavaTaskName()).configure(compileJava -> {
          compileJava.dependsOn(rhinoCompileTask);
        });
      });
    });
  }

  private String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }
}

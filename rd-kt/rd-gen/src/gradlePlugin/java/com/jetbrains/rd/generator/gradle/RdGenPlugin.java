package com.jetbrains.rd.generator.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

@SuppressWarnings("unused")
public class RdGenPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getExtensions().create("rdgen", RdGenExtension.class, project);
    Configuration cfg = project.getConfigurations().create("rdGenConfiguration");
    project.getDependencies().add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.0");
    project.getDependencies().add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-stdlib:1.4.0");
    project.getDependencies().add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-reflect:1.4.0");
    project.getDependencies().add("rdGenConfiguration", "org.jetbrains.kotlin:kotlin-stdlib-common:1.4.0");
    project.getDependencies().add("rdGenConfiguration", "org.jetbrains.intellij.deps:trove4j:1.0.20181211");

    RdGenTask rdGenTask =  project.getTasks().create("rdgen", RdGenTask.class);
  }
}

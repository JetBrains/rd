package com.jetbrains.rd.generator.gradle;

import org.gradle.api.tasks.JavaExec;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RdGenTask extends JavaExec {
  private final RdGenExtension local = getExtensions().create("params", RdGenExtension.class, this);
  private final RdGenExtension global = getProject().getExtensions().findByType(RdGenExtension.class);

  public RdGenTask() {
    setMain("com.jetbrains.rd.generator.nova.MainKt");
  }

  @Override
  public void exec() {
    args(generateArgs());
    Set<File> files = getProject().getConfigurations().getByName("rdGenConfiguration").getFiles();
    Set<File> buildScriptFiles = getProject().getBuildscript().getConfigurations().getByName("classpath").getFiles();
    Set<File> rdFiles = new HashSet<>();
    for (File file : buildScriptFiles) {
      if (file.getName().contains("rd-")) {
        rdFiles.add(file);
      }
    }
    classpath(files);
    classpath(rdFiles);
    super.exec();
  }

  private List<String> generateArgs() {
    try {
      RdGenExtension effective = local.mergeWith(global);
      return effective.toArguments();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}

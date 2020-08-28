package com.jetbrains.rd.generator.gradle;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RdGenExtension {
  private final Project project;

  public RdGenExtension(Project project){
    this.project = project;
  }

  public RdGenExtension(Task task) {
    this.project = task.getProject();
  }

  public RdGenExtension mergeWith(RdGenExtension defaults) {
    RdGenExtension result = new RdGenExtension(project);
    result.sources(mergeFiles(defaults, e -> e.sources));
    result.setHashFolder(mergeObject(defaults, RdGenExtension::getHashFolder));
    result.setCompiled(mergeObject(defaults, RdGenExtension::getCompiled));
    result.classpath(mergeFiles(defaults, e -> e.classpath));
    result.setPackages(mergeObject(defaults, RdGenExtension::getPackages));
    result.setFilter(mergeObject(defaults, RdGenExtension::getFilter));
    result.setForce(mergeObject(defaults, RdGenExtension::getForce));
    result.setVerbose(mergeObject(defaults, RdGenExtension::getVerbose));
    result.setClearOutput(mergeObject(defaults, RdGenExtension::getClearOutput));
    result.generators.addAll(mergeObject(defaults, e -> e.generators));
    return result;
  }

  private <T> T mergeObject(RdGenExtension defaults, Function<RdGenExtension, T> getter) {
    T value = getter.apply(this);
    if (defaults == null) return value;
    if (value == null) return getter.apply(defaults);
    if (value instanceof Collection<?> && ((Collection<?>)value).isEmpty()) return getter.apply(defaults);
    if (value instanceof Map<?, ?> && ((Map<?, ?>)value).isEmpty()) return getter.apply(defaults);
    return value;
  }

  private Set<File> mergeFiles(RdGenExtension global, Function<RdGenExtension, List<?>> getter) {
    List<?> mergedFiles = mergeObject(global, getter);
    return  project.files(mergedFiles).getFiles();
  }

  public List<String> toArguments() throws IOException {
    ArrayList<String> arguments = new ArrayList<>();
    arguments.add("-s"); arguments.add(String.join(";", getSourceFiles()));
    if (getHashFolder() != null) {
      arguments.add("-h"); arguments.add(getHashFolder());
    }
    if (getCompiled() != null) {
      arguments.add("--compiled"); arguments.add(getCompiled());
    }
    arguments.add("-c"); arguments.add(String.join(System.getProperty("path.separator"), getClassPathEntries()));
    if (Objects.equals(getForce(), true)) arguments.add("-f");
    if (Objects.equals(getClearOutput(), true)) arguments.add("-x");
    if (getPackages() != null) {
      arguments.add("-p"); arguments.add(getPackages());
    }
    if (getFilter() != null) {
      arguments.add("--filter"); arguments.add(getFilter());
    }
    if (Objects.equals(getVerbose(), true)) arguments.add("-v");
    File generatorsFile = createGeneratorsFile();
    if (generatorsFile != null) {
      arguments.add("-g"); arguments.add(generatorsFile.getPath());
    }
    return arguments;
  }

  private String hashFolder;
  private String compiled;
  private Boolean force;
  private Boolean clearOutput;
  private String packages;
  private String filter;
  private Boolean verbose;

  public String getHashFolder() {
    return hashFolder;
  }

  public void setHashFolder(String hashFolder) {
    this.hashFolder = hashFolder;
  }

  public String getCompiled() {
    return compiled;
  }

  public void setCompiled(String compiled) {
    this.compiled = compiled;
  }

  public Boolean getForce() {
    return force;
  }

  public void setForce(Boolean force) {
    this.force = force;
  }

  public Boolean getClearOutput() {
    return clearOutput;
  }

  public void setClearOutput(Boolean clearOutput) {
    this.clearOutput = clearOutput;
  }

  public String getPackages() {
    return packages;
  }

  public void setPackages(String packages) {
    this.packages = packages;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public Boolean getVerbose() {
    return verbose;
  }

  public void setVerbose(Boolean verbose) {
    this.verbose = verbose;
  }

  private final List<GradleGenerationSpec> generators = new ArrayList<>();

  public GradleGenerationSpec generator(Closure<GradleGenerationSpec> closure) {
    GradleGenerationSpec spec = new GradleGenerationSpec();
    project.configure(spec, closure);
    generators.add(spec);
    return spec;
  }

  // TODO: ???
  // public void generator(closure: GradleGenerationSpec.() -> Unit) = GradleGenerationSpec().apply {
  //  closure()
  //  generators.add(this)
  // }

  private final List<Object> sources = new ArrayList<>();
  public void sources(Object... paths) {
    sources.addAll(Arrays.asList(paths));
  }

  private final List<Object> classpath = new ArrayList<>();
  public void classpath(Object... paths) {
    classpath.addAll(Arrays.asList(paths));
  }

  private List<String> getSourceFiles() {
    return project.files(sources).getFiles().stream().map(File::getPath).collect(Collectors.toList());
  }

  private List<String> getClassPathEntries() {
    return project.files(classpath).getFiles().stream().map(File::getPath).collect(Collectors.toList());
  }

  private File createGeneratorsFile() throws IOException {
    if (generators.isEmpty()) return null;
    File file = Files.createTempFile("rd-", ".generators").toFile();
    StringBuilder sb = new StringBuilder();
    for (GradleGenerationSpec generator : generators) {
      sb.append(generator.toString());
      sb.append("\n");
    }
    Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    return file;
  }
}

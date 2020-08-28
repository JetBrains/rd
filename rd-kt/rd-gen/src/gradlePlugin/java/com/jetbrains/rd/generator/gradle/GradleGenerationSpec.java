package com.jetbrains.rd.generator.gradle;

public class GradleGenerationSpec {
  private String language = "";
  private String transform = null;
  private String root = "";
  private String namespace = "";
  private String directory = "";
  private String generatedFileSuffix = ".Generated";

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getTransform() {
    return transform;
  }

  public void setTransform(String transform) {
    this.transform = transform;
  }

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public String getGeneratedFileSuffix() {
    return generatedFileSuffix;
  }

  public void setGeneratedFileSuffix(String generatedFileSuffix) {
    this.generatedFileSuffix = generatedFileSuffix;
  }

  @Override
  public String toString() {
    return language + "||" + transform + "||" + root + "||" + namespace + "||" + directory + "||" + generatedFileSuffix;
  }
}

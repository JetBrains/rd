package com.jetbrains.rd.gradle.tasks

import org.gradle.api.Task
import java.io.File
import java.nio.file.Paths


val Task.goldFilePath
    get() = Paths.get("${project.rootProject.rootDir}", "buildSrc", "src", "main", "resources", "gold", "$name.gold").toAbsolutePath().toString()

val Task.tmpFileDirectory: File
    get() = Paths.get("${project.rootProject.buildDir}", "src", "main", "resources", "tmp", System.getProperty("TmpSubDirectory")).toFile()

val Task.tmpFile: File
    get() = File(tmpFileDirectory, "$name.tmp")






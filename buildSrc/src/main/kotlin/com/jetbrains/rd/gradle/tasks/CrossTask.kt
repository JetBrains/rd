package com.jetbrains.rd.gradle.tasks

import org.gradle.api.Task
import java.nio.file.Paths


val Task.goldFilePath
    get() = Paths.get("${project.rootProject.rootDir}", "buildSrc", "src", "main", "resources", "gold", "$name.gold").toAbsolutePath().toString()

val Task.tmpFilePath
    get() = Paths.get("${project.rootProject.buildDir}", "src", "main", "resources", "tmp", "$name.tmp").toAbsolutePath().toString()






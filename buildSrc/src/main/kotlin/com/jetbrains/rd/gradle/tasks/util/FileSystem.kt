package com.jetbrains.rd.gradle.tasks.util

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File

val rdTmpDir: File = File(System.getProperty("java.io.tmpdir"), "rd")
    get() {
        field.mkdirs()
        return field
    }

val portFile = File(rdTmpDir, "port.txt")
    get() {
        rdTmpDir.mkdirs()
        return field
    }

val portFileClosed = portFile.resolveSibling("port.txt.closed")

fun copyGeneratedSources(currentSourceSet: SourceSet, currentProject: Project, generativeSourceSet: SourceSet) {
    val generatedDir = currentProject.buildDir.resolve("generated")
    generatedDir.mkdirs()
    currentSourceSet.java.srcDirs(generatedDir.absolutePath)
    generativeSourceSet.output.dirs.forEach { dir ->
        dir.copyRecursively(File(generatedDir, dir.name), overwrite = true)
    }
}
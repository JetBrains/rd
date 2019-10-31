package com.jetbrains.rd.gradle.tasks.util

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import java.io.File
import org.jetbrains.kotlin.gradle.plugin.*

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

val portFileStamp = portFile.resolveSibling("port.txt.stamp")

const val cppDirectorySystemPropertyKey = "model.out.src.cpp.dir"
const val ktDirectorySystemPropertyKey = "model.out.src.kt.dir"
const val csDirectorySystemPropertyKey = "model.out.src.cs.dir"

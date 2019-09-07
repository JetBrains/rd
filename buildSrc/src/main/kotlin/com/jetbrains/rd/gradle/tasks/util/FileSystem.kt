package com.jetbrains.rd.gradle.tasks.util

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
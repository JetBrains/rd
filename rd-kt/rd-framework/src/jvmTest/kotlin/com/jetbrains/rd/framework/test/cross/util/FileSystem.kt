package com.jetbrains.rd.framework.test.cross.util

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
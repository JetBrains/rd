package com.jetbrains.rd.generator.nova.cpp

import com.jetbrains.rd.util.string.Eol
import com.jetbrains.rd.util.string.PrettyPrinter
import java.io.File

class FileSystemPrettyPrinter(val file: File) {
    fun use(block: PrettyPrinter.() -> Unit) {
        file.bufferedWriter().use { writer ->
            PrettyPrinter().apply {
                setUp()

                block()

                writer.write(toString())
            }
        }
    }
}

internal fun PrettyPrinter.setUp() = this.apply {
    eolKind = Eol.linux
    step = 4
}
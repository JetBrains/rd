package com.jetbrains.rider.util

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.getThrowableText(): String {
    val stringWriter = StringWriter()
    val writer = PrintWriter(stringWriter)
    printStackTrace(writer)
    return stringWriter.buffer.toString()
}
package com.jetbrains.rd.generator.nova.util

import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.util.Statics
import java.util.*


val InvalidSysproperty = "--INVALID--"
fun syspropertyOrInvalid(name: String) : String {
    val properties = Statics<Properties>().get() ?: System.getProperties()
    return properties[name]?.toString() ?: "$InvalidSysproperty($name)"
}

internal fun getSourceFileAndLine() : String? {
    val rdgenNamespace = IGenerator::class.java.`package`.name

    Thread.currentThread().stackTrace.drop(1).forEach { ste ->
        if (!ste.className.startsWith(rdgenNamespace))
            return ste.fileName?.let { it + ":" + ste.lineNumber }
    }
    return null
}


package com.jetbrains.rd.generator.nova.util

import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.util.Statics
import java.util.*


/**
 * Marker constant. If [syspropertyOrInvalid] return some string that **contains** [InvalidSysproperty], it mean that property wasn't resolved correctly.
 */
const val InvalidSysproperty = "--INVALID--"

/**
 * Try to get java property with name [name] from [System.getProperties].
 * If failed get [default]
 * If [default] is null return string with some dignostics and [InvalidSysproperty] inside.
 */
fun syspropertyOrInvalid(name: String, default: String? = null) : String {
    val properties = System.getProperties()
    return properties[name]?.toString()
            ?: default
            ?: "$InvalidSysproperty($name)"
}

internal fun getSourceFileAndLine() : String? {
    val rdgenNamespace = IGenerator::class.java.`package`.name

    Thread.currentThread().stackTrace.drop(1).forEach { ste ->
        if (!ste.className.startsWith(rdgenNamespace))
            return ste.fileName?.let { it + ":" + ste.lineNumber }
    }
    return null
}


package com.jetbrains.rd.generator.nova.util

import com.jetbrains.rd.generator.nova.IGenerator
import com.jetbrains.rd.util.Statics
import java.util.*


/**
 * Marker constant. If [syspropertyOrInvalid] return some string that **contains** [InvalidSysproperty], it mean that property wasn't resolved correctly.
 */
const val InvalidSysproperty = "--INVALID--"

/**
 * Try to get java property with name [name] from [Statics] ?: [System.getProperties].
 * If failed get [default]
 * If [default] is null return string with some dignostics and [InvalidSysproperty] inside.
 */
fun syspropertyOrInvalid(name: String, default: String? = null) : String {
    val properties = Statics<Properties>().get() ?: System.getProperties()
    return properties[name]?.toString()
            ?: default
            ?: "$InvalidSysproperty($name)"
}

fun booleanSystemProperty(name: String, default: Boolean): Boolean {
    val propertyValue = syspropertyOrInvalid(name, default.toString())
    return propertyValue.equals("true", ignoreCase = true) || propertyValue == "1"
}

fun <T> usingSystemProperty(name: String, value: String, block: () -> T): T {
    val oldValue = System.getProperty(name)
    try {
        System.setProperty(name, value)
        return block()
    } finally {
        if (oldValue == null)
            System.clearProperty(name)
        else
            System.setProperty(name, oldValue)
    }
}

internal fun getSourceFileAndLine(withLine: Boolean): String? {
    val rdgenNamespace = IGenerator::class.java.`package`.name

    Thread.currentThread().stackTrace.drop(1).forEach { ste ->
        if (!ste.className.startsWith(rdgenNamespace))
            return (
                if (withLine) ste.fileName?.let { it + ":" + ste.lineNumber }
                else ste.fileName
            )
    }
    return null
}


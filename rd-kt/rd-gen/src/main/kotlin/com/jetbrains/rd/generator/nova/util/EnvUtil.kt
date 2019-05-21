package com.jetbrains.rd.generator.nova.util

import com.jetbrains.rd.util.Statics
import java.util.*


val InvalidSysproperty = "--INVALID--"
fun syspropertyOrInvalid(name: String) : String {
    val properties = Statics<Properties>().get() ?: System.getProperties()
    return properties[name]?.toString() ?: InvalidSysproperty
}



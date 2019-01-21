package com.jetbrains.rd.generator.nova.util

import com.jetbrains.rd.util.Statics
import java.util.*


fun syspropertyOrEmpty(name: String) : String {
    val properties = Statics<Properties>().get() ?: System.getProperties()
    return properties[name]?.toString() ?: ""
}



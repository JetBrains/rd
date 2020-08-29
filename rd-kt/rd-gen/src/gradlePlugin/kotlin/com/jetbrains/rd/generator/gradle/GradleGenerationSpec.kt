package com.jetbrains.rd.generator.gradle

class GradleGenerationSpec {
    var language = ""
    var transform: String? = null
    var root = ""
    var namespace = ""
    var directory = ""
    var generatedFileSuffix = ".Generated"

    override fun toString(): String {
        return "$language||$transform||$root||$namespace||$directory||$generatedFileSuffix"
    }
}
package com.jetbrains.rd.generator.nova.kotlin

import com.jetbrains.rd.generator.nova.ISanitizer

object KotlinSanitizer : ISanitizer {
    override val language: String
        get() = "Kotlin"

    private val keywords = arrayOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
        "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var",
        "when", "while", "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get",
        "import", "init", "param", "property", "receiver", "set", "setparam", "where", "actual", "abstract",
        "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external", "final", "infix",
        "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override", "private",
        "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg", "field", "it", "get", "set"
    )
    internal fun sanitize(name: String): String = if (isKeyword(name)) "`$name`" else name

    override fun isKeyword(name: String) = keywords.contains(name)
}
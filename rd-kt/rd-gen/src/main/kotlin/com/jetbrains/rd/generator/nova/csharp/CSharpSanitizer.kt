package com.jetbrains.rd.generator.nova.csharp

import com.jetbrains.rd.generator.nova.ISanitizer
import com.jetbrains.rd.util.string.condstr

object CSharpSanitizer : ISanitizer {
    override val language: String
        get() = "CSharp"


    private val keywords = arrayOf("abstract", "as", "base", "bool", "break",
        "byte", "case", "catch", "char", "checked",
        "class", "const", "continue", "decimal", "default",
        "delegate", "do", "double", "else", "enum",
        "event", "explicit", "extern", "false", "finally",
        "fixed", "float", "for", "foreach",
        "goto", "if", "implicit", "in", "int",
        "interface", "internal", "is", "lock", "long",
        "namespace", "new", "null", "object", "operator",
        "out", "override", "params", "private", "protected",
        "public", "readonly", "ref", "return", "sbyte",
        "sealed", "short", "sizeof", "stackalloc",
        "static", "string", "struct", "switch", "this",
        "throw", "true", "try", "typeof", "uint",
        "ulong", "unchecked", "unsafe", "ushort", "using",
        "var", "virtual", "void", "volatile", "while")

    internal fun sanitize(name: String, vararg contextVariables: String): String = isKeyword(name).condstr { "@" } + contextVariables.contains(name).condstr { "_" } + name

    override fun isKeyword(name: String) = keywords.contains(name)
}
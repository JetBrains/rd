package com.jetbrains.rd.generator.nova

interface ISanitizer {
    val language: String

    fun isKeyword(name: String) : Boolean
}
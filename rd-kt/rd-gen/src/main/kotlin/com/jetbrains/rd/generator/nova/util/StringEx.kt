package com.jetbrains.rd.generator.nova.util

fun String.decapitalizeInvariant() = replaceFirstChar { it.lowercase() }
fun String.capitalizeInvariant() = replaceFirstChar { it.uppercase() }

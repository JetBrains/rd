package com.jetbrains.rider.generator.nova.util

fun <T> Array<T>.joinToOptString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null) =
    if (isEmpty()) ""
    else toList().joinToString(separator = separator, prefix = prefix, postfix=postfix, transform = transform)

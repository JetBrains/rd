package com.jetbrains.rider.util

import com.jetbrains.rider.util.reflection.threadLocal


class Boxed<T>(var value: T) {}

class TlsBoxed<T>(initialValue : T) {
    var value: T by threadLocal(initialValue)
}

fun <T> TlsBoxed<Boolean>.forbidReentrancy(action: () -> T) : T {
    require(!value)
    value = true
    try {
        return action()
    } finally {
        value = false
    }
}
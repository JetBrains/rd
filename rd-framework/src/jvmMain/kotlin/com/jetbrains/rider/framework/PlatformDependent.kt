package com.jetbrains.rider.framework

import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.hasValue
import com.jetbrains.rider.util.threading.SpinWait

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    return SpinWait.spinUntil(Lifetime.Eternal, timeoutMs) {
        result.hasValue.apply { if (!this) pump() }
    }
}

actual fun createAbstractBuffer(): AbstractBuffer {
    return UnsafeBuffer(ByteArray(10))
}

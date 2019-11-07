package com.jetbrains.rd.framework

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.SpinWait

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    return SpinWait.spinUntil(Lifetime.Eternal, timeoutMs) {
        result.hasValue.apply { if (!this) pump() }
    }
}

actual fun createAbstractBuffer(): AbstractBuffer {
    return UnsafeBuffer(ByteArray(12))
}

actual fun createBackgroundScheduler(lifetime: Lifetime, name: String) : IScheduler = SingleThreadScheduler(lifetime, name)
package com.jetbrains.rd.framework

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.hasValue
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.SpinWait
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    return SpinWait.spinUntil(Lifetime.Eternal, timeoutMs) {
        result.hasValue.apply { if (!this) pump() }
    }
}

actual fun createAbstractBuffer(): AbstractBuffer {
    return UnsafeBuffer(ByteArray(12))
}

actual fun createBackgroundScheduler(lifetime: Lifetime, name: String) : IScheduler = SingleThreadScheduler(lifetime, name)
actual fun createAbstractBuffer(bytes: ByteArray): AbstractBuffer {
    return UnsafeBuffer(bytes)
}

internal actual suspend fun <T> IRdTask<T>.awaitInternal(): T = Lifetime.using { lifetime ->
    suspendCancellableCoroutine { c ->
        result.advise(lifetime) {
            when (it) {
                is RdTaskResult.Success -> c.resume(it.value)
                is RdTaskResult.Cancelled -> c.cancel(CancellationException("Task finished in Cancelled state"))
                is RdTaskResult.Fault -> c.resumeWithException(it.error)
            }
        }
    }
}
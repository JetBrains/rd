package com.jetbrains.rd.framework

import com.jetbrains.rd.util.CancellationException
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rd.util.threading.SynchronousScheduler
import org.khronos.webgl.ArrayBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual fun createAbstractBuffer(): AbstractBuffer {
    return JsBuffer(ArrayBuffer(10))
}

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    TODO("not implemented")
}

actual fun createBackgroundScheduler(lifetime: Lifetime, name: String): IScheduler = SynchronousScheduler //no bg on javascript

actual fun createAbstractBuffer(bytes: ByteArray): AbstractBuffer {
    val buffer = JsBuffer(ArrayBuffer(bytes.size))
    buffer.writeByteArrayRaw(bytes)
    buffer.rewind()
    return buffer
}

internal actual suspend fun <T> IRdTask<T>.awaitInternal(): T {
    return suspendCoroutine { c ->
        result.adviseOnce(Lifetime.Eternal) {
            when (it) {
                is RdTaskResult.Success -> c.resume(it.value)
                is RdTaskResult.Cancelled -> c.resumeWithException(CancellationException("Task finished in Cancelled state"))
                is RdTaskResult.Fault -> c.resumeWithException(it.error)
            }
        }
    }
}
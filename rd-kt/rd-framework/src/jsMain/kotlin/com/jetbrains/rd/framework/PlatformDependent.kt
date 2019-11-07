package com.jetbrains.rd.framework

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.SynchronousScheduler
import org.khronos.webgl.ArrayBuffer

actual fun createAbstractBuffer(): AbstractBuffer {
    return JsBuffer(ArrayBuffer(10))
}

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    TODO("not implemented")
}

actual fun createBackgroundScheduler(lifetime: Lifetime, name: String): IScheduler = SynchronousScheduler //no bg on javascript
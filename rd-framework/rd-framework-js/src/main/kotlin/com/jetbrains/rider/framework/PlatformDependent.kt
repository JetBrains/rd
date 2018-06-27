package com.jetbrains.rider.framework

import org.khronos.webgl.ArrayBuffer

actual fun createAbstractBuffer(): AbstractBuffer {
    return JsBuffer(ArrayBuffer(10))
}

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    TODO("not implemented")
}
package com.jetbrains.rd.framework

import org.khronos.webgl.ArrayBuffer

actual fun createAbstractBuffer(): AbstractBuffer {
    return JsBuffer(ArrayBuffer(10))
}

actual inline fun <T> IRdTask<T>.wait(timeoutMs: Long, pump: () -> Unit) : Boolean {
    TODO("not implemented")
}

actual fun createAbstractBuffer(bytes: ByteArray): AbstractBuffer {
    val buffer = JsBuffer(ArrayBuffer(bytes.size))
    buffer.writeByteArrayRaw(bytes)
    buffer.rewind()
    return buffer
}
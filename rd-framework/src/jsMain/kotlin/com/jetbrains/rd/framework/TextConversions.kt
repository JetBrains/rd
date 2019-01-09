package com.jetbrains.rd.framework

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Uint8Array

internal external class TextDecoder(encoding: String, options: dynamic = definedExternally) {
    val encoding: String

    fun decode(): String
    fun decode(buffer: ArrayBuffer): String
    fun decode(buffer: ArrayBuffer, options: dynamic): String
    fun decode(buffer: ArrayBufferView): String
    fun decode(buffer: ArrayBufferView, options: dynamic): String
}



internal external class TextEncoder(encoding: String, options: dynamic = definedExternally) {
    val encoding: String

    fun encode(): Uint8Array
    fun encode(buffer: String): Uint8Array
    fun encode(buffer: String, options: dynamic): Uint8Array
}
package com.jetbrains.rider.util.threading

object Sync {
    inline fun <R: Any?> lock(obj: Any, acton: () -> R) = synchronized(obj, acton)
    fun notifyAll(obj: Any) = (obj as Object).notifyAll()
    fun notify(obj: Any) = (obj as Object).notify()
    fun wait(obj: Any) = (obj as Object).wait()
}

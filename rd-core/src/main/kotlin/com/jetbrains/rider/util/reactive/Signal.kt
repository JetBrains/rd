package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.TlsBoxed
import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reflection.incrementCookie

class Signal<T> : ISignal<T> {
    companion object {
        private val cookie = TlsBoxed(0)

        private val isPriorityAdvise: Boolean get() = cookie.value > 0
        fun priorityAdviseSection(block:() -> Unit) = incrementCookie(cookie, TlsBoxed<Int>::value) { block() }

        fun Void() = Signal<Unit>()
    }


    private var priorityListeners = AtomicReference<Array<(T) -> Unit>>(emptyArray())
    private var listeners = AtomicReference<Array<(T) -> Unit>>(emptyArray())

    override fun fire(value: T) {
        priorityListeners.get().forEach { catch { it(value) } }
        listeners.get().forEach { catch { it(value) } }
    }


    override fun advise(lifetime : Lifetime, handler: (T) -> Unit) {
        advise0(if (Signal.isPriorityAdvise) priorityListeners else listeners, lifetime, handler)
    }


    private fun advise0(queue:AtomicReference<Array<(T) -> Unit>>, lifetime : Lifetime, handler: (T) -> Unit) {
        if (lifetime.isTerminated) return

        lifetime.bracket(
                {
                    queue.getAndUpdate { arr ->
                        if (arr.contains(handler)) throw IllegalArgumentException("Duplicate handler: $handler")
                        arr.insert(handler, arr.size)
                    }
                },
                {
                    queue.getAndUpdate { arr ->
                        arr.remove (handler).apply { if (equals(arr)) throw IllegalArgumentException("No handler: $handler") }
                    }
                }
        )
    }

}


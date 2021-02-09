package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.TlsBoxed
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reflection.incrementCookie

open class Signal<T> : ISignal<T> {
    companion object {
        private val cookie = TlsBoxed(0)

        private val isPriorityAdvise: Boolean get() = cookie.value > 0
        fun priorityAdviseSection(block:() -> Unit) = incrementCookie(cookie, TlsBoxed<Int>::value) { block() }

        fun Void() = Signal<Unit>()

        private val logger by lazy { getLogger<Signal<*>>() }
    }


    private var priorityListeners = AtomicReference<Array<(T) -> Unit>>(emptyArray())
    private var listeners = AtomicReference<Array<(T) -> Unit>>(emptyArray())

    // todo: fix the race condition during increment
    private var _changingCnt = 0
    override val changing: Boolean get() = _changingCnt > 0

    override fun fire(value: T) {
        incrementCookie(this, Signal<*>::_changingCnt) {
            priorityListeners.get().forEach { catch { it(value) } }
            listeners.get().forEach { catch { it(value) } }
        }
    }


    override fun advise(lifetime : Lifetime, handler: (T) -> Unit) {
        advise0(if (Signal.isPriorityAdvise) priorityListeners else listeners, lifetime, handler)
    }


    private fun advise0(queue:AtomicReference<Array<(T) -> Unit>>, lifetime : Lifetime, handler: (T) -> Unit) {
        if (!lifetime.isAlive) return

        lifetime.bracket(
                {
                    queue.getAndUpdate { arr ->
                        if (arr.contains(handler)) throw IllegalArgumentException("Duplicate handler: $handler")
                        if (arr.size == 10_000) {
                            logger.error { "10k handlers were added for a signal; this will cause performance degradation" }
                        }
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


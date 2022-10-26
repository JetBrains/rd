package com.jetbrains.rd.util

import kotlin.reflect.KClass

/**
 * Globals statics for classes managed in a stack-like way
 */
class Statics<T: Any> private constructor(val kclass: KClass<T>){

    companion object {
        private val map = mutableMapOf<KClass<*>, Statics<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <T:Any> of(kclass: KClass<T>) : Statics<T> {
            Sync.lock(map) {
                return map.getOrCreate(kclass) { Statics<T>(kclass) } as Statics<T>
            }
        }
        inline operator fun <reified T:Any> invoke() = of(T::class)
    }

    private var curValue = AtomicReference<T?>(null)

    fun get() = curValue.get()

    fun push(value: T) : Closeable {
        val old = curValue.getAndSet(value)
        return object : Closeable {
            override fun close() {
                if (!curValue.compareAndSet(value, old))
                    throw IllegalStateException("$this: current element must be $value but: $curValue")
            }
        }
    }

    inline fun<R> use(value: T, action: () -> R) :R = push(value).use { action() }


    override fun toString(): String {
        return Statics::class.simpleName+"<" + kclass.simpleName +">"
    }
}
package com.jetbrains.rd.util

import com.jetbrains.rd.util.reactive.IViewableList
import com.jetbrains.rd.util.reactive.ViewableList
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

    private val _stack = ViewableList<T>()
    val stack : IViewableList<T> get() = _stack

    fun get() : T? = _stack.lastOrNull()

    fun push(value: T) : Closeable {
        _stack.add(value)
        return object : Closeable {
            override fun close() {
                require (_stack.size > 0) { "$this: Empty stack" }
                require (_stack[_stack.lastIndex] == value) { "$this: Last element must be $value but: ${_stack[_stack.lastIndex]}" }
                _stack.removeAt(_stack.lastIndex)
            }
        }
    }

    inline fun<R> use(value: T, action: () -> R) :R = push(value).use { action() }


    override fun toString(): String {
        return Statics::class.simpleName+"<" + kclass.simpleName +">"
    }


}
package com.jetbrains.rd.framework.base

import com.jetbrains.rd.util.threadLocalWithInitial
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal object AllowBindingCookie {
    val myLocalCount = threadLocalWithInitial { 0 }

    val isBindAllowed get() = myLocalCount.get() > 0
    val isBindNotAllowed get() = !isBindAllowed

    @OptIn(ExperimentalContracts::class)
    inline fun allowBind(action: () -> Unit) {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        val thread = Thread.currentThread()
        myLocalCount.set(myLocalCount.get() + 1)
        try {
            action()
        } finally {
            if (thread != Thread.currentThread())
                throw IllegalAccessException("Wrong thread. Expected: $thread, but actual: ${Thread.currentThread()}")

            myLocalCount.set(myLocalCount.get() - 1)
        }
    }
}
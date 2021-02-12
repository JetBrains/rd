package com.jetbrains.rd.framework.util

import com.jetbrains.rd.framework.IRdEndpoint
import com.jetbrains.rd.framework.impl.RdEndpoint
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler

fun <TReq, TRes> IRdEndpoint<TReq, TRes>.setSuspend(
    cancellationScheduler: IScheduler? = null,
    handlerScheduler: IScheduler? = null,
    handler: suspend (Lifetime, TReq) -> TRes
) {
    // wireScheduler is not be available if RdEndpoint is not bound
    val coroutineDispatcher by lazy {
        val scheduler = handlerScheduler ?: (this as RdEndpoint).wireScheduler
        scheduler.asCoroutineDispatcher(allowInlining = true)
    }

    set(cancellationScheduler, handlerScheduler) { lt, req ->
        lt.startAsync(coroutineDispatcher) { handler(lt, req) }.toRdTask()
    }
}
package com.jetbrains.rd.framework.util

import com.jetbrains.rd.framework.IRdEndpoint
import com.jetbrains.rd.framework.impl.RdEndpoint
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.SynchronousScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Deprecated("Use the overload with CoroutineScope and CoroutineContext and pass all required context elements")
fun <TReq, TRes> IRdEndpoint<TReq, TRes>.setSuspend(
    cancellationScheduler: IScheduler? = null,
    handlerScheduler: IScheduler? = null,
    handler: suspend (Lifetime, TReq) -> TRes
) {
    // wireScheduler is not be available if RdEndpoint is not bound
    val coroutineDispatcher by lazy {
        val scheduler = handlerScheduler ?: (this as RdEndpoint).protocol?.scheduler ?: SynchronousScheduler
        scheduler.asCoroutineDispatcher(allowInlining = true)
    }

    set(cancellationScheduler, handlerScheduler) { lt, req ->
        lt.startAsync(coroutineDispatcher) { handler(lt, req) }.toRdTask()
    }
}

/**
 * Sets suspend handler for the [IRdEndpoint].
 *
 * When a protocol call is occurred it starts a new coroutine on [scope] passing [coroutineContext] and [coroutineStart] to it.
 * [cancellationScheduler] and [handlerScheduler] are passed to [IRdEndpoint.set]
 */
fun <TReq, TRes> IRdEndpoint<TReq, TRes>.setSuspend(
    scope: CoroutineScope,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    cancellationScheduler: IScheduler? = null,
    handlerScheduler: IScheduler? = null,
    handler: suspend (Lifetime, TReq) -> TRes
) {
    set(cancellationScheduler, handlerScheduler) { lt, req ->
        scope.async(coroutineContext, coroutineStart) {
            handler(lt, req)
        }.toRdTask()
    }
}
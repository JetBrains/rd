package com.jetbrains.rd.framework.util

import com.jetbrains.rd.util.AtomicReference
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.onTermination
import kotlinx.coroutines.*
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

open class RdCoroutineScope(lifetime: Lifetime) : CoroutineScope {
    companion object {
        private val logger = getLogger<RdCoroutineScope>()

        private val default = RdCoroutineScope(Lifetime.Eternal)
        private var currentHost: AtomicReference<RdCoroutineScope?> = AtomicReference(null)

        val current: RdCoroutineScope get() = currentHost.get() ?: default

        /**
         * Should be called on start of the application to override the default behavior of the Rd-based coroutines (default dispatcher, exception handler, shutdown behavior).
         */
        fun override(lifetime: Lifetime, host: RdCoroutineScope) {
            lifetime.bracket({
                if (!currentHost.compareAndSet(null, host)) {
                    throw IllegalStateException("Could not override RdCoroutineHost")
                }

                logger.info { "RdCoroutineHost overridden" }
            }, {
                if (!currentHost.compareAndSet(host, null)) {
                    throw IllegalStateException("currentHost must not be null")
                }

                logger.info { "RdCoroutineHost has been reset" }
            })
        }
    }

    final override val coroutineContext by lazy {
        defaultDispatcher + CoroutineExceptionHandler { _, throwable ->
            onException(throwable)
        }
    }

    protected open val defaultDispatcher: CoroutineContext get() = Dispatchers.Default

    init {
        lifetime.onTermination {
            shutdown()
            logger.info { "RdCoroutineHost disposed" }
        }
    }

    open fun onException(throwable: Throwable) {
        if (throwable !is CancellationException) {
            logger.error("Unhandled coroutine throwable", throwable)
        }
    }

    fun <T> async(
        lifetime: Lifetime,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        action: suspend CoroutineScope.() -> T
    ): Deferred<T> {
        val nestedDef = lifetime.createNested()
        return async(context, start, action).also { job -> nestedDef.synchronizeWith(job) }
    }

    fun launch(
        lifetime: Lifetime,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        action: suspend CoroutineScope.() -> Unit
    ): Job {
        val nestedDef = lifetime.createNested()
        return launch(context, start, action).also { job -> nestedDef.synchronizeWith(job) }
    }

    protected open fun shutdown() {
        try {
            runBlocking {
                coroutineContext[Job]!!.cancelAndJoin()
            }
        } catch (e: CancellationException) {
            // nothing
        } catch (e: Throwable) {
            logger.error(e)
        }
    }
}

fun Job.noAwait() {
    invokeOnCompletion { throwable ->
        if (throwable == null || isCancelled) return@invokeOnCompletion
        RdCoroutineScope.current.onException(throwable)
    }
}
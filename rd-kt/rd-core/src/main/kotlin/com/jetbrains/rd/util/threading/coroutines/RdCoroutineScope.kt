package com.jetbrains.rd.util.threading.coroutines

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

open class RdCoroutineScope : CoroutineScope {
    companion object {
        private val logger = getLogger<RdCoroutineScope>()

        private val default = RdCoroutineScope()
        private var currentHost: AtomicReference<RdCoroutineScope?> = AtomicReference(null)

        val current: RdCoroutineScope get() = currentHost.get() ?: default

        /**
         * Should be called on start of the application to override the default behavior of the Rd-based coroutines (default dispatcher, exception handler, shutdown behavior).
         */
        fun override(host: RdCoroutineScope) {
            if (!currentHost.compareAndSet(null, host))
                throw IllegalStateException("Could not override RdCoroutineHost")

            logger.debug { "RdCoroutineHost has been overridden" }
            host.coroutineContext.job.invokeOnCompletion {
                currentHost.getAndSet(null)
                logger.debug { "RdCoroutineHost has been reset" }
            }
        }
    }

    final override val coroutineContext by lazy {
        defaultContext + CoroutineExceptionHandler { _, throwable ->
            onException(throwable)
        }
    }

    protected open val defaultContext: CoroutineContext get() = Dispatchers.Default

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
}

fun Job.noAwait() {
    invokeOnCompletion { throwable ->
        if (throwable == null || isCancelled) return@invokeOnCompletion
        RdCoroutineScope.current.onException(throwable)
    }
}
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

            logger.trace { "RdCoroutineHost has been overridden" }
            host.coroutineContext.job.invokeOnCompletion {
                currentHost.getAndSet(null)
                logger.trace { "RdCoroutineHost has been reset" }
            }
        }
    }

    final override val coroutineContext by lazy {
        defaultContext + CoroutineExceptionHandler { _, throwable ->
            onException(throwable)
        }
    }

    protected open val defaultContext: CoroutineContext get() = Dispatchers.Default + SupervisorJob()

    open fun onException(throwable: Throwable) {
        if (throwable !is CancellationException) {
            logger.error("Unhandled coroutine throwable", throwable)
        }
    }

    val cancelledScope by lazy { createNestedScope(null).apply { cancel() } }

    internal fun createNestedScope(id: String?): CoroutineScope {
        var context = coroutineContext + SupervisorJob(parent = coroutineContext.job)
        if (id != null)
            context += CoroutineName(id)
        val scope = CoroutineScope(context)
        registerChildScope(scope)
        return scope
    }

    @Deprecated("Use coroutine scope built in lifetime",
        ReplaceWith("lifetime.coroutineScope.async(context, start, action)", "kotlinx.coroutines.async")
    )
    fun <T> async(
        lifetime: Lifetime,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        action: suspend CoroutineScope.() -> T
    ): Deferred<T> = lifetime.coroutineScope.async(context, start, action)

    @Deprecated("Use coroutine scope built in lifetime",
        ReplaceWith("lifetime.coroutineScope.launch(context, start, action)", "kotlinx.coroutines.launch")
    )
    fun launch(
        lifetime: Lifetime,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        action: suspend CoroutineScope.() -> Unit
    ): Job = lifetime.coroutineScope.launch(context, start, action)

    /**
     * An extension point to track and join scopes after lifetime termination.
     *
     * It is impossible to join scope inside lifetime termination, because it's a synchronous action,
     * but we want to wait for completion lifetime-related scopes during project/application termination,
     * so we can use this api to track and join scopes that  have been cancelled during lifetime termination
     */
    protected open fun registerChildScope(scope: CoroutineScope) {}
}

fun Job.noAwait() {
    invokeOnCompletion { throwable ->
        if (throwable == null || isCancelled) return@invokeOnCompletion
        RdCoroutineScope.current.onException(throwable)
    }
}
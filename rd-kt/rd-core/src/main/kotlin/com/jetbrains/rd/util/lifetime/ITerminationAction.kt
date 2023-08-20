package com.jetbrains.rd.util.lifetime

import com.jetbrains.rd.util.DelicateRdApi
interface ITerminationAction {
    /**
     * Optimizes throughput. Instead of creating a new lambda that calls termination code,
     * you can directly pass `this` to [Lifetime.onTermination].
     * Note: If invoked concurrently, this method might return false.
     */
    fun terminate(): Boolean
}

interface ICancellableTerminationAction : ITerminationAction {
    /**
     * This API is intended to help mark up as canceled joints between Lifetime and other similar data structures such as `com.intellij.openapi.openapi.util.Disposer`.
     *
     * You can call this method multiple times. However, after marking as cancelled,
     * it's imperative to call [terminate] to complete the termination flow.
     *
     * Warning: Failing to call [terminate] strictly after [markCancelled] may lead to [terminate]
     * returning false, which can result in some resources remaining undisposed.
     */
    @DelicateRdApi
    fun markCancelled()
}
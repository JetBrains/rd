package com.jetbrains.rd.util.lifetime

/**
 * Lifetime's lifecycle statuses. Lifetime is created in [Alive] status and eventually becomes [Terminated].
 * Status change is one way road: from lower ordinal to higher ([Alive] -> [Canceling] -> [Terminating] -> [Terminated]).
 */
enum class LifetimeStatus {
    /**
     * Lifetime is ready to use. Every lifetime's method will work.
     */
    Alive,
    /**
     * This status propagates instantly through all lifetime's children graph when [LifetimeDefinition.terminate] is invoked.
     * Lifetime is in consistent state (no resources are terminated) but termination process is already began. All background activities that block
     * termination (e.g. started with [Lifetime.executeOrThrow], should be interrupted  as fast as possible.
     * That's why background activities must check [Lifetime.isAlive] or [Lifetime.throwIfNotAlive]
     * quite ofter (200 ms is a good reference value).
     *
     * Some methods in this status still works, e.g. [Lifetime.onTermination] others do nothing ([Lifetime.executeIfAlive])
     * or throw [CancellationException] ([Lifetime.executeOrThrow], [Lifetime.bracketOrThrow])
     */
    Canceling,
    /**
     * Lifetime is in inconsistent state. Destruction begins: some resources are terminated, other not. All method throw exception or do nothing
     * (e.g. [Lifetime.onTerminationIfAlive]).
     */
    Terminating,
    /**
     * Lifetime is fully terminated, all resources are disposed and method's behavior is the same as in [Terminating] state.
     */
    Terminated
}
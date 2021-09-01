package com.jetbrains.rd.util.lifetime

import com.jetbrains.rd.util.AtomicReference
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.error

open class SequentialLifetimes(private val parentLifetime: Lifetime) {
    private val currentDef = AtomicReference(LifetimeDefinition.Terminated)

    init {
        parentLifetime += { setCurrentLifetime(LifetimeDefinition.Terminated) } //todo toRemove
    }

    open fun next(): LifetimeDefinition {
        val newDef = parentLifetime.createNested()
        setCurrentLifetime(newDef)
        return newDef
    }

    open fun terminateCurrent(): Unit = setCurrentLifetime(LifetimeDefinition.Terminated)

    val isTerminated: Boolean get() { //todo toRemove
        val current = currentDef.get()
        return current.isEternal || !current.isAlive
    }

    open fun defineNext(fNext: (LifetimeDefinition, Lifetime) -> Unit) {
        setCurrentLifetime(parentLifetime.createNested()) { ld ->
            try {
                ld.executeIfAlive { fNext(ld, ld.lifetime) }
            } catch (t: Throwable) {
                ld.terminate()
                throw t
            }
        }
    }

    /**
     * Atomically, assigns the new lifetime and terminates the old one.
     *
     * In case of a race condition, when current lifetime is overwritten, new lifetime is terminated.
     */
    protected fun setCurrentLifetime(newDef: LifetimeDefinition, action: ((LifetimeDefinition) -> Unit)? = null) {
        // Temporary lifetime definition that'll be used as a substitutor during current lifetime termination. We cannot
        // use Lifetime.Terminated here, because we need a distinct instance to use it in atomic operations later.
        val tempLifetimeDefinition = LifetimeDefinition()
        tempLifetimeDefinition.terminate()

        val old = currentDef.getAndSet(tempLifetimeDefinition)
        try {
            old.terminate(true)
        } catch (t: Throwable) {
            Logger.root.error(t)
        }

        try {
            action?.invoke(newDef)
        } finally {
            if (!currentDef.compareAndSet(tempLifetimeDefinition, newDef)) {
                // Means someone else has already interrupted us and replaced the current value (a race condition).
                newDef.terminate(true)
            }
        }
    }
}
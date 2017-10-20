package com.jetbrains.rider.util.lifetime

import java.util.concurrent.atomic.AtomicReference

open class SequentialLifetimes(private val parentLifetime: Lifetime) {
    private val currentDef = AtomicReference<LifetimeDefinition>(LifetimeDefinition.Eternal)

    init {
        parentLifetime += { setCurrentLifetime(LifetimeDefinition.Eternal) } //todo toRemove
    }

    open fun next(): Lifetime {
        val newDef = Lifetime.create(parentLifetime)
        setCurrentLifetime(newDef)
        return newDef.lifetime
    }

    open fun terminateCurrent(): Unit = setCurrentLifetime(LifetimeDefinition.Eternal)

    val isTerminated: Boolean get() = currentDef.get().isEternal || currentDef.get().isTerminated //todo toRemove

    open fun defineNext(fNext: (LifetimeDefinition, Lifetime) -> Unit) {
        setCurrentLifetime(LifetimeDefinition.Eternal)
        setCurrentLifetime(Lifetime.define(parentLifetime, fNext))
    }

    protected fun setCurrentLifetime(newDef: LifetimeDefinition) {
        currentDef.getAndSet(newDef).terminate()
    }
}
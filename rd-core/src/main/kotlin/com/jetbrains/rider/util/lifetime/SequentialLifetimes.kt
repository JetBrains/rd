package com.jetbrains.rider.util.lifetime


open class SequentialLifetimes(private val parentLifetime: Lifetime) {
    private var currentDef = LifetimeDefinition.Terminated

    init {
        parentLifetime += { setCurrentLifetime(LifetimeDefinition.Terminated) } //todo toRemove
    }

    open fun next(): LifetimeDefinition {
        val newDef = Lifetime.create(parentLifetime)
        setCurrentLifetime(newDef)
        return newDef
    }

    open fun terminateCurrent(): Unit = setCurrentLifetime(LifetimeDefinition.Terminated)

    val isTerminated: Boolean get() = currentDef.isEternal || currentDef.isTerminated //todo toRemove

    open fun defineNext(fNext: (LifetimeDefinition, Lifetime) -> Unit) {
        setCurrentLifetime(LifetimeDefinition.Terminated)
        setCurrentLifetime(Lifetime.define(parentLifetime, fNext))
    }

    protected fun setCurrentLifetime(newDef: LifetimeDefinition) {
        //todo atomicy
        val prev = currentDef
        currentDef = newDef
        prev.terminate()
    }
}
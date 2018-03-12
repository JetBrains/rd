package com.jetbrains.rider.util.lifetime


open class SequentialLifetimes(private val parentLifetime: Lifetime) {
    private var currentDef = LifetimeDefinition.Eternal

    init {
        parentLifetime += { setCurrentLifetime(LifetimeDefinition.Eternal) } //todo toRemove
    }

    open fun next(): Lifetime {
        val newDef = Lifetime.create(parentLifetime)
        setCurrentLifetime(newDef)
        return newDef.lifetime
    }

    open fun terminateCurrent(): Unit = setCurrentLifetime(LifetimeDefinition.Eternal)

    val isTerminated: Boolean get() = currentDef.isEternal || currentDef.isTerminated //todo toRemove

    open fun defineNext(fNext: (LifetimeDefinition, Lifetime) -> Unit) {
        setCurrentLifetime(LifetimeDefinition.Eternal)
        setCurrentLifetime(Lifetime.define(parentLifetime, fNext))
    }

    protected fun setCurrentLifetime(newDef: LifetimeDefinition) {
        //todo atomicy
        val prev = currentDef
        currentDef = newDef
        prev.terminate()
    }
}
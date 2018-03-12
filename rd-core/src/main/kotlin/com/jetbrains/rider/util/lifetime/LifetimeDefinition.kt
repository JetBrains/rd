package com.jetbrains.rider.util.lifetime

//allows multiple termination
class LifetimeDefinition internal constructor(val isEternal: Boolean = false) {
    val lifetime: Lifetime = Lifetime(isEternal)
    fun terminate() = lifetime.terminate()
    val isTerminated: Boolean get() = lifetime.isTerminated

    companion object {
        val Eternal: LifetimeDefinition = LifetimeDefinition(true)

        fun synchronize(vararg defs: LifetimeDefinition) {
            for (a in defs) {
                for (b in defs) {
                    val bb = b
                    if (bb != a) a.lifetime += { bb.terminate() }
                }
            }
        }
    }
}
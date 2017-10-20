package com.jetbrains.rider.util.lifetime


operator fun Lifetime.plusAssign(action : () -> Unit) {
    add(action)
}


fun Lifetime.intersect(lifetime: Lifetime): Lifetime {
    val intersection = LifetimeDefinition()
    this.attachNested(intersection)
    lifetime.attachNested(intersection)
    return intersection.lifetime
}

inline fun Lifetime.ifAlive(action : () -> Unit) {
    if (isTerminated) return
    action()
}

val Lifetime.isAlive: Boolean
    get() = !this.isTerminated

fun Lifetime.assertIsAlive() {
    if (isTerminated) throw AssertionError("lifetime not alive")
}

data class Lifetimed<T>(public val lifetime : Lifetime, public val value : T)

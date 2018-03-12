package com.jetbrains.rider.util.lifetime

import com.jetbrains.rider.util.reactive.IViewable
import com.jetbrains.rider.util.reactive.viewNotNull

val EternalLifetime get() = Lifetime.Eternal

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

inline fun <T> Lifetime.view(viewable: IViewable<T>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.view(this) { lt, value -> lt.handler(value) }
}

inline fun <T:Any> Lifetime.viewNotNull(viewable: IViewable<T?>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.viewNotNull(this) { lt, value -> lt.handler(value) }
}

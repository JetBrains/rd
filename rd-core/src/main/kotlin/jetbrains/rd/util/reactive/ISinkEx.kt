package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.lifetime.Lifetime

/**
 * Executes [handler] exactly once then terminates it subscription
 */
fun <T> ISink<T>.adviseOnce(lifetime: Lifetime, handler: (T) -> Unit) {
    adviseUntil(lifetime) { handler(it); true}
}

@Suppress("unused")
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "It's not safe! Since the inner lifetime passed to the [handler] " +
        "will be terminated after the first call with the subscription itself, " +
        "which is counter-intuitive and error-prone"
)
fun <T> IViewable<T>.viewOnce(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) {
}

/**
 * Holds subscription until [handler] returns true or [lifetime] is terminated
 */
fun <T> ISink<T>.adviseUntil(lifetime: Lifetime, handler: (T) -> Boolean) {
    if (lifetime.isTerminated) return

    Lifetime.define(lifetime) { definition, lt ->
        this.advise(lt) {
            if (handler(it)) definition.terminate()
        }
    }
}

fun <TSink, TSource> ISink<TSink>.flowInto(lifetime: Lifetime, target: ISource<TSource>, converter: (TSink) -> TSource)
{
    advise(lifetime) { target.fire(converter(it)) }
}

fun <T> ISink<T>.flowInto(lifetime: Lifetime, target: ISource<T>) {
    advise(lifetime) { value ->
        target.fire(value)
    }
}
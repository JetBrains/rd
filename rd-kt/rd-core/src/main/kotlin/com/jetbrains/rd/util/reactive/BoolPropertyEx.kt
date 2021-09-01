package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.lifetime.Lifetime

val IOptPropertyView<Boolean>.hasTrueValue: Boolean
    get() = this.valueOrNull == true

/**
 * Executes the given [action] every time the value of the property changes to true.
 */
fun IViewable<Boolean>.whenTrue(lifetime: Lifetime, action: (Lifetime) -> Unit) {
    view(lifetime) { lf, flag -> if (flag) action(lf)}
}

/**
 * Executes the given [action] every time the value of the property changes to false.
 */
fun IViewable<Boolean>.whenFalse(lifetime: Lifetime, action: (Lifetime) -> Unit) {
    view(lifetime) { lf, flag -> if (!flag) action(lf)}
}

fun IOptPropertyView<Boolean>.and(other: IOptPropertyView<Boolean>) =
        this.compose(other) { a, b -> a && b }

fun IOptPropertyView<Boolean>.or(other: IOptPropertyView<Boolean>) =
        this.compose(other) { a, b -> a || b }

fun IPropertyView<Boolean>.and(other: IPropertyView<Boolean>) =
        this.compose(other) { a, b -> a && b }

fun IPropertyView<Boolean>.or(other: IPropertyView<Boolean>) =
        this.compose(other) { a, b -> a || b }

fun IOptPropertyView<Boolean>.not() = map { a -> !a }
fun IPropertyView<Boolean>.not() = map { a -> !a }


package com.jetbrains.rd.util.reactive

import com.jetbrains.rd.util.Boxed
import com.jetbrains.rd.util.Maybe
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reflection.usingTrueFlag

fun <T1, T2, TRes> IPropertyView<T1>.compose(other: IPropertyView<T2>, composer: (T1, T2) -> TRes): IPropertyView<TRes> =
        CompositePropertyView(this, other, composer)

private class CompositePropertyView<T1, T2, TRes>(
        private val left: IPropertyView<T1>,
        private val right: IPropertyView<T2>,
        private val composer: (T1, T2) -> TRes
) : IPropertyView<TRes> {

    override val value: TRes get() = composer(left.value, right.value)
    override val change: ISource<TRes> = object : ISource<TRes> {
        override fun advise(lifetime: Lifetime, handler: (TRes) -> Unit) {
            var lastValue = value

            fun handleIfChanged() {
                val newValue = value
                if (newValue == lastValue) return
                lastValue = newValue
                handler(newValue)
            }

            left.change.advise(lifetime) { handleIfChanged() }
            right.change.advise(lifetime) { handleIfChanged() }
        }
    }
}

fun <T1 : Any, T2 : Any, TRes : Any> IOptPropertyView<T1>.compose(other: IOptPropertyView<T2>, composer: (T1, T2) -> TRes): IOptPropertyView<TRes> = CompositeOptPropertyView(this, other, composer)

private class CompositeOptPropertyView<T1 : Any, T2 : Any, TRes : Any>(
        private val left: IOptPropertyView<T1>,
        private val right: IOptPropertyView<T2>,
        private val composer: (T1, T2) -> TRes
) : IOptPropertyView<TRes> {
    override val valueOrNull: TRes?
        get() {
            val v1: T1 = left.valueOrNull ?: return null
            val v2: T2 = right.valueOrNull ?: return null
            return composer(v1, v2)
        }

    override val change: ISource<TRes> = object : ISource<TRes> {
        override fun advise(lifetime: Lifetime, handler: (TRes) -> Unit) {
            var lastValue: TRes? = valueOrNull

            fun handleIfChanged() {
                val newValue = valueOrNull
                if (newValue != null && newValue != lastValue) {
                    handler(newValue)
                    lastValue = newValue
                }
            }

            left.change.advise(lifetime) { handleIfChanged() }
            right.change.advise(lifetime) { handleIfChanged() }
        }
    }
}

fun <TSource, TResult : Any> List<IPropertyView<TSource>>.foldRight(lifetime: Lifetime, initial: TResult, func: (TSource, TResult) -> TResult): OptProperty<TResult> {
    val property = OptProperty<TResult>()
    for (p in this) {
        p.advise(lifetime) {
            val value = this.foldRight(initial) { x, acc -> func(x.value, acc) }
            property.set(value)
        }
    }
    return property
}

//@JvmName("foldRightOpt")
fun <TSource : Any, TResult : Any> List<IOptPropertyView<TSource>>.foldRightOpt(lifetime: Lifetime, initial: TResult, func: (TSource, TResult) -> TResult): OptProperty<TResult> {
    val property = OptProperty<TResult>()
    for (p in this) {
        p.advise(lifetime) {
            val value = this.foldRight(initial) { x, acc -> x.valueOrNull?.let { func(it, acc) } ?: acc }
            property.set(value)
        }
    }
    return property
}

fun <TSource, TResult : Any> Iterable<IPropertyView<TSource>>.fold(lifetime: Lifetime, initial: TResult, func: (TResult, TSource) -> TResult): OptProperty<TResult> {
    val property = OptProperty<TResult>()
    for (p in this) {
        p.advise(lifetime) {
            val value = this.fold(initial) { acc, x -> func(acc, x.value) }
            property.set(value)
        }
    }
    return property
}

fun <TSource : Any, TResult : Any> Iterable<IOptPropertyView<TSource>>.foldOpt(lifetime: Lifetime, initial: TResult, func: (TResult, TSource) -> TResult): OptProperty<TResult> {
    val property = OptProperty<TResult>()
    for (p in this) {
        p.advise(lifetime) {
            val value = this.fold(initial) { acc, x -> x.valueOrNull?.let { func(acc, it ) } ?: acc }
            property.set(value)
        }
    }
    return property
}

/*
fun <T : Any> Iterable<IOptProperty<T>>.all(lifetime: Lifetime, predicate: (T) -> Boolean): IPropertyView<Boolean> {
    return this.fold(lifetime, true) { acc, x -> acc && predicate(x) }
}

fun <T : Any> Iterable<IOptProperty<T>>.any(lifetime: Lifetime, predicate: (T) -> Boolean): IPropertyView<Boolean> {
    return this.fold(lifetime, false) { acc, x -> acc || predicate(x) }
}

fun Iterable<IOptProperty<Boolean>>.any(lifetime: Lifetime): IPropertyView<Boolean> {
    return this.fold(lifetime, false) { acc, x -> acc || x }
}

fun Iterable<IOptProperty<Boolean>>.all(lifetime: Lifetime): IPropertyView<Boolean> {
    return this.fold(lifetime, true) { acc, x -> acc && x }
}
*/


fun <T> ISource<T>.distinct() = object : ISource<T> {
    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        var old : Maybe<T> = Maybe.None
        this@distinct.advise(lifetime) {
            val new = Maybe.Just(it)
            if (new != old) {
                old = new
                handler(it)
            }
        }
    }
}


fun <T> ISource<T>.filter(f: (T) -> Boolean) = object : ISource<T> {
    override fun advise(lifetime: Lifetime, handler: (T) -> Unit) {
        this@filter.advise(lifetime) {
            if (f(it))
                handler(it)
        }
    }
}

fun <T, R> IPropertyView<T>.map(f: (T) -> R) = object : IPropertyView<R> {
    override val change: ISource<R> = object : ISource<R> {
        override fun advise(lifetime: Lifetime, handler: (R) -> Unit) {
            var lastValue = value
            this@map.advise(lifetime) {
                val newValue = f(it)
                if (newValue != lastValue) {
                    lastValue = newValue
                    handler(f(it))
                }
            }
        }
    }

    override val value: R
        get() = f(this@map.value)
}



fun <T : Any, R : Any> IOptPropertyView<T>.map(f: (T) -> R) = object : IOptPropertyView<R> {
    override val valueOrNull: R?
        get() = this@map.valueOrNull?.let { f(it) }

    override val change: ISource<R> = object : ISource<R> {
        override fun advise(lifetime: Lifetime, handler: (R) -> Unit) {
            var lastValue = valueOrNull
            this@map.advise(lifetime) {
                val newValue = f(it)
                if (newValue != lastValue) {
                    lastValue = newValue
                    handler(f(it))
                }
            }
        }
    }
}

// bind property to UI control
fun <T> IMutablePropertyBase<T>.bind(lifetime: Lifetime, setValue: (value: T) -> Unit, valueUpdated: ((value: T) -> Unit) -> Unit) {
    val guard = Boxed(false)

    advise(lifetime) {
        if (!guard.value) setValue(it)
    }

    valueUpdated { v ->
        lifetime.executeIfAlive {
            guard.usingTrueFlag(Boxed<Boolean>::value) {
                set(v)
            }
        }
    }
}



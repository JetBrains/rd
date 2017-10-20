package com.jetbrains.rider.util.reactive

import com.jetbrains.rider.util.Maybe
import com.jetbrains.rider.util.collections.Stack
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.lifetime.SequentialLifetimes
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.onFalse
import com.jetbrains.rider.util.putUnique
import com.jetbrains.rider.util.time.InfiniteDuration
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


interface IScheduler {
    fun queue(action: () -> Unit)
    val isActive: Boolean
    fun assertThread() = isActive.onFalse {
        throw IllegalStateException("Illegal scheduler for current action, must be: $this, current thread: ${Thread.currentThread().name}")
    }

    //Provides better performance but loose event consistency.
    val outOfOrderExecution : Boolean get() = false


    fun invokeOrQueue(action: () -> Unit) {
        if (isActive) action()
        else queue(action)
    }
}


interface ISink<out T> {
    fun advise(lifetime : Lifetime, handler : (T) -> Unit)
}

interface IAsyncSink<out T> {
    fun adviseOn(lifetime : Lifetime, scheduler : IScheduler, handler : (T) -> Unit)
}

typealias IVoidSink = ISink<Unit>
fun IVoidSink.advise(lifetime: Lifetime, handler: () -> Unit) = advise(lifetime) { handler() }
fun IAsyncSink<Unit>.adviseOn(lifetime : Lifetime, scheduler : IScheduler, handler : () -> Unit) = adviseOn(lifetime, scheduler) { handler() }



interface IReadonlyProperty<out T> : ISink<T>, IViewable<T> {
    val maybe: Maybe<T>
    val value: T //could lead to exception
    val change: ISink<T>

    override fun view(lifetime : Lifetime, handler : (Lifetime, T) -> Unit) {
        if (lifetime.isTerminated) return

        // nested lifetime is needed due to exception that could be thrown
        // while viewing a property change right at the moment of <param>lifetime</param>'s termination
        // but before <param>handler</param> gets removed (e.g. p.view(lf) { /*here*/ }; lf += { p.set(..) })
        val lf = lifetime.createNested()
        SequentialLifetimes(lf).let {
            advise(lf) {v ->
                handler(it.next(), v)
            }
        }
    }
    operator fun invoke() = value
}


interface ISource<in T> {
    fun fire(value : T)
}

//Touching the Void
typealias IVoidSource = ISource<Unit>
fun IVoidSource.fire() = fire(Unit)


interface ISignal<T> : ISource<T>, ISink<T>
interface IAsyncSignal<T> : ISource<T>, IAsyncSink<T>

typealias IVoidSignal = ISignal<Unit>
typealias IAsyncVoidSignal = IAsyncSignal<Unit>

interface IViewable<out T> {
    fun view(lifetime : Lifetime, handler : (Lifetime, T) -> Unit)
}

interface IProperty<T> : IReadonlyProperty<T>, IViewable<T> {
    override var value : T

    operator fun timesAssign(v : T) { value = v }
}


enum class AddRemove {Add, Remove}


interface IViewableSet<T : Any> : Set<T>, IViewable<T>, ISink<IViewableSet.Event<T>> {
    data class Event<T>(val kind: AddRemove, val value: T)

    fun advise(lifetime: Lifetime, handler: (AddRemove, T) -> Unit) = advise(lifetime) {evt -> handler(evt.kind, evt.value)}

    override fun view(lifetime: Lifetime, handler: (Lifetime, T) -> Unit) {
        val lifetimes = hashMapOf<T, LifetimeDefinition>()
        advise(lifetime) { kind, v ->
            when (kind) {
                AddRemove.Add -> {
                    val def = lifetimes.putUnique(v, Lifetime.create(lifetime))
                    handler(def.lifetime, v)
                }
                AddRemove.Remove -> lifetimes.remove(v)!!.terminate()
            }
        }
    }
}

interface IMutableViewableSet<T:Any> : MutableSet<T>, IViewableSet<T>


data class KeyValuePair<K,V>(override val key: K, override val value: V) : Map.Entry<K, V>

interface IViewableMap<K : Any, V:Any> : Map<K, V>, IViewable<Map.Entry<K, V>>, ISink<IViewableMap.Event<K, V>> {
    sealed class Event<K,V>(val key: K) {
        class Add<K,V>   (key: K,                   val newValue : V) : Event<K,V>(key)
        class Update<K,V>(key: K, val oldValue : V, val newValue : V) : Event<K,V>(key)
        class Remove<K,V>(key: K, val oldValue : V                  ) : Event<K,V>(key)

        val newValueOpt: V? get() = when (this) {
            is Event.Add    -> this.newValue
            is Event.Update -> this.newValue
            else -> null
        }
    }

    override fun view(lifetime: Lifetime, handler: (Lifetime, Map.Entry<K, V>) -> Unit) {
        val lifetimes = hashMapOf<Map.Entry<K, V>, LifetimeDefinition>()
        adviseAddRemove(lifetime) { kind, key, value ->
            val entry = KeyValuePair(key, value)
            when (kind) {
                AddRemove.Add -> {
                    val def = lifetimes.putUnique(entry, Lifetime.create(lifetime))
                    handler(def.lifetime, entry)
                }
                AddRemove.Remove -> {
                    val remove = lifetimes.remove(entry) ?: error("attempting to remove non-existing item $entry")
                    remove.terminate()
                }
            }
        }
    }

    fun adviseAddRemove(lifetime: Lifetime, handler: (AddRemove, K, V) -> Unit) {
        advise(lifetime) { when (it) {
            is Event.Add -> handler(AddRemove.Add, it.key, it.newValue)
            is Event.Update -> {
                handler(AddRemove.Remove, it.key, it.oldValue)
                handler(AddRemove.Add, it.key, it.newValue)
            }
            is Event.Remove -> handler(AddRemove.Remove, it.key, it.oldValue)
        }}
    }

    fun view(lifetime: Lifetime, handler: (Lifetime, K, V) -> Unit) = view(lifetime, {lf, entry -> handler(lf, entry.key, entry.value)})
}

interface IViewableList<V: Any> : List<V>, IViewable<V>, ISink<IViewableList.Event<V>> {
    @Suppress("AddVarianceModifier")
    data class Event<V>(val kind: AddRemove, val value: V, val index: Int)
    fun advise(lifetime: Lifetime, handler: (AddRemove, V, Int) -> Unit) = advise(lifetime) {evt -> handler(evt.kind, evt.value, evt.index)}

    override fun view(lifetime: Lifetime, handler: (Lifetime, V) -> Unit) {
        val lifetimes = hashMapOf<V, LifetimeDefinition>()
        advise(lifetime) { (kind, value) ->
            when (kind) {
                AddRemove.Add -> {
                    val def = lifetimes.putUnique(value, Lifetime.create(lifetime))
                    handler(def.lifetime, value)
                }
                AddRemove.Remove -> lifetimes.remove(value)!!.terminate()
            }
        }
    }
}

interface IAsyncViewableMap<K : Any, V: Any> : IViewableMap<K, V>, IAsyncSink<IViewableMap.Event<K, V>>

interface IMutableViewableMap<K : Any, V: Any> : MutableMap<K, V>, IViewableMap<K, V>

interface IMutableViewableList<V : Any> : MutableList<V>, IViewableList<V>

interface IViewableStack<T : Any> : Stack<T>, IViewable<T>




interface ITrigger<T> : IReadonlyProperty<T> {
    fun wait(cancellationToken : Lifetime, timeout: Duration) : Boolean {
        //short circuit
        if (cancellationToken.isTerminated) return false
        if (hasValue) return true

        val lock = CountDownLatch(1)
        cancellationToken += {lock.countDown()}

        adviseOnce(cancellationToken) { lock.countDown() }
        if (cancellationToken.isTerminated) return false

        return lock.await(timeout.toNanos(), TimeUnit.NANOSECONDS)
    }
}

fun<T> ITrigger<T>.wait() : Boolean = wait(Lifetime.Eternal, InfiniteDuration)
fun<T> ITrigger<T>.wait(timeout: Duration) : Boolean = wait(Lifetime.Eternal, timeout)

interface IMutableTrigger<T> : IProperty<T>, ITrigger<T>
//@file:JvmName("LifetimeKt")

package com.jetbrains.rd.util.lifetime

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.collections.CountingSet
import com.jetbrains.rd.util.lifetime.LifetimeStatus.*
import com.jetbrains.rd.util.reactive.IViewable
import com.jetbrains.rd.util.reactive.viewNotNull
import com.jetbrains.rd.util.reflection.threadLocal

enum class LifetimeStatus {
    Alive,
    Canceled,
    Terminating,
    Terminated
}


sealed class Lifetime {
    companion object {
        private val threadLocalExecutingBackingFiled : ThreadLocal<CountingSet<Lifetime>> = threadLocalWithInitial { CountingSet() }
        // !!! IMPORTANT !!! Don't use 'by ThreadLocal' to avoid slow reflection initialization
        internal val threadLocalExecuting get() = threadLocalExecutingBackingFiled.get()

        var waitForExecutingInTerminationTimeout = 500L //timeout for waiting executeIfAlive in termination

        val Eternal : Lifetime get() = LifetimeDefinition.eternal //some marker
        val Terminated get() = LifetimeDefinition.Terminated.lifetime

        inline fun <T> using(block : (Lifetime) -> T) : T{
            val def = LifetimeDefinition()
            try {
                return block(def.lifetime)
            } finally {
                def.terminate()
            }
        }


        @Deprecated("Use lifetime.createNested { def -> ... }")
        fun define(lifetime: Lifetime, atomicAction : (LifetimeDefinition, Lifetime) -> Unit) = lifetime.createNested { atomicAction(it, it) }

        @Deprecated("Use lifetime.createNested", ReplaceWith("lifetime.createNested()"))
        fun create(lifetime: Lifetime): LifetimeDefinition = lifetime.createNested()
    }

    @Deprecated("Use lifetime.createNested", ReplaceWith("lifetime.createNested()"))
    fun createNestedDef() = createNested()

    fun createNested() = LifetimeDefinition().also { attach(it) }

    fun createNested(atomicAction : (LifetimeDefinition) -> Unit) = createNested().also { nested ->
        attach(nested)
        try {
            nested.executeIfAlive { atomicAction(nested) }
        } catch (e: Exception) {
            nested.terminate()
            throw e
        }
    }

    inline fun <T> usingNested(action: (Lifetime) -> T): T {
        val nested = createNested()
        return try {
            action(nested.lifetime)
        } finally {
            nested.terminate()
        }
    }

    abstract val status : LifetimeStatus

    abstract fun <T : Any> executeIfAlive(action: () -> T) : T?

    abstract fun onTerminationIfAlive(action: () -> Unit): Boolean
    abstract fun onTerminationIfAlive(closeable: Closeable): Boolean


    abstract fun <T : Any> bracket(opening: () -> T, terminationAction: () -> Unit): T?
    internal abstract fun attach(child: LifetimeDefinition)

    @Deprecated("Use onTermination", ReplaceWith("onTermination(action)"))
    fun add(action: () -> Unit) = onTermination(action)

    @Deprecated("Use !isAlive")
    val isTerminated: Boolean get() = !isAlive

    @Deprecated("Use executeIfAlive(action)", ReplaceWith("executeIfAlive(action)"))
    fun <T : Any> ifAlive(action: () -> T) = executeIfAlive(action)
}


class LifetimeDefinition : Lifetime() {
    val lifetime: Lifetime get() = this

    companion object {
        internal val eternal = LifetimeDefinition()
        private val log : Logger by lazy {getLogger<Lifetime>()}

        //State decomposition
        private val executingSlice = BitSlice.int(20)
        private val statusSlice = BitSlice.enum<LifetimeStatus>(executingSlice)
        private val mutexSlice = BitSlice.bool(statusSlice)

        val Terminated : LifetimeDefinition = LifetimeDefinition()

        init {
            Terminated.terminate()
        }
    }



    //Fields
    private var state = AtomicInteger()
    private var resources = mutableListOf<Any>()

    /**
     * Only possible [Alive] -> [Canceled] -> [Terminating] -> [Terminated]
     */
    override val status : LifetimeStatus get() = statusSlice[state]



    private inline infix fun<T> (() -> T).executeIf(check: (Int) -> Boolean) : T? {
        //increase [executing] by 1
        while (true) {
            val s = state.get()
            if (!check(s))
                return null

            if (state.compareAndSet(s, s+1))
                break
        }

        threadLocalExecuting.add(this@LifetimeDefinition, +1)
        try {

            return this()

        } finally {
            threadLocalExecuting.add(this@LifetimeDefinition, -1)
            state.decrementAndGet()
        }
    }


    private inline infix fun<T> (() -> T).underMutexIf(check: (Int) -> Boolean) : T? {
        //increase [executing] by 1
        while (true) {
            val s = state.get()
            if (!check(s))
                return null

            if (mutexSlice[s])
                continue

            if (state.compareAndSet(s, mutexSlice.updated(s, true)))
                break
        }


        try {

            return this()

        } finally {
            while (true) {
                val s = state.get()
                assert(mutexSlice[s])

                if (state.compareAndSet(s, mutexSlice.updated(s, false)))
                    break
            }
        }
    }



    override fun <T : Any> executeIfAlive(action: () -> T) : T? {
        return action executeIf { state -> statusSlice[state] == Alive }
    }

    private fun tryAdd(action: Any) : Boolean {
        //we could add anything to Eternal lifetime and it'll never be executed
        if (lifetime === eternal)
            return true

        return {
            resources.add(action)
            true
        } underMutexIf  {
            statusSlice[it] < Terminating
        } ?: false
    }



    private inline fun incrementStatusIf(check: (Int) -> Boolean) : Boolean {
        assert(this !== eternal) { "Trying to change eternal lifetime" }

        while (true) {
            val s = state.get()
            if (!check(s))
                return false

            val nextStatus = enumValues<LifetimeStatus>()[statusSlice[s].ordinal + 1]
            val newS = statusSlice.updated(s, nextStatus)

            if (state.compareAndSet(s, newS))
                return true
        }
    }


    private fun markCanceledRecursively() : Boolean {
        assert(this !== eternal) { "Trying to terminate eternal lifetime" }

        if (!incrementStatusIf { statusSlice[it] == Alive } )
            return false

        {
            for (i in resources.lastIndex downTo 0) {
                val def = resources[i] as? LifetimeDefinition ?: continue
                def.markCanceledRecursively()
            }
        } underMutexIf {
            statusSlice[it] < Terminating //some parallel thread already destructuring
        } ?: return false

        return true
    }


    fun terminate(supportsTerminationUnderExecuting: Boolean = false) : Boolean {
        if (isEternal)
            return false



        if (threadLocalExecuting[this] > 0 && !supportsTerminationUnderExecuting) {
            error("Can't terminate lifetime under `executeIfAlive` because termination doesn't support this. Use `terminate(true)`")
        }


        markCanceledRecursively()

        //wait for all executions finished
        if (!spinUntil(waitForExecutingInTerminationTimeout) { executingSlice[state] <= threadLocalExecuting[this] }) {
            log.error { "Can't wait for executeIfAlive for more than $waitForExecutingInTerminationTimeout ms. Keep termination." }
        }


        //Already terminated by someone else.
        if (!incrementStatusIf { statusSlice[it] == Canceled })
            return false

        //wait for all resource modification finished
        spinUntil { !mutexSlice[state] }

        destruct(supportsTerminationUnderExecuting)

        return true
    }


    //assumed that we are already in Terminating state
    private fun destruct(supportsRecursion: Boolean) {
        assert (status == Terminating) { "Bad status for destructuring start: $status"}

        for (i in resources.lastIndex downTo 0) {
            val resource = resources[i]
            @Suppress("UNCHECKED_CAST")
            //first comparing to function
            (resource as? () -> Any?)?.let {action -> log.catch { action() }}?:

            when(resource) {
                is Closeable -> log.catch { resource.close() }

                is LifetimeDefinition -> log.catch {
                    resource.terminate(supportsRecursion)
                }

                is ClearLifetimeMarker -> {
                    resource.parentToClear.clearObsoleteAttachedLifetimes()
                }

                else -> log.catch { error("Unknown termination resource: $resource") }
            }

            //we can don't worry about
            resources.removeAt(i)
        }

        require (incrementStatusIf { statusSlice[it] == Terminating }) { "Bad status for destructuring finish: $status" }
    }


    override fun onTerminationIfAlive(action: () -> Unit) = tryAdd(action)
    override fun onTerminationIfAlive(closeable: Closeable) = tryAdd(closeable)



    override fun attach(child: LifetimeDefinition) {
        require(!child.isEternal) { "Can't attach eternal lifetime" }

        if (child.tryAdd(ClearLifetimeMarker(this))) {
            if (!this.tryAdd(child))
                child.terminate()
        }
    }


    override fun<T:Any> bracket(opening: () -> T, terminationAction: () -> Unit) : T? {
        return executeIfAlive {
            val res = opening()

            if(!tryAdd(terminationAction)) {
                //terminated with `terminate(true)`
                terminationAction()
            }
            res
        }
    }

    private fun clearObsoleteAttachedLifetimes() {
        {
            for (i in resources.lastIndex downTo 0) {
                if ((resources[i] as? LifetimeDefinition)?.let { it.status >= Terminating  } == true)
                    resources.removeAt(i)
                else
                    break
            }
        } underMutexIf { state ->
            //no need to clear if lifetime will be cleared soon
            statusSlice[state] == Alive
        }
    }
}

private class ClearLifetimeMarker (val parentToClear: LifetimeDefinition)

fun Lifetime.waitTermination() = spinUntil { status == Terminated }

fun Lifetime.throwIfNotAlive() { if (status != Alive) throw CancellationException() }
fun Lifetime.assertAlive() { assert(status == Alive) { "Not alive: $status" } }

val Lifetime.isAlive : Boolean get() = status == Alive
val Lifetime.isNotAlive : Boolean get() = status != Alive
val Lifetime.isEternal : Boolean get() = this === Lifetime.Eternal


private fun Lifetime.badStatusForAddActions() {
    error ("Lifetime in '$status', can't add termination actions")
}

fun Lifetime.onTermination(action: () -> Unit) {
    if (!onTerminationIfAlive(action))
        badStatusForAddActions()
}

fun Lifetime.onTermination(closeable: Closeable) {
    if (!onTerminationIfAlive(closeable))
        badStatusForAddActions()
}


val EternalLifetime get() = Lifetime.Eternal

operator fun Lifetime.plusAssign(action : () -> Unit) = onTermination(action)

fun Lifetime.intersect(lifetime: Lifetime): LifetimeDefinition {
    return LifetimeDefinition().also {
        this.attach(it)
        lifetime.attach(it)
    }
}


inline fun <T> Lifetime.view(viewable: IViewable<T>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.view(this) { lt, value -> lt.handler(value) }
}

inline fun <T:Any> Lifetime.viewNotNull(viewable: IViewable<T?>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.viewNotNull(this) { lt, value -> lt.handler(value) }
}

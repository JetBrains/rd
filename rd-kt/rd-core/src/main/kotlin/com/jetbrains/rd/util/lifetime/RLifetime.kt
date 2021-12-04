//@file:JvmName("LifetimeKt")

package com.jetbrains.rd.util.lifetime

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.collections.CountingSet
import com.jetbrains.rd.util.lifetime.LifetimeStatus.*
import com.jetbrains.rd.util.reactive.IViewable
import com.jetbrains.rd.util.reactive.viewNotNull
import kotlin.math.min

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
        internal val eternal = LifetimeDefinition().apply { id = "Eternal" }
        private val log : Logger by lazy {getLogger<Lifetime>()}

        //State decomposition
        private val executingSlice = BitSlice.int(20)
        private val statusSlice = BitSlice.enum<LifetimeStatus>(executingSlice)
        private val mutexSlice = BitSlice.bool(statusSlice)

        val Terminated : LifetimeDefinition = LifetimeDefinition().apply { id = "Terminated" }

        const val anonymousLifetimeId = "Anonymous"

        init {
            Terminated.terminate()
        }
    }



    //Fields
    private var state = AtomicInteger()
    private var resources: Array<Any?>? = arrayOfNulls(1)
    private var resCount = 0

    /**
     * Only possible [Alive] -> [Canceled] -> [Terminating] -> [Terminated]
     */
    override val status : LifetimeStatus get() = statusSlice[state]

    /**
     * You can optionally set this identification information to see logs with lifetime's id other than [anonymousLifetimeId]"/>
     */
    var id: Any? = null

    override fun<T : Any> executeIfAlive(action: () -> T): T? {
        //increase [executing] by 1
        while (true) {
            val s = state.get()
            if (statusSlice[s] != Alive)
                return null

            if (state.compareAndSet(s, s+1))
                break
        }

        threadLocalExecuting.add(this@LifetimeDefinition, +1)
        try {

            return action()

        } finally {
            threadLocalExecuting.add(this@LifetimeDefinition, -1)
            state.decrementAndGet()
        }
    }


    private inline fun<T> underMutexIfLessOrEqual(status: LifetimeStatus, action: () -> T): T? {
        //increase [executing] by 1
        while (true) {
            val s = state.get()
            if (statusSlice[s] > status)
                return null

            if (mutexSlice[s])
                continue

            if (state.compareAndSet(s, mutexSlice.updated(s, true)))
                break
        }


        try {

            return action()

        } finally {
            while (true) {
                val s = state.get()
                assert(mutexSlice[s])

                if (state.compareAndSet(s, mutexSlice.updated(s, false)))
                    break
            }
        }
    }

    private fun tryAdd(action: Any): Boolean {
        //we could add anything to Eternal lifetime and it'll never be executed
        if (lifetime === eternal)
            return true

        return underMutexIfLessOrEqual(Canceled) {
            val localResources = resources
            require(localResources != null) { "$this: `resources` can't be null under mutex while status < Terminating" }

            if (resCount == localResources.size) {
                var countAfterCleaning = 0
                for (i in 0 until resCount) {
                    //can't clear Canceled because TryAdd works in Canceled state
                    val resource = localResources[i]
                    if (resource is LifetimeDefinition && resource.status >= Terminating) {
                        localResources[i] = null
                    } else {
                        localResources[countAfterCleaning++] = resource
                    }
                }

                resCount = countAfterCleaning
                if (countAfterCleaning * 2 > localResources.size) {
                    val newArray = arrayOfNulls<Any?>(countAfterCleaning * 2)  //must be more than 1, so it always should be room for one more resource
                    localResources.copyInto(newArray, 0, 0, countAfterCleaning)
                    resources = newArray
                }
            }

            resources!![resCount++] = action
            true
        } ?: false
    }


    private fun incrementStatusIfEqualTo(status: LifetimeStatus): Boolean {
        assert(this !== eternal) { "$this: Trying to change eternal lifetime" }

        while (true) {
            val s = state.get()
            if (statusSlice[s] != status)
                return false

            val nextStatus = enumValues<LifetimeStatus>()[statusSlice[s].ordinal + 1]
            val newS = statusSlice.updated(s, nextStatus)

            if (state.compareAndSet(s, newS))
                return true
        }
    }


    private fun markCanceledRecursively() {
        assert(this !== eternal) { "$this: Trying to terminate eternal lifetime" }

        if (!incrementStatusIfEqualTo(Alive))
            return

        // in fact here access to resources could be done without mutex because setting cancellation status of children is rather optimization than necessity
        val localResources = resources ?: return

        //Math.min is to ensure that even if some other thread increased myResCount, we don't get IndexOutOfBoundsException
        for (i in min(resCount, localResources.size) - 1 downTo 0) {
            (localResources[i] as? LifetimeDefinition)?.markCanceledRecursively()
        }
    }


    fun terminate(supportsTerminationUnderExecuting: Boolean = false): Boolean {
        if (isEternal || status > Canceled)
            return false



        if (threadLocalExecuting[this] > 0 && !supportsTerminationUnderExecuting) {
            error("$this: Can't terminate lifetime under `executeIfAlive` because termination doesn't support this. Use `terminate(true)`")
        }


        markCanceledRecursively()

        //wait for all executions finished
        if (!spinUntil(waitForExecutingInTerminationTimeout) { executingSlice[state] <= threadLocalExecuting[this] }) {
            log.error { "$this: Can't wait for executeIfAlive for more than $waitForExecutingInTerminationTimeout ms. Keep termination." }
        }


        //Already terminated by someone else.
        if (!incrementStatusIfEqualTo(Canceled))
            return false

        //now status is 'Terminating' and we have to wait for all resource modifications to complete. No mutex acquire is possible beyond this point.
        spinUntil { !mutexSlice[state] }

        destruct(supportsTerminationUnderExecuting)

        return true
    }


    //assumed that we are already in Terminating state
    private fun destruct(supportsRecursion: Boolean) {
        assert(status == Terminating) { "Bad status for destructuring start: $this" }
        assert(!mutexSlice[state]) { "$this: mutex must be released in this point" }
        //no one can take mutex after this point

        val localResources = resources
        require(localResources != null) { "$this: `resources` can't be null on destructuring stage" }

        for (i in resCount - 1 downTo 0) {
            val resource = localResources[i]
            try {
                //first comparing to function
                (resource as? () -> Any?)?.let { action -> action() } ?:

                when (resource) {
                    is Closeable -> resource.close()

                    is LifetimeDefinition -> resource.terminate(supportsRecursion)

                    else -> log.error { "$this: Unknown termination resource: $resource" }
                }
            } catch (e: Throwable) {
                log.error("$this: exception on termination of resource: $resource", e)
            }
        }

        resources = null
        resCount = 0

        require(incrementStatusIfEqualTo(Terminating)) { "Bad status for destructuring finish: $this" }
    }


    override fun onTerminationIfAlive(action: () -> Unit) = tryAdd(action)
    override fun onTerminationIfAlive(closeable: Closeable) = tryAdd(closeable)



    override fun attach(child: LifetimeDefinition) {
        require(!child.isEternal) { "$this: Can't attach eternal lifetime" }

        if (child.isNotAlive)
            return

        if (!this.tryAdd(child))
            child.terminate()
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

    override fun toString() = "Lifetime `${id ?: anonymousLifetimeId}` [${status}, executing=${executingSlice[state]}, resources=$resCount]"
}

fun Lifetime.waitTermination() = spinUntil { status == Terminated }

fun Lifetime.throwIfNotAlive() { if (status != Alive) throw CancellationException() }
fun Lifetime.assertAlive() { assert(status == Alive) { "Not alive: $this" } }

val Lifetime.isAlive : Boolean get() = status == Alive
val Lifetime.isNotAlive : Boolean get() = status != Alive
val Lifetime.isEternal : Boolean get() = this === Lifetime.Eternal


private fun Lifetime.badStatusForAddActions() {
    error ("$this: can't add termination action if lifetime terminating or terminated (Status > Canceled); you can consider usage of `onTerminationIfAlive`")
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

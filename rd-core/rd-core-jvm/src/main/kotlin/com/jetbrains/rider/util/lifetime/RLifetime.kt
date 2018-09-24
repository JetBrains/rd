package com.jetbrains.rider.util.lifetime2

import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.collections.CountingSet
import com.jetbrains.rider.util.lifetime2.RLifetimeStatus.*
import com.jetbrains.rider.util.reflection.threadLocal
import com.jetbrains.rider.util.threading.SpinWait

enum class RLifetimeStatus {
    Alive,
    Canceled,
    Terminating,
    Terminated
}

//fun RLifetime.throwIfNotAlive() { if (status != Alive) throw CancellationException() }
//fun RLifetime.assertAlive() { assert(status == Alive) { "Not alive: $status" } }


sealed class RLifetime() {
    companion object {
        var waitForExecutingInTerminationTimeout = 500L //timeout for waiting executeIfAlive in termination
        val eternal = RLifetimeDef().lifetime //some marker
        internal val threadLocalExecuting : CountingSet<RLifetime> by threadLocal { CountingSet<RLifetime>() }

        inline fun <T> using(block : (RLifetime) -> T) : T{
            val def = RLifetimeDef()
            try {
                return block(def.lifetime)
            } finally {
                def.terminate()
            }
        }
    }

    abstract val status : RLifetimeStatus

    abstract fun <T : Any> executeIfAlive(action: () -> T) : T?

    abstract fun onTerminationIfAlive(action: () -> Unit): Boolean
    abstract fun onTerminationIfAlive(closeable: AutoCloseable): Boolean


    abstract fun <T : Any> bracket(opening: () -> T, terminationAction: () -> Unit): T?
}


class RLifetimeDef : RLifetime() {
    val lifetime: RLifetime get() = this

    companion object {
        private val log = getLogger<RLifetime>()

        //State decomposition
        private val executingSlice = BitSlice.int(20)
        private val statusSlice = BitSlice.enum<RLifetimeStatus>(executingSlice)
        private val mutexSlice = BitSlice.bool(statusSlice)
    }




    //Fields
    private var state = AtomicInteger()
    private var resources = mutableListOf<Any>()

    /**
     * Only possible [Alive] -> [Canceled] -> [Terminating] -> [Terminated]
     */
    override val status : RLifetimeStatus get() = statusSlice[state]



    private inline infix fun<T> (() -> T).executeIf(check: (Int) -> Boolean) : T? {
        //increase [executing] by 1
        while (true) {
            val s = state.get()
            if (!check(s))
                return null

            if (state.compareAndSet(s, s+1))
                break
        }

        threadLocalExecuting.add(this@RLifetimeDef, +1)
        try {

            return this()

        } finally {
            threadLocalExecuting.add(this@RLifetimeDef, -1)
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

            val nextStatus = enumValues<RLifetimeStatus>()[statusSlice[s].ordinal + 1]
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
                val def = resources[i] as? RLifetimeDef ?: continue
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

        markCanceledRecursively()


        if (threadLocalExecuting[this] > 0 && !supportsTerminationUnderExecuting) {
            error("Can't terminate lifetime under `executeIfAlive` because termination doesn't support this. Use `terminate(true)`")
        }

        //wait for all executions finished
        if (!SpinWait.spinUntil(waitForExecutingInTerminationTimeout) { executingSlice[state] <= threadLocalExecuting[this] }) {
            log.error { "Can't wait for executeIfAlive for more than $waitForExecutingInTerminationTimeout ms. Keep termination." }
        }


        //Already terminated by someone else.
        if (!incrementStatusIf { statusSlice[it] == Canceled })
            return false

        //wait for all resource modification finished
        SpinWait.spinUntil { !mutexSlice[state] }

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
                is AutoCloseable -> log.catch { resource.close() }

                is RLifetimeDef -> log.catch {
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
    override fun onTerminationIfAlive(closeable: AutoCloseable) = tryAdd(closeable)



    internal fun attach(child: RLifetimeDef) {
        require(!child.isEternal) { "Can't attach eternal lifetime" }

        if (child.tryAdd(ClearLifetimeMarker(this))) {
            if (!this.tryAdd(child))
                child.terminate()
        }
    }


    override fun<T:Any> bracket(opening: () -> T, terminationAction: () -> Unit) : T? {
        return executeIfAlive {
            val res = opening()
            require(tryAdd(terminationAction))
            res
        }
    }

    private fun clearObsoleteAttachedLifetimes() {
        {
            for (i in resources.lastIndex downTo 0) {
                if ((resources[i] as? RLifetimeDef)?.let { it.status >= Terminating  } == true)
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

private class ClearLifetimeMarker (val parentToClear: RLifetimeDef)

fun RLifetime.waitTermination() {
    SpinWait.spinUntil { status == Terminated }
}

val RLifetime.isAlive : Boolean get() = status == Alive
val RLifetime.isEternal : Boolean get() = this === RLifetime.eternal
fun RLifetime.defineNested() = RLifetimeDef().also { (this as RLifetimeDef).attach(it) }


private fun RLifetime.badStatusForAddActions() {
    error {"Lifetime in '$status', can't add termination actions"}
}

fun RLifetime.onTermination(action: () -> Unit) {
    if (!onTerminationIfAlive(action))
        badStatusForAddActions()
}
fun RLifetime.onTermination(closeable: AutoCloseable) {
    if (!onTerminationIfAlive(closeable))
        badStatusForAddActions()
}



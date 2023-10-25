//@file:JvmName("LifetimeKt")

package com.jetbrains.rd.util.lifetime

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.collections.CountingSet
import com.jetbrains.rd.util.lifetime.LifetimeStatus.*
import com.jetbrains.rd.util.reactive.IViewable
import com.jetbrains.rd.util.reactive.viewNotNull
import com.jetbrains.rd.util.threading.coroutines.RdCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.math.min


@JvmInline
value class Lifetime(internal val definition: LifetimeDefinition) {
    companion object {
        private const val waitForExecutingInTerminationTimeoutMsDefault = 500L
        private val terminationTimeoutMs = arrayOf(250L, 5000L, 30000L)

        @Deprecated("Use waitForExecutingInTerminationTimeoutMs")
        var waitForExecutingInTerminationTimeout
            get() = waitForExecutingInTerminationTimeoutMs
            set(value) {
                waitForExecutingInTerminationTimeoutMs = value
            }

        var waitForExecutingInTerminationTimeoutMs = waitForExecutingInTerminationTimeoutMsDefault //timeout for waiting executeIfAlive in termination


        val Eternal: Lifetime get() = LifetimeDefinition.eternal.lifetime //some marker
        val Terminated: Lifetime get() = LifetimeDefinition.Terminated.lifetime

        inline fun <T> using(block: (Lifetime) -> T): T {
            val def = LifetimeDefinition()
            try {
                return block(def.lifetime)
            } finally {
                def.terminate()
            }
        }


        /**
         * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
         * Created lifetime inherits the smallest [terminationTimeoutKind]
         */
        fun intersect(lifetime1: Lifetime, lifetime2: Lifetime): Lifetime  {
            if (lifetime1.referenceEquals(lifetime2) || lifetime2.isEternal)
                return lifetime1

            if (lifetime1.isEternal)
                return lifetime2

            return defineIntersection(lifetime1, lifetime2).lifetime
        }

        /**
         * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
         * Created lifetime inherits the smallest [terminationTimeoutKind]
         */
        fun intersect(lifetimes: List<Lifetime>): Lifetime = defineIntersection(lifetimes).lifetime

        /**
         * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
         * Created lifetime inherits the smallest [terminationTimeoutKind]
         */
        fun defineIntersection(lifetimes: List<Lifetime>): LifetimeDefinition {
            assert(lifetimes.isNotEmpty()) { "One or more parameters must be passed" }

            return LifetimeDefinition().also { res ->
                var minTimeoutKind = LifetimeTerminationTimeoutKind.maxValue
                lifetimes.forEach { lifetime ->
                    lifetime.definition.attach(res, false)

                    val timeoutKind = lifetime.terminationTimeoutKind
                    if (minTimeoutKind.value > timeoutKind.value)
                        minTimeoutKind = timeoutKind
                }

                res.terminationTimeoutKind = minTimeoutKind
            }
        }

        /**
         * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
         * Created lifetime inherits the smallest [terminationTimeoutKind]
         */
        fun defineIntersection(lifetime1: Lifetime, lifetime2: Lifetime): LifetimeDefinition {
            return LifetimeDefinition().also { res ->
                val timeoutKind1 = lifetime1.terminationTimeoutKind
                val timeoutKind2 = lifetime2.terminationTimeoutKind

                res.terminationTimeoutKind = if (timeoutKind1 > timeoutKind2) timeoutKind2 else timeoutKind1

                lifetime1.attach(res, false)
                lifetime2.attach(res, false)
            }
        }

        /**
         * Gets the actual value in milliseconds for termination timeout kind (short, long, etc).
         *
         * @param [timeoutKind] timeout kind as defined by [LifetimeTerminationTimeoutKind]
         * @return timeout value in milliseconds
         */
        fun getTerminationTimeoutMs(timeoutKind: LifetimeTerminationTimeoutKind): Long =
            if (timeoutKind == LifetimeTerminationTimeoutKind.Default) {
                waitForExecutingInTerminationTimeoutMs
            } else {
                terminationTimeoutMs[timeoutKind.value - 1]
            }

        /**
         * Sets the actual value in milliseconds for termination timeout kind (short, long, etc).
         *
         * @param [timeoutKind] timeout kind as defined by [LifetimeTerminationTimeoutKind]
         * @param [milliseconds] timeout value in milliseconds
         */
        fun setTerminationTimeoutMs(timeoutKind: LifetimeTerminationTimeoutKind, milliseconds: Long) {
            if (timeoutKind == LifetimeTerminationTimeoutKind.Default) {
                waitForExecutingInTerminationTimeoutMs = milliseconds
            } else {
                terminationTimeoutMs[timeoutKind.value - 1] = milliseconds
            }
        }
    }

    @Deprecated("Use lifetime.createNested", ReplaceWith("lifetime.createNested()"))
    fun createNestedDef() = createNested()

    fun createNested() = LifetimeDefinition(this)

    fun createNested(atomicAction : (LifetimeDefinition) -> Unit) = createNested().also { nested ->
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

    val status : LifetimeStatus get() = definition.status
    val terminationTimeoutKind: LifetimeTerminationTimeoutKind get() = definition.terminationTimeoutKind

    val allowTerminationUnderExecution: Boolean get() = definition.allowTerminationUnderExecution
    val coroutineScope: CoroutineScope get() = definition.coroutineScope

    fun <T : Any> executeIfAlive(action: () -> T) : T? = definition.executeIfAlive(action)
    fun <T : Any> executeOrThrow(action: () -> T) : T = definition.executeOrThrow(action)

    fun onTerminationIfAlive(action: () -> Unit): Boolean = definition.onTerminationIfAlive(action)
    fun onTerminationIfAlive(closeable: Closeable): Boolean = definition.onTerminationIfAlive(closeable)

    fun onTermination(action: () -> Unit): Unit = definition.onTermination(action)
    fun onTermination(closeable: Closeable): Unit = definition.onTermination(closeable)

    fun <T : Any> bracketIfAlive(opening: () -> T, terminationAction: () -> Unit): T? = definition.bracketIfAlive(opening, terminationAction)
    fun <T : Any> bracketIfAliveEx(opening: () -> T, terminationAction: (T) -> Unit): T? = definition.bracketIfAliveEx(opening, terminationAction)

    fun <T : Any> bracketOrThrow(opening: () -> T, terminationAction: () -> Unit): T = definition.bracketOrThrow(opening, terminationAction)
    fun <T : Any> bracketOrThrowEx(opening: () -> T, terminationAction: (T) -> Unit): T = definition.bracketOrThrowEx(opening, terminationAction)

    fun attach(child: LifetimeDefinition, inheritTimeoutKind: Boolean): Unit = definition.attach(child, inheritTimeoutKind)
}


class LifetimeDefinition constructor() {
    val lifetime: Lifetime get() = Lifetime(this)

    constructor(parent: Lifetime) : this() {
        parent.attach(this, inheritTimeoutKind = true)
    }

    companion object {
        private val log : Logger by lazy {getLogger<Lifetime>()}

        //State decomposition
        private val executingSlice = BitSlice.int(20)
        private val statusSlice = BitSlice.enum<LifetimeStatus>(executingSlice)
        private val mutexSlice = BitSlice.bool(statusSlice)
        private val logErrorAfterExecution = BitSlice.bool(mutexSlice)
        private val terminationTimeoutKindSlice = BitSlice.enum<LifetimeTerminationTimeoutKind>(logErrorAfterExecution)
        private val allowTerminationUnderExecutionSlice = BitSlice.bool(terminationTimeoutKindSlice)

        private val threadLocalExecutingBackingFiled: ThreadLocal<CountingSet<LifetimeDefinition>> = threadLocalWithInitial { CountingSet() }
        // !!! IMPORTANT !!! Don't use 'by ThreadLocal' to avoid slow reflection initialization
        internal val threadLocalExecuting get() = threadLocalExecutingBackingFiled.get()


        val Terminated : LifetimeDefinition = LifetimeDefinition().apply { id = "Terminated" }
        internal val eternal = LifetimeDefinition().apply { id = "Eternal" }

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
     * Only possible [Alive] -> [Canceling] -> [Terminating] -> [Terminated]
     */
    val status : LifetimeStatus get() = statusSlice[state]

    /**
     * Gets or sets termination timeout kind for the definition.
     *
     * The sub-definitions inherit this value at the moment of creation.
     * The changing of terminationTimeoutKind doesn't affect already created sub-definitions.
     */
    var terminationTimeoutKind: LifetimeTerminationTimeoutKind
        get() = terminationTimeoutKindSlice[state]
        set(value) {
            terminationTimeoutKindSlice.atomicUpdate(state, value)
        }

    var allowTerminationUnderExecution: Boolean
        get() = allowTerminationUnderExecutionSlice[state]
        set(value) {
            allowTerminationUnderExecutionSlice.atomicUpdate(state, value)
        }

    val coroutineScope: CoroutineScope
        get() = tryGetOrCreateAdditionalFields()?.scope ?: RdCoroutineScope.current.cancelledScope

    /**
     * You can optionally set this identification information to see logs with lifetime's id other than [anonymousLifetimeId]"/>
     */
    var id: Any? = null
        get() = field
        set(value)  {
            if (value is Lifetime) {
                Logger.root.error { "Set lifetime as id for another lifetime is not allowed" }
                return
            }

            field = value
            if (status == LifetimeStatus.Terminated)
                field = null
        }

    fun<T : Any> executeIfAlive(action: () -> T): T? {
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

            if (logErrorAfterExecution[state]) {
                val terminationTimeoutMs = Lifetime.getTerminationTimeoutMs(terminationTimeoutKind)
                log.error { "executeIfAlive after termination of $this took too much time (>${terminationTimeoutMs}ms)" }
            }
        }
    }

    fun <T : Any> executeOrThrow(action: () -> T): T {
        return executeIfAlive(action) ?: throw CancellationException()
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

    private fun tryGetAdditionalFields(): AdditionalFields? {
        if (isEternal)
            return EternalAdditionalFields

        val resources = resources ?: return null
        return resources[0] as? AdditionalFields
    }

    private fun tryGetOrCreateAdditionalFields(): AdditionalFields? {
        return tryGetAdditionalFields() ?: run {
            val additionalFields = AdditionalFields(id)
            if (tryAdd(additionalFields)) {
                additionalFields
            } else {
                additionalFields.cancel()
                tryGetAdditionalFields()
            }
        }
    }

    private fun tryAdd(action: Any): Boolean {
        //we could add anything to Eternal lifetime and it'll never be executed
        if (isEternal)
            return true

        return underMutexIfLessOrEqual(Canceling) {
            var localResources = resources
            require(localResources != null) { "$this: `resources` can't be null under mutex while status < Terminating" }

            if (resCount == localResources.size) {
                var countAfterCleaning = 0
                for (i in 0 until resCount) {
                    //can't clear Canceling because TryAdd works in Canceling state
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

            localResources = resources!!

            if (action is AdditionalFields) {
                if (localResources[0] is AdditionalFields)
                    false
                else {
                    System.arraycopy(localResources, 0, localResources, 1, resCount++)
                    localResources[0] = action

                    if (isNotAlive)
                        action.cancel()
                    true
                }
            } else {
                localResources[resCount++] = action
                true
            }
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


    private fun markCancelingRecursively() {
        assert(this !== eternal) { "$this: Trying to terminate eternal lifetime" }

        if (!incrementStatusIfEqualTo(Alive))
            return

        // in fact here access to resources could be done without mutex because setting cancellation status of children is rather optimization than necessity
        val localResources = resources ?: return

        (localResources[0] as? AdditionalFields)?.cancel()

        //Math.min is to ensure that even if some other thread increased myResCount, we don't get IndexOutOfBoundsException
        for (i in min(resCount, localResources.size) - 1 downTo 0) {
            (localResources[i] as? LifetimeDefinition)?.markCancelingRecursively()
        }
    }


    fun terminate(supportsTerminationUnderExecuting: Boolean = false): Boolean {
        if (isEternal || status > Canceling)
            return false



        if (threadLocalExecuting[this] > 0 && !supportsTerminationUnderExecuting && !allowTerminationUnderExecution) {
            error("$this: Can't terminate lifetime under `executeIfAlive` because termination doesn't support this. Use `terminate(true)`")
        }


        markCancelingRecursively()

        //wait for all executions finished
        val terminationTimeoutMs = Lifetime.getTerminationTimeoutMs(terminationTimeoutKind)
        if (!spinUntil(terminationTimeoutMs) { executingSlice[state] <= threadLocalExecuting[this] }) {
            log.warn {
                "$this: can't wait for `executeIfAlive` completed on other thread in $terminationTimeoutMs ms. Keep termination.${System.lineSeparator()}" +
                "This may happen either because of the executeIfAlive failed to complete in a timely manner. In the case there will be following error messages.${System.lineSeparator()}" +
                "This is also possible if the thread waiting for the termination wasn't able to receive execution time during the wait in SpinWait.SpinUntil, so it has missed the fact that the lifetime was terminated in time."
            }

            logErrorAfterExecution.atomicUpdate(state, true)
        }


        //Already terminated by someone else.
        if (!incrementStatusIfEqualTo(Canceling))
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

        (localResources[0] as? AdditionalFields)?.cancel()

        for (i in resCount - 1 downTo 0) {
            val resource = localResources[i]
            try {
                //first comparing to function
                (resource as? () -> Any?)?.let { action -> action() } ?: when (resource) {

                    is Closeable -> resource.close()

                    is LifetimeDefinition -> resource.terminate(supportsRecursion)

                    is AdditionalFields -> resource.cancel()

                    else -> log.error { "$this: Unknown termination resource: $resource" }
                }
            } catch (e: Throwable) {
                log.error("$this: exception on termination of resource: $resource", e)
            }
        }

        resources = null
        resCount = 0

        require(incrementStatusIfEqualTo(Terminating)) { "Bad status for destructuring finish: $this" }
        id = null
    }


    fun onTerminationIfAlive(action: () -> Unit) = tryAdd(action)
    fun onTerminationIfAlive(closeable: Closeable) = tryAdd(closeable)

    fun onTermination(action: () -> Unit) = onTerminationImpl(action)
    fun onTermination(closeable: Closeable) = onTerminationImpl(closeable)

    private fun onTerminationImpl(resource: Any) {
        if (tryAdd(resource)) return

        try {
            //first comparing to function
            (resource as? () -> Any?)?.let { action -> action() } ?: when (resource) {

                is Closeable -> resource.close()

                else -> log.error { "$this: Unknown termination resource: $resource" }
            }
        } catch (e: Throwable) {
            log.error("$this: exception on synchronous execute of action on terminated lifetime: $resource", e)
        }

        badStatusForAddActions()
    }


    fun attach(child: LifetimeDefinition, inheritTimeoutKind: Boolean) {
        require(!child.isEternal) { "$this: Can't attach eternal lifetime" }

        if (child.isNotAlive)
            return

        if (inheritTimeoutKind)
            child.terminationTimeoutKind = terminationTimeoutKind

        if (!this.tryAdd(child))
            child.terminate()
    }


    fun<T:Any> bracketIfAlive(opening: () -> T, terminationAction: () -> Unit) : T? {
        return executeIfAlive {
            val res = opening()

            if(!tryAdd(terminationAction)) {
                //terminated with `terminate(true)`
                terminationAction()
            }
            res
        }
    }

    fun <T : Any> bracketIfAliveEx(opening: () -> T, terminationAction: (T) -> Unit): T? {
        return executeIfAlive {
            val res = opening()

            if(!tryAdd({ terminationAction(res) })) {
                //terminated with `terminate(true)`
                terminationAction(res)
            }
            res
        }
    }

    fun <T : Any> bracketOrThrow(opening: () -> T, terminationAction: () -> Unit): T {
        return bracketIfAlive(opening, terminationAction) ?: throw CancellationException()
    }

    fun <T : Any> bracketOrThrowEx(opening: () -> T, terminationAction: (T) -> Unit): T {
        return bracketIfAliveEx(opening, terminationAction) ?: throw CancellationException()
    }

    override fun toString() = "Lifetime `${id ?: anonymousLifetimeId}` [${status}, executing=${executingSlice[state]}, resources=$resCount]"

    /**
     * Utility class to encapsulate rarely used properties or functionalities
     * without adding them as dedicated fields in the primary object.
     *
     * Instead of bloating the main object with seldom accessed stuff, we create this object on-demand and put it place it in resources[0] for quick access
     *
     * Rider utilizes an extensive number of lifetimes.
     * Even the mere addition of a potentially uninitialized (null) field can lead to a substantial increase in memory consumption, potentially spanning megabytes.
     */
    private open class AdditionalFields(id: Any?) {
        open val scope = RdCoroutineScope.current.createNestedScope(id?.toString())

        fun cancel() {
            scope.cancel()
        }
    }

    private object EternalAdditionalFields : AdditionalFields(null) {
        override val scope: CoroutineScope
            get() = RdCoroutineScope.current
    }
}


fun Lifetime?.referenceEquals(lifetime: Lifetime?) = this?.definition === lifetime?.definition

fun Lifetime.waitTermination(): Unit = definition.waitTermination()

fun Lifetime.throwIfNotAlive(): Unit = definition.throwIfNotAlive()
fun Lifetime.assertAlive(): Unit = definition.assertAlive()

val Lifetime.isAlive : Boolean get() = definition.isAlive
val Lifetime.isNotAlive : Boolean get() = definition.isNotAlive
val Lifetime.isEternal : Boolean get() = definition.isEternal

fun LifetimeDefinition.waitTermination() = spinUntil { status == Terminated }

fun LifetimeDefinition.throwIfNotAlive() { if (status != Alive) throw CancellationException() }
fun LifetimeDefinition.assertAlive() { assert(status == Alive) { "Not alive: $this" } }

val LifetimeDefinition.isAlive : Boolean get() = status == Alive
val LifetimeDefinition.isNotAlive : Boolean get() = status != Alive
val LifetimeDefinition.isEternal : Boolean get() = this === LifetimeDefinition.eternal


private fun LifetimeDefinition.badStatusForAddActions() {
    error ("$this: can't add termination action if lifetime terminating or terminated (Status > Canceling); you can consider usage of `onTerminationIfAlive`")
}


val EternalLifetime get() = Lifetime.Eternal

operator fun Lifetime.plusAssign(action : () -> Unit) = onTermination(action)

/**
 * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
 * Created lifetime inherits the smallest [terminationTimeoutKind]
 */
fun Lifetime.intersect(lifetime: Lifetime): Lifetime = Lifetime.intersect(this, lifetime)
fun Lifetime.defineIntersection(lifetime: Lifetime): LifetimeDefinition = Lifetime.defineIntersection(this, lifetime)

inline fun <T> Lifetime.view(viewable: IViewable<T>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.view(this) { lt, value -> lt.handler(value) }
}

inline fun <T:Any> Lifetime.viewNotNull(viewable: IViewable<T?>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.viewNotNull(this) { lt, value -> lt.handler(value) }
}

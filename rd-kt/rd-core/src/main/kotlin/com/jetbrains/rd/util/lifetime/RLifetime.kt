//@file:JvmName("LifetimeKt")

package com.jetbrains.rd.util.lifetime

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.CancellationException
import com.jetbrains.rd.util.collections.CountingSet
import com.jetbrains.rd.util.lifetime.LifetimeStatus.*
import com.jetbrains.rd.util.reactive.IViewable
import com.jetbrains.rd.util.reactive.viewNotNull
import com.jetbrains.rd.util.threading.coroutines.RdCoroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.min


sealed class Lifetime {
    companion object {
        private val threadLocalExecutingBackingFiled: ThreadLocal<CountingSet<Lifetime>> = threadLocalWithInitial { CountingSet() }
        // !!! IMPORTANT !!! Don't use 'by ThreadLocal' to avoid slow reflection initialization
        internal val threadLocalExecuting get() = threadLocalExecutingBackingFiled.get()

        private const val waitForExecutingInTerminationTimeoutMsDefault = 500L
        private val terminationTimeoutMs = arrayOf(250L, 5000L, 30000L)

        @Deprecated("Use waitForExecutingInTerminationTimeoutMs")
        var waitForExecutingInTerminationTimeout
            get() = waitForExecutingInTerminationTimeoutMs
            set(value) {
                waitForExecutingInTerminationTimeoutMs = value
            }

        var waitForExecutingInTerminationTimeoutMs = waitForExecutingInTerminationTimeoutMsDefault //timeout for waiting executeIfAlive in termination


        val Eternal: Lifetime get() = LifetimeDefinition.eternal //some marker
        val Terminated get() = LifetimeDefinition.Terminated.lifetime

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
        fun intersect(lifetime1: Lifetime, lifetime2: Lifetime): Lifetime = defineIntersection(lifetime1, lifetime2).lifetime

        /**
         * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
         * Created lifetime inherits the smallest [terminationTimeoutKind]
         */
        fun intersect(vararg lifetimes: Lifetime): Lifetime = defineIntersection(*lifetimes).lifetime

        /**
         * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
         * Created lifetime inherits the smallest [terminationTimeoutKind]
         */
        fun defineIntersection(vararg lifetimes: Lifetime): LifetimeDefinition {
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


        @Deprecated("Use lifetime.createNested { def -> ... }")
        fun define(lifetime: Lifetime, atomicAction: (LifetimeDefinition, Lifetime) -> Unit) = lifetime.createNested { atomicAction(it, it) }

        @Deprecated("Use lifetime.createNested", ReplaceWith("lifetime.createNested()"))
        fun create(lifetime: Lifetime): LifetimeDefinition = lifetime.createNested()
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

    abstract val status : LifetimeStatus
    abstract val terminationTimeoutKind: LifetimeTerminationTimeoutKind

    abstract val allowTerminationUnderExecution: Boolean
    abstract val coroutineScope: CoroutineScope

    internal val definition get() = this as LifetimeDefinition

    abstract fun <T : Any> executeIfAlive(action: () -> T) : T?
    abstract fun <T : Any> executeOrThrow(action: () -> T) : T

    abstract fun onTerminationIfAlive(action: () -> Unit): Boolean
    abstract fun onTerminationIfAlive(closeable: Closeable): Boolean
    abstract fun onTerminationIfAlive(terminationAction: ITerminationAction): Boolean

    abstract fun onTermination(action: () -> Unit)
    abstract fun onTermination(closeable: Closeable)
    abstract fun onTermination(terminationAction: ITerminationAction)

    abstract suspend fun awaitTermination()

    @Deprecated("Use bracketIfAlive", ReplaceWith("bracketIfAlive(opening, terminationAction)"))
    fun <T : Any> bracket(opening: () -> T, terminationAction: () -> Unit): T? = bracketIfAlive(opening, terminationAction)

    abstract fun <T : Any> bracketIfAlive(opening: () -> T, terminationAction: () -> Unit): T?
    //todo think of a better name or use only this api (more clear code, but more allocations)
    abstract fun <T : Any> bracketIfAliveEx(opening: () -> T, terminationAction: (T) -> Unit): T?

    abstract fun <T : Any> bracketOrThrow(opening: () -> T, terminationAction: () -> Unit): T
    //todo think of a better name or use only this api (more clear code, but more allocations)
    abstract fun <T : Any> bracketOrThrowEx(opening: () -> T, terminationAction: (T) -> Unit): T

    abstract fun attach(child: LifetimeDefinition, inheritTimeoutKind: Boolean)

    @Deprecated("Use onTermination", ReplaceWith("onTermination(action)"))
    fun add(action: () -> Unit) = onTermination(action)

    @Deprecated("Use isNotAlive", ReplaceWith("isNotAlive"))
    val isTerminated: Boolean get() = isNotAlive

    @Deprecated("Use executeIfAlive(action)", ReplaceWith("executeIfAlive(action)"))
    fun <T : Any> ifAlive(action: () -> T) = executeIfAlive(action)
}


class LifetimeDefinition constructor() : Lifetime(), ICancellableTerminationAction {
    val lifetime: Lifetime get() = this

    constructor(parent: Lifetime) : this() {
        parent.attach(this, inheritTimeoutKind = true)
    }

    companion object {
        private val log : Logger by lazy {getLogger<Lifetime>()}

        //State decomposition
        private val executingSlice = BitSlice.int(20)
        private val statusSlice = BitSlice.enum<LifetimeStatus>(executingSlice)
        private val mutexSlice = BitSlice.bool(statusSlice)
        private val additionalResourcesMutexSlice = BitSlice.bool(mutexSlice)
        private val cancellingFinishedSlice = BitSlice.bool(additionalResourcesMutexSlice)
        private val logErrorAfterExecution = BitSlice.bool(cancellingFinishedSlice)
        private val terminationTimeoutKindSlice = BitSlice.enum<LifetimeTerminationTimeoutKind>(logErrorAfterExecution)
        private val allowTerminationUnderExecutionSlice = BitSlice.bool(terminationTimeoutKindSlice)


        val Terminated : LifetimeDefinition = LifetimeDefinition().apply { id = "Terminated" }
        internal val eternal = LifetimeDefinition().apply { id = "Eternal" }

        const val anonymousLifetimeId = "Anonymous"

        init {
            Terminated.terminate()
        }
    }

    //Fields
    private var state = AtomicInteger()
    @Volatile
    private var resources: Any? = arrayOfNulls<Any?>(1)
    private var terminationResources: Array<Any?>?
        get() {
            val localHolder = resources ?: return null
            return if (localHolder is ResourcesHolder) localHolder.resources
            else localHolder as Array<Any?>
        }
        set(value) {
            val localHolder = resources
            if (localHolder is ResourcesHolder)
                localHolder.resources = value
            else
                resources = value
        }

    private var resCount = 0

    /**
     * Only possible [Alive] -> [Canceling] -> [Terminating] -> [Terminated]
     */
    override val status : LifetimeStatus get() = statusSlice[state]

    /**
     * Gets or sets termination timeout kind for the definition.
     *
     * The sub-definitions inherit this value at the moment of creation.
     * The changing of terminationTimeoutKind doesn't affect already created sub-definitions.
     */
    override var terminationTimeoutKind: LifetimeTerminationTimeoutKind
        get() = terminationTimeoutKindSlice[state]
        set(value) {
            terminationTimeoutKindSlice.atomicUpdate(state, value)
        }

    override var allowTerminationUnderExecution: Boolean
        get() = allowTerminationUnderExecutionSlice[state]
        set(value) {
            allowTerminationUnderExecutionSlice.atomicUpdate(state, value)
        }

    override val coroutineScope: CoroutineScope
        get() = tryGetOrCreateResourcesHolder()?.getOrCreateCoroutineScope(this) ?: RdCoroutineScope.current.cancelledScope

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

            if (logErrorAfterExecution[state]) {
                val terminationTimeoutMs = getTerminationTimeoutMs(terminationTimeoutKind)
                log.error { "executeIfAlive after termination of $this took too much time (>${terminationTimeoutMs}ms)" }
            }

            if (isNotAlive && executingSlice[state] == 0) {
                // lifetime is not alive, so there will be no new execution actions
                tryGetResourcesHolder()?.tryGetTerminationAwaiter()?.updateStatus(TerminationAwaiter.Status.ExecutionsFinished)
            }
        }
    }

    override fun <T : Any> executeOrThrow(action: () -> T): T {
        return executeIfAlive(action) ?: throw CancellationException()
    }


    private inline fun<T> underMutexIfLessOrEqual(status: LifetimeStatus, additionalResources: Boolean = false, action: () -> T): T? {
        //increase [executing] by 1

        val slice = if (additionalResources) {
            require(status <= Terminating) {
                "Status can't be more than Terminating : $status"
            }
            additionalResourcesMutexSlice
        }
        else {
            require(status < Terminating) {
                "Status must be less than Terminating : $status"
            }
            mutexSlice
        }

        while (true) {
            val s = state.get()
            if (statusSlice[s] > status)
                return null

            // only 1 mutex can be taken at a time
            if (mutexSlice[s] || additionalResourcesMutexSlice[s])
                continue

            if (state.compareAndSet(s, slice.updated(s, true)))
                break
        }


        try {

            return action()

        } finally {
            while (true) {
                val s = state.get()
                assert(slice[s])

                if (state.compareAndSet(s, slice.updated(s, false)))
                    break
            }
        }
    }

    private fun tryGetResourcesHolder(): ResourcesHolder? {
        return resources as? ResourcesHolder
    }

    private fun tryGetOrCreateResourcesHolder(): ResourcesHolder? {
        tryGetResourcesHolder()?.let { return it }

        return underMutexIfLessOrEqual(Terminating, true) {
            val localHolder = resources
            requireNotNull(localHolder) {
                "resourcesHolder must not be null at this point"
            }
            if (localHolder is ResourcesHolder)
                return localHolder

            val resources = ResourcesHolder(localHolder as Array<Any?>)
            this.resources = resources
            resources
        }
            // there is a chance that ResourcesHolder has been created at the same time from another thread
            ?: tryGetResourcesHolder()
    }

    private fun tryAdd(action: Any): Boolean {
        //we could add anything to Eternal lifetime and it'll never be executed
        if (lifetime === eternal)
            return true

        val result = underMutexIfLessOrEqual(Canceling) {
            var localResources = terminationResources
            require(localResources != null) { "$this: `resources` can't be null under mutex while status < Terminating" }

            if (resCount == localResources.size) {
                var countAfterCleaning = 0
                for (i in 0 until resCount) {
                    val resource = localResources[i]
                    if (resource is LifetimeDefinition && resource.isFullyTerminated()) {
                        // we have to check isFullyTerminated, because a resource may have some incomplete resources, like CoroutineScope, like CoroutineScope,
                        // and if we clear it here, it will be impossible to await for all resources to complete
                        localResources[i] = null
                    } else {
                        localResources[countAfterCleaning++] = resource
                    }
                }

                resCount = countAfterCleaning
                if (countAfterCleaning * 2 > localResources.size) {
                    val newArray = arrayOfNulls<Any?>(countAfterCleaning * 2)  //must be more than 1, so it always should be room for one more resource
                    localResources.copyInto(newArray, 0, 0, countAfterCleaning)
                    terminationResources = newArray
                }
            }

            localResources = terminationResources!!

            localResources[resCount++] = action
            true
        } ?: false

        if (result && isNotAlive && action is ICancellableTerminationAction) {
            @OptIn(DelicateRdApi::class)
            log.catch { action.markCancelled() }
        }

        return result
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

    @DelicateRdApi
    override fun markCancelled() {
        markCancelingRecursively()
    }

    private fun markCancelingRecursively(): Boolean {
        assert(this !== eternal) { "$this: Trying to terminate eternal lifetime" }

        if (!incrementStatusIfEqualTo(Alive))
            return false

        // in fact here access to resources could be done without mutex because setting cancellation status of children is rather optimization than necessity
        val localResourcesHolder = resources ?: return false
        val localResources = if (localResourcesHolder is ResourcesHolder) {
            localResourcesHolder.tryGetCoroutineScope()?.cancel()
            localResourcesHolder.resources ?: return false
        } else
            localResourcesHolder as Array<Any?>

        //Math.min is to ensure that even if some other thread increased myResCount, we don't get IndexOutOfBoundsException
        for (i in min(resCount, localResources.size) - 1 downTo 0) {
            @OptIn(DelicateRdApi::class)
            log.catch { (localResources[i] as? ICancellableTerminationAction)?.markCancelled() }
        }

        cancellingFinishedSlice.atomicUpdate(state, true)
        tryGetResourcesHolder()?.tryGetTerminationAwaiter()?.updateStatus(TerminationAwaiter.Status.Cancelled)
        return true
    }

    suspend fun terminateSuspending(joinScope: Boolean = true) {
        if (isEternal)
            return

        val resourcesHolder = tryGetOrCreateResourcesHolder() ?: return
        val terminationAwaiter = resourcesHolder.getOrCreateTerminationAwaiter(this@LifetimeDefinition)
        if (!markCancelingRecursively())
            terminationAwaiter.waitForCancelled()

        // cancellation here can lead to undisposed resources
        withContext(NonCancellable) {
            val terminationTimeoutMs = getTerminationTimeoutMs(terminationTimeoutKind)
            withTimeoutOrNull(terminationTimeoutMs) {
                terminationAwaiter.waitForExecutionsFinished(this@LifetimeDefinition)
            } ?: run {
                markAsExecutionIsNotFinishedInTime(terminationTimeoutMs)
                // we are not blocking any thread while we wait for executions to complete, so we can continue to wait for it
                terminationAwaiter.waitForExecutionsFinished(this@LifetimeDefinition)
            }

            if (incrementStatusIfEqualTo(Canceling)) {
                //now status is 'Terminating' and we have to wait for all resource modifications to complete. No mutex acquire is possible beyond this point.
                if (mutexSlice[state]) {
                    withContext(Dispatchers.IO) {
                        spinUntil {
                            yield()
                            !mutexSlice[state]
                        }
                    }
                }

                // destruct on original context
                destruct { it.terminateSuspending(joinScope = false/* do not join scope under NonCancellable context to avoid deadlocks*/) }
            }
            // else someone else terminates lifetime right now
        }

        terminationAwaiter.waitForTerminated()

        if (joinScope)
            terminationAwaiter.waitForFullyTerminated()
    }

    override suspend fun awaitTermination() {
        val resourcesHolder = tryGetOrCreateResourcesHolder() ?: return
        val terminationAwaiter = resourcesHolder.getOrCreateTerminationAwaiter(this@LifetimeDefinition)
        terminationAwaiter.waitForFullyTerminated()
    }

    override fun terminate(): Boolean {
        return terminate(false)
    }

    fun terminate(supportsTerminationUnderExecuting: Boolean): Boolean {
        if (isEternal)
            return false

        if (threadLocalExecuting[this] > 0 && !supportsTerminationUnderExecuting && !allowTerminationUnderExecution) {
            error("$this: Can't terminate lifetime under `executeIfAlive` because termination doesn't support this. Use `terminate(true)`")
        }

        markCancelingRecursively()

        //wait for all executions finished
        val terminationTimeoutMs = getTerminationTimeoutMs(terminationTimeoutKind)
        if (!spinUntil(terminationTimeoutMs) { executingSlice[state] <= threadLocalExecuting[this] }) {
            markAsExecutionIsNotFinishedInTime(terminationTimeoutMs)
        }

        //Already terminated by someone else.
        if (!incrementStatusIfEqualTo(Canceling))
            return false

        //now status is 'Terminating' and we have to wait for all resource modifications to complete. No mutex acquire is possible beyond this point.
        spinUntil { !mutexSlice[state] }

        destruct { it.terminate(supportsTerminationUnderExecuting) }

        return true
    }

    private fun markAsExecutionIsNotFinishedInTime(terminationTimeoutMs: Long) {
        log.warn {
            "$this: can't wait for `executeIfAlive` completed on other thread in $terminationTimeoutMs ms. Keep termination.${System.lineSeparator()}" +
                    "This may happen either because of the executeIfAlive failed to complete in a timely manner. In the case there will be following error messages.${System.lineSeparator()}" +
                    "This is also possible if the thread waiting for the termination wasn't able to receive execution time during the wait in SpinWait.SpinUntil, so it has missed the fact that the lifetime was terminated in time."
        }

        logErrorAfterExecution.atomicUpdate(state, true)
    }

    fun isFullyTerminated(): Boolean {
        return status == LifetimeStatus.Terminated && resources == null
    }


    //assumed that we are already in Terminating state
    private inline fun destruct(onLifetimeDefinition: (LifetimeDefinition) -> Unit) {
        assert(status == Terminating) { "Bad status for destructuring start: $this" }
        assert(!mutexSlice[state]) { "$this: mutex must be released in this point" }
        //no one can take mutex after this point

        val localResources = terminationResources
        require(localResources != null) { "$this: `resources` can't be null on destructuring stage" }

        tryGetResourcesHolder()?.tryGetCoroutineScope()?.cancel()

        var uncompletedLifetimes: MutableList<LifetimeDefinition>? = null

        for (i in resCount - 1 downTo 0) {
            val resource = localResources[i]
            try {
                //first comparing to function
                (resource as? () -> Any?)?.let { action -> action() } ?: when (resource) {

                    is Closeable -> resource.close()

                    is LifetimeDefinition -> {
                        onLifetimeDefinition(resource)

                        if (!resource.isFullyTerminated()) {
                            if (uncompletedLifetimes == null)
                                uncompletedLifetimes = mutableListOf()

                            uncompletedLifetimes.add(resource)
                        }
                    }

                    is ITerminationAction -> {
                        resource.terminate()
                    }

                    else -> log.error { "$this: Unknown termination resource: $resource" }
                }
            } catch (e: Throwable) {
                log.error("$this: exception on termination of resource: $resource", e)
            }
        }

        resCount = 0
        terminationResources = null

        val scope = tryGetResourcesHolder()?.tryGetCoroutineScope()
        if (uncompletedLifetimes != null || scope?.coroutineContext?.job?.isCompleted == false) {
            // there are uncompleted resources, so force ResourceHolder creation before we increase status to Terminate
            val resourcesHolder = tryGetOrCreateResourcesHolder()
            requireNotNull(resourcesHolder)

            val terminationAwaiter = resourcesHolder.getOrCreateTerminationAwaiter(this)
            kotlin.assert(!terminationAwaiter.isTerminated)
        }

        require(incrementStatusIfEqualTo(Terminating)) { "Bad status for destructuring finish: $this" }
        spinUntil { !additionalResourcesMutexSlice[state] }
        finaliseDestruction(uncompletedLifetimes)
    }

    private fun finaliseDestruction(uncompletedLifetimes: MutableList<LifetimeDefinition>?) {
        kotlin.assert(!additionalResourcesMutexSlice[state]) { "additional resource mutex cannot be taken at this point" }

        fun nullResources() {
            resources = null
        }

        val resourcesHolder = tryGetResourcesHolder()
        if (resourcesHolder == null) {
            nullResources()
            return
        }

        val terminationAwaiter = resourcesHolder.tryGetTerminationAwaiter()
        if (terminationAwaiter == null) {
            nullResources()
            return
        }

        terminationAwaiter.updateStatus(TerminationAwaiter.Status.Terminated)

        val scope = resourcesHolder.tryGetCoroutineScope()
        val scopeCompleted = scope?.coroutineContext?.job?.isCompleted ?: true

        if (uncompletedLifetimes != null || !scopeCompleted) {

            // use global scope to ensure this coroutine will not be cancelled
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                uncompletedLifetimes?.forEach {
                    it.awaitTermination()
                }

                scope?.coroutineContext?.job?.join()

            }.invokeOnCompletion {
                terminationAwaiter.updateStatus(TerminationAwaiter.Status.FullyTerminated)
                nullResources()
            }
        } else {
            resourcesHolder.tryGetTerminationAwaiter()?.updateStatus(TerminationAwaiter.Status.FullyTerminated)
            nullResources()
        }
    }

    override fun onTerminationIfAlive(action: () -> Unit) = tryAdd(action)
    override fun onTerminationIfAlive(closeable: Closeable) = tryAdd(closeable)
    override fun onTerminationIfAlive(terminationAction: ITerminationAction) = tryAdd(terminationAction)

    override fun onTermination(action: () -> Unit) = onTerminationImpl(action)
    override fun onTermination(closeable: Closeable) = onTerminationImpl(closeable)
    override fun onTermination(terminationAction: ITerminationAction) = onTerminationImpl(terminationAction)

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


    override fun attach(child: LifetimeDefinition, inheritTimeoutKind: Boolean) {
        require(!child.isEternal) { "$this: Can't attach eternal lifetime" }

        if (child.isNotAlive)
            return

        if (inheritTimeoutKind)
            child.terminationTimeoutKind = terminationTimeoutKind

        if (!this.tryAdd(child))
            child.terminate()
    }


    override fun<T:Any> bracketIfAlive(opening: () -> T, terminationAction: () -> Unit) : T? {
        return executeIfAlive {
            val res = opening()

            if(!tryAdd(terminationAction)) {
                //terminated with `terminate(true)`
                terminationAction()
            }
            res
        }
    }

    override fun <T : Any> bracketIfAliveEx(opening: () -> T, terminationAction: (T) -> Unit): T? {
        return executeIfAlive {
            val res = opening()

            if(!tryAdd({ terminationAction(res) })) {
                //terminated with `terminate(true)`
                terminationAction(res)
            }
            res
        }
    }

    override fun <T : Any> bracketOrThrow(opening: () -> T, terminationAction: () -> Unit): T {
        return bracketIfAlive(opening, terminationAction) ?: throw CancellationException()
    }

    override fun <T : Any> bracketOrThrowEx(opening: () -> T, terminationAction: (T) -> Unit): T {
        return bracketIfAliveEx(opening, terminationAction) ?: throw CancellationException()
    }

    override fun toString() = "Lifetime `${id ?: anonymousLifetimeId}` [${status}, executing=${executingSlice[state]}, resources=$resCount]"

    private class TerminationAwaiter {
        enum class Status {
            Alive,
            Cancelled,
            ExecutionsFinished,
            Terminated,
            FullyTerminated
        }
        private val flow = MutableStateFlow(Status.Alive)

        val isTerminated get() = flow.value == Status.FullyTerminated

        suspend fun waitForCancelled() = waitForStatus(Status.Cancelled)

        suspend fun waitForExecutionsFinished(lifetimeDefinition: LifetimeDefinition) {
            if (lifetimeDefinition.isNotAlive && executingSlice[lifetimeDefinition.state] == 0)
                return

            waitForStatus(Status.ExecutionsFinished)
        }

        suspend fun waitForTerminated() = waitForStatus(Status.Terminated)
        suspend fun waitForFullyTerminated() = waitForStatus(Status.FullyTerminated)

        suspend fun waitForStatus(status: Status) {
            flow.filter { it >= status }.first()
        }

        fun updateStatus(definition: LifetimeDefinition) {
            if (cancellingFinishedSlice[definition.state])
                updateStatus(Status.Cancelled)
            if (definition.isNotAlive && executingSlice[definition.state] == 0)
                updateStatus(Status.ExecutionsFinished)
            if (definition.status == LifetimeStatus.Terminated)
                updateStatus(Status.Terminated)
        }

        fun updateStatus(newStatus: Status) = synchronized(this) {
            if (flow.value >= newStatus) return

            flow.value = newStatus
        }
    }

    private class ResourcesHolder(var resources: Array<Any?>?) {
        companion object {
            private val completedTerminationAwaiter = TerminationAwaiter().apply { updateStatus(TerminationAwaiter.Status.FullyTerminated) }
        }

        @Volatile
        var additionalResources: Any? = null

        private inline fun forEach(action: (Any) -> Unit) {
            val resources = additionalResources
            if (resources is Array<*>) {
                resources.forEach {
                    action(it!!)
                }
            } else if (resources != null) {
                action(resources)
            }
        }

        fun tryGetTerminationAwaiter() = tryGetAdditionalResource<TerminationAwaiter>()
        fun tryGetCoroutineScope() = tryGetAdditionalResource<CoroutineScope>()

        private inline fun <reified T: Any> tryGetAdditionalResource(): T? {
            val resources = additionalResources
            if (resources is T)
                return resources

            if (resources is Array<*>) {
                resources.forEach {
                    if (it is T)
                        return it
                }
            }

            return null
        }

        private inline fun <reified T: Any> getOrCreate(definition: LifetimeDefinition, status: LifetimeStatus, create: () -> T, cancel: (T) -> Unit): T? {
            tryGetAdditionalResource<T>()?.let { return it }

            val result = create()
            val returnedResult = definition.underMutexIfLessOrEqual(status, true) {
                tryGetAdditionalResource<T>()?.let { return@underMutexIfLessOrEqual it }

                val localAdditionalResources = additionalResources
                val newArray = if (localAdditionalResources is Array<*>) {
                    arrayOf(*localAdditionalResources, result)
                } else {
                    arrayOf(localAdditionalResources, result)
                }

                additionalResources = newArray
                result
            } ?: tryGetAdditionalResource<T>()

            if (result !== returnedResult || definition.status > status)
                cancel(result)

            return returnedResult
        }

        fun getOrCreateCoroutineScope(definition: LifetimeDefinition): CoroutineScope = getOrCreate(
            definition,
            Alive,
            { RdCoroutineScope.current.createNestedScope(definition.id?.toString()) },
            { it.cancel() })
            ?: RdCoroutineScope.current.cancelledScope

        fun getOrCreateTerminationAwaiter(definition: LifetimeDefinition): TerminationAwaiter {
            val awaiter = getOrCreate(definition, Terminating, { TerminationAwaiter() },  {
                it.updateStatus(definition)
            }) ?: completedTerminationAwaiter

            awaiter.updateStatus(definition)
            return awaiter
        }
    }
}

fun Lifetime.waitTermination() = spinUntil { status == Terminated }

fun Lifetime.throwIfNotAlive() { if (status != Alive) throw CancellationException() }
fun Lifetime.assertAlive() { assert(status == Alive) { "Not alive: $this" } }

val Lifetime.isAlive : Boolean get() = status == Alive
val Lifetime.isNotAlive : Boolean get() = status != Alive
val Lifetime.isEternal : Boolean get() = this === Lifetime.Eternal


private fun Lifetime.badStatusForAddActions() {
    error ("$this: can't add termination action if lifetime terminating or terminated (Status > Canceling); you can consider usage of `onTerminationIfAlive`")
}

@Deprecated("Use the native implementation", ReplaceWith("onTermination(action)"))
fun Lifetime.onTermination(action: () -> Unit) {
    onTermination(action)
}

@Deprecated("Use the native implementation", ReplaceWith("onTermination(closeable)"))
fun Lifetime.onTermination(closeable: Closeable) {
    onTermination(closeable)
}


val EternalLifetime get() = Lifetime.Eternal

operator fun Lifetime.plusAssign(action : () -> Unit) = onTermination(action)

/**
 * Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
 * Created lifetime inherits the smallest [terminationTimeoutKind]
 */
fun Lifetime.intersect(lifetime: Lifetime): LifetimeDefinition = Lifetime.defineIntersection(this, lifetime)

inline fun <T> Lifetime.view(viewable: IViewable<T>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.view(this) { lt, value -> lt.handler(value) }
}

inline fun <T:Any> Lifetime.viewNotNull(viewable: IViewable<T?>, crossinline handler: Lifetime.(T) -> Unit) {
    viewable.viewNotNull(this) { lt, value -> lt.handler(value) }
}
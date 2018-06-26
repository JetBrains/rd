//package com.jetbrains.rider.util.lifetime
//
//import com.jetbrains.rider.util.catch
//import com.jetbrains.rider.util.getLogger
//import com.jetbrains.rider.util.lifetime2.RLifetimeStatus.*
//import com.jetbrains.rider.util.threading.SpinWait
//import com.jetbrains.rider.util.threading.Sync
//
//enum class RLifetimeStatus {
//    Alive,
//    Terminating,
//    Terminated
//}
//
////fun RLifetime.throwIfNotAlive() { if (status != Alive) throw CancellationException() }
////fun RLifetime.assertAlive() { assert(status == Alive) { "Not alive: $status" } }
//
//
//sealed class RLifetime() {
//    companion object {
//        //some marker
//        val Eternal = RLifetimeDef().lifetime
//    }
//
//    abstract val status : RLifetimeStatus
//    abstract fun <T : Any> executeIfAlive(action: () -> T) : T?
//
//    abstract fun addTerminationAction(action: () -> Unit) : Boolean
//    abstract fun addTerminationObject(closeable: AutoCloseable) : Boolean
//
//    abstract fun attach(def: RLifetimeDef)
//}
//
//class RLifetimeDef : RLifetime() {
//    companion object {
//        val log = getLogger<RLifetime>()
//    }
//
//    /**
//     * Only possible [Alive] -> [Terminating] -> [Terminated]
//     */
//    //todo express [status] and [executing] through single atomic field
//    override var status : RLifetimeStatus = Alive
//        private set
//
//    val lifetime: RLifetime get() = this
//
//    private val resources = mutableListOf<Any>()
//
//    private var executing = 0;
//    override fun <T : Any> executeIfAlive(action: () -> T) : T? {
//        Sync.lock(this) {
//            if (status > Alive) return null
//            executing ++
//        }
//
//        try {
//            return action()
//        } finally {
//            Sync.lock(this) {
//                if (--executing == 0) Sync.notify(this);
//            }
//
//            //todo maybe we could make termination helper here to suppress termination waiting
//        }
//    }
//
//    override fun addTerminationAction(action: () -> Unit) = add0(action)
//    override fun addTerminationObject(closeable: AutoCloseable) = add0(closeable)
//
//    override fun attach(def: RLifetimeDef) {
//        if (!def.addTerminationAction { this.clearTail() })
//            return
//
//        if (!this.add0(def))
//            def.terminate()
//    }
//
//    private fun add0(action: Any): Boolean {
//        //we could add anything to Eternal lifetime and it'll never be executed
//        if (lifetime === Eternal)
//            return true
//
//        Sync.lock (this) {
//            if (status > Alive)
//                return false
//            resources.add(action)
//            return true
//        }
//    }
//
//    fun bracket(opening: () -> Unit, terminationAction: () -> Unit) {
//
//    }
//
//    //do not need to wait
//    fun terminate() : Boolean {
//        if (!markTerminatingRecursively())
//        //somebody else is in charge of termination
//            return false
//
//        Sync.lock(this) {
//            while (executing > 0) Sync.wait(this)
//            //todo or use helper from executing threads
//        }
//
//        assert (!isAlive) {status}
//        //resources are not guarded by lock because nobody can add anything into them if sta
//        for (i in resources.lastIndex downTo 0) {
//            val resource = resources[i]
//            @Suppress("UNCHECKED_CAST")
//            //first comparing to function
//            (resource as? () -> Unit)?.let {action -> log.catch { action() }}?:
//
//            when(resource) {
//
//                is AutoCloseable -> log.catch { resource.close() }
//
//                is RLifetimeDef -> log.catch {
//                    resource.terminate()
//                    //todo clear parent's tail
//                }
//
//                else -> log.catch { error("Unknown termination resource: $resource") }
//            }
//            resources.removeAt(i)
//        }
//
//        Sync.lock(this) {
//            status = Terminated
//        }
//        return true
//    }
//
//    internal fun clearTail() {
//        Sync.lock(this) {
//            //            resources.lis
//        }
//    }
//
//    private fun markTerminatingRecursively() : Boolean {
//        assert(this !== RLifetime.Eternal) { "Trying to terminate eternal lifetime" }
//
//        if (status > Alive) return false //double checked locking
//
//        Sync.lock (this) {
//            if (status > Alive) return false
//            status = Terminating
//        }
//
//        for (i in resources.lastIndex downTo 0) {
//            val def = resources[i] as? RLifetimeDef ?: continue
//            def.markTerminatingRecursively()
//        }
//
//        return true
//    }
//}
//
//fun RLifetime.waitTermination() {
//    SpinWait.spinUntil { status == Terminated }
//}
//
//val RLifetime.isAlive : Boolean get() = status == Alive

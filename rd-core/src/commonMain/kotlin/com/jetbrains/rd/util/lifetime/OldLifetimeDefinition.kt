//package com.jetbrains.rider.util.lifetime
//
//import com.jetbrains.rider.util.concurrentMapOf
//
////allows multiple termination
//class LifetimeDefinition internal constructor(val isEternal: Boolean = false) {
//    val lifetime: Lifetime = Lifetime(isEternal)
//    fun terminate() = lifetime.terminate()
//    val isTerminated: Boolean get() = lifetime.isTerminated
//
//    companion object {
//        val Eternal: LifetimeDefinition = LifetimeDefinition(true)
//
//        @Deprecated("Don't use this API, consider Lifetime.intersect instead")
//        fun synchronize(vararg defs: LifetimeDefinition) {
//            for (a in defs) {
//                for (b in defs) {
//                    val bb = b
//                    if (bb != a) a.lifetime += { bb.terminate() }
//                }
//            }
//
////            val x = concurrentMapOf<Int, Int>()
////             x.get
//        }
//    }
//}
//package com.jetbrains.rider.util.lifetime
//
//import com.jetbrains.rider.util.forEachReversed
//
//class Lifetime constructor(val isEternal: Boolean = false) {
//    companion object {
//        val Eternal: Lifetime = LifetimeDefinition.Eternal.lifetime
//
//        inline fun define(lifetime: Lifetime, f: (LifetimeDefinition, Lifetime) -> Unit): LifetimeDefinition {
//            val nested = create(lifetime)
//            try {
//                f(nested, nested.lifetime)
//            } catch(e: Throwable) {
//                nested.terminate()
//                throw e
//            }
//            return nested
//        }
//
//        fun create(parent : Lifetime) : LifetimeDefinition {
//            val res = LifetimeDefinition()
//            parent.attachNested(res)
//            return res
//        }
//
//        inline fun <T> using(block : (Lifetime) -> T) : T{
//            val def = create(Eternal)
//            try {
//                return block(def.lifetime)
//            } finally {
//                def.terminate()
//            }
//        }
//    }
//
//    var isTerminated : Boolean = false
//        private set
//
//    private val actions = arrayListOf<()->Unit>()
//
//
//    inline fun bracket(opening : () -> Unit, noinline closing: () -> Unit) {
//        if (isTerminated) return
//        opening()
//        add(closing)
//    }
//
//    fun add(action : () -> Unit) {
//        if (isEternal) return
//        if (isTerminated) throw IllegalStateException("Already terminated")
//        synchronized(this) { actions.add (action) }
//    }
//
//    fun createNestedDef() : LifetimeDefinition = Lifetime.create(this)
//    fun createNested() : Lifetime = if (isEternal) this else createNestedDef().lifetime
//
//    //short-living lifetimes could explode action termination queue, so we need to drop them after termination
//    internal fun attachNested(nestedDef: LifetimeDefinition) {
//        if (nestedDef.lifetime.isTerminated || this.isEternal) return
//
//        val action = { nestedDef.terminate()}
//        synchronized(this) {this.add(action)}
//        nestedDef.lifetime.add { actions.remove(action) }
//    }
//
//    internal fun terminate() {
//        if (isEternal) return
//
//        isTerminated = true
//        val actionsCopy = synchronized(this) { val res = actions.toTypedArray(); actions.clear() ; res }
//        actionsCopy.forEachReversed { it() }
//    }
//}
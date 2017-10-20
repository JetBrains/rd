package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.IProtocol
import com.jetbrains.rider.framework.IRdDynamic
import com.jetbrains.rider.framework.SerializationCtx
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.Signal


abstract class RdBindableBase : IRdBindable {

    //fields
    protected var parent : IRdDynamic? = null
    protected open var name: String = "<<not bound yet>>"

    private fun <T> nb() : T = throw IllegalStateException("Not bound: $name")

    //calculable properties
    override val protocol : IProtocol get() = parent?.protocol?: nb()
    val wire get() = protocol.wire

    protected val isBound : Boolean  get() = parent != null

    override val serializationContext: SerializationCtx
        get() = parent?.serializationContext ?: throw IllegalStateException("Trying to get serialization context of unbound object $name")

    private var locationBeforeUnbind : String? = null

    final override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        require (this.parent == null) { throw IllegalStateException("Already bound to `${location()}") }

        this.parent = parent
        this.name = parent.let { if (it is RdBindableBase) it.name + "." + name else name}

        val a = ArrayList<String>()

        Signal.priorityAdviseSection {
            init(lf)
        }

        lf.add {
            //this.name = "<<already unbound : ${location()}>>"
            this.parent = null
        }

    }

    //need to implement in subclasses
    protected abstract fun init(lifetime : Lifetime)

    fun location() : String = name
}


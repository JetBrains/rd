package com.jetbrains.rider.framework.base

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.reactive.Signal
import com.jetbrains.rider.util.string.IPrintable
import java.util.concurrent.ConcurrentHashMap


abstract class RdBindableBase : IRdBindable, IPrintable {

    //fields
    protected var parent : IRdDynamic? = null
    private var _name: String = "<<not bound yet>>"
    override val name: String get() = _name
    protected val bindableChildren = mutableListOf<Pair<String, Any?>>()

    override var rdid: RdId = RdId.Null
        internal set

    private var bindLifetime: Lifetime? = null

    val containingExt: RdExtBase?
        get() {
            var cur: IRdDynamic? = this
            while (cur is RdBindableBase) {
                if (cur is RdExtBase) return cur
                cur = cur.parent
            }
            return null
        }

    private fun <T> nb() : T = throw IllegalStateException("Not bound: $name")

    //calculable properties
    override val protocol : IProtocol get() = parent?.protocol?: nb()
    val wire get() = protocol.wire

    internal val isBound : Boolean  get() = parent != null

    override val serializationContext: SerializationCtx
        get() = parent?.serializationContext ?: throw IllegalStateException("Trying to get serialization context of unbound object $name")



    final override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        require (this.parent == null) { "Already bound to `${location()}`: " }
//        require (!rdid.isNull) { "Must be identified first" }

        lf.bracket(
            {
                this.parent = parent
                _name = parent.let { if (it is RdBindableBase) it.name + "." + name else name}
                bindLifetime = lf
            },
            {
                bindLifetime = lf
                this.parent = null
                rdid = RdId.Null
            }
        )

        protocol.scheduler.assertThread()

        Signal.priorityAdviseSection {
            init(lf)
        }
    }

    private val extensions = ConcurrentHashMap<String, Any>()

    inline fun <reified T: Any> getOrCreateExtension(name: String, noinline create: () -> T) = getOrCreateExtension(name, T::class.java, create)

    fun <T:Any> getOrCreateExtension(name: String, clazz: Class<T>, create: () -> T) : T {
        val res = extensions.getOrPut(name) {
            val newExtension = create()
            if (newExtension is IRdBindable) {
                bindableChildren.add(name to newExtension)
                bindLifetime?.let {
                    newExtension.identify(protocol.identity, rdid.mix("." + name))
                    newExtension.bind(it, this, name)
                }
            }

            newExtension
        }
        @Suppress("UNCHECKED_CAST")
        return res as? T ?: throw error("Wrong class found in extension ${location()}.$name : Expected ${clazz.name} but found ${res.javaClass.name}. Maybe you already set this extension with another type?")
    }

    //need to implement in subclasses
    protected open fun init(lifetime : Lifetime) {
        for ((name, child) in bindableChildren) {
            child?.bindPolymorphic(lifetime, this, name)
        }
    }

    override fun identify(identities: IIdentities, id: RdId) {
        require(rdid.isNull) { "Already has RdId: $rdid" }
        require(!id.isNull) { "Assigned RdId mustn't be null" }

        rdid = id
        for ((name, child) in bindableChildren) {
            child?.identifyPolymorphic(identities, id.mix(".$name"))
        }
    }

    fun location() : String = name
}

fun <T : RdBindableBase> T.withId(id: RdId) : T {
    require(this.rdid == RdId.Null) {"this.id != RdId.Null, but ${this.rdid}"}
    require(id != RdId.Null) {"id != RdId.Null"}

    return this.apply { this.rdid = id }
}

fun <T : RdBindableBase> T.static(id: Int) : T {
    require(id > 0 && id < RdId.MAX_STATIC_ID) { "Expected id > 0 && id < RdId.MaxStaticId, got $id" }
    return withId(RdId(id.toLong()))
}

fun <T : RdBindableBase> T.withIdFromName(name: String) : T {
    return withId(RdId.Null.mix(name))
}


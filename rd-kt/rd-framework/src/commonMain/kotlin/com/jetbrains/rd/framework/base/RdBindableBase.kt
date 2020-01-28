package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.concurrentMapOf
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.RName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


abstract class RdBindableBase : IRdBindable, IPrintable {

    //bound state: main
    override var rdid: RdId = RdId.Null
        internal set

    final override var location: RName = RName("<<not bound>>")
        private set

    protected var parent : IRdDynamic? = null

    private var bindLifetime: Lifetime? = null


    //bound state: inferred

    val isBound : Boolean  get() = parent != null

    override val protocol : IProtocol get() = parent?.protocol?: nb()

    protected val bindableChildren : MutableList<Pair<String, Any?>> = ViewableList<Pair<String, Any?>>()

    override val serializationContext: SerializationCtx get() = parent?.serializationContext ?: nb()

    val containingExt: RdExtBase?
        get() {
            var cur: IRdDynamic? = this
            while (cur is RdBindableBase) {
                if (cur is RdExtBase) return cur
                cur = cur.parent
            }
            return null
        }

    private fun <T> nb() : T = throw IllegalStateException("Not bound: $location")





    final override fun bind(lf: Lifetime, parent: IRdDynamic, name: String) {
        require (this.parent == null) { "Trying to bound already bound $this to ${parent.location}" }
//todo uncomment it
//        require (!rdid.isNull) { "Must be identified first" }

        lf.bracket(
            {
                this.parent = parent
                location = parent.location.sub(name, ".")
                bindLifetime = lf
            },
            {
                bindLifetime = lf
                location = location.sub("<<unbound>>","::")
                this.parent = null
                rdid = RdId.Null
            }
        )

        protocol.scheduler.assertThread(this)

        Signal.priorityAdviseSection {
            init(lf)
        }
    }

    private val extensions = mutableMapOf<String, Any>()

    inline fun <reified T: Any> getOrCreateExtension(name: String, noinline create: () -> T) = getOrCreateExtension(name, T::class, create)
    internal inline fun <reified T: Any> getOrCreateHighPriorityExtension(name: String, noinline create: () -> T) = getOrCreateHighPriorityExtension(name, T::class, create)

    fun <T:Any> getOrCreateExtension(name: String, clazz: KClass<T>, create: () -> T) : T = getOrCreateExtension0(name, clazz, false, create)
    internal fun <T:Any> getOrCreateHighPriorityExtension(name: String, clazz: KClass<T>, create: () -> T) : T = getOrCreateExtension0(name, clazz, true, create)

    private fun <T:Any> getOrCreateExtension0(name: String, clazz: KClass<T>, highPriorityExtension: Boolean = false, create: () -> T) : T {
        Sync.lock(extensions) {
            val res = extensions.getOrPut(name) {
                val newExtension = create()
                if (newExtension is IRdBindable) {
                    bindableChildren.add(if (highPriorityExtension) 0 else bindableChildren.size, name to newExtension)
                    bindLifetime?.let {
                        newExtension.identify(protocol.identity, rdid.mix(".$name"))
                        newExtension.bind(it, this, name)
                    }
                }

                newExtension
            }
            @Suppress("UNCHECKED_CAST")
            return res as? T
                ?: throw error("Wrong class found in extension `$location.$name` : Expected `${clazz.simpleName}` but found `${res::class.simpleName}`. Maybe you already set this extension with another type?")
        }
    }

    //need to implement in subclasses
    protected open fun init(lifetime : Lifetime) {
        for ((name, child) in bindableChildren) {
            child?.bindPolymorphic(lifetime, this, name)
        }
    }

    override fun identify(identities: IIdentities, id: RdId) {
        require(rdid.isNull) { "Already has RdId: $rdid, entity: $this" }
        require(!id.isNull) { "Assigned RdId mustn't be null, entity: $this" }

        rdid = id
        for ((name, child) in bindableChildren) {
            child?.identifyPolymorphic(identities, id.mix(".$name"))
        }
    }

    override fun print(printer: PrettyPrinter) {
        printer.print(toString())
        printer.print(" (")
        printer.print(rdid.toString())
        printer.print(")")
    }

    override fun toString(): String {
        return this::class.simpleName +": `$location`"
    }

    //Reflection
    private fun <T> T.appendToBindableChildren(thisRef: Any?, property: KProperty<*>) : T {
        val self = thisRef as RdBindableBase
        self.bindableChildren.add(property.name to this)
        return this
    }

    operator fun <T : IRdBindable?> T.getValue(thisRef: Any?, property: KProperty<*>) : T = appendToBindableChildren(thisRef, property)
    operator fun <T : List<IRdBindable?>> T.getValue(thisRef: Any?, property: KProperty<*>) : T = appendToBindableChildren(thisRef, property)


    fun synchronizeWith(lifetime: Lifetime, otherBindable: RdBindableBase, accepts: (Any?) -> Boolean = { true }) {
        require (otherBindable::class == this::class) { "Can't synchronize ${this::class} with ${otherBindable::class}" }

        //todo so the trick is that exts can appear in different order and sometimes
        val alreadySynchronized = hashSetOf<String>()


        fun doOneWay(lifetime: Lifetime, me: RdBindableBase, counterpart: RdBindableBase) {
            (me.bindableChildren as ViewableList).adviseAddRemove(lifetime) {addRemove, idx, (name, value) ->
                require (addRemove == AddRemove.Add) {"No delete events for bindableChildren are permitted: ${this}"}
                if (value == null)
                    return@adviseAddRemove

                if (!alreadySynchronized.add(name)) //already synchronized
                    return@adviseAddRemove

                if (value is InternRoot<*>)
                    return@adviseAddRemove

                val other = counterpart.bindableChildren.getOrNull(idx) //by index must be faster
                    ?.takeIf { it.first == name } ?.second //value by index has the same name. Class will be checked when we try to synchronize
                    ?: counterpart.bindableChildren.firstOrNull { it.first == name }?.second // try searching by name in case bindable child lists are different (i.e. extern root is present)
                    ?: counterpart.getOrCreateExtension(name) { value.deepClonePolymorphic() }

                ModelSynchronizer(accepts).synchronizePolymorphic(lifetime, value, other)
            }
        }

        doOneWay(lifetime, this, otherBindable)
        doOneWay(lifetime, otherBindable, this)
    }
}

fun <T : RdBindableBase> T.withId(id: RdId) : T {
    require(this.rdid == RdId.Null) { "this.id != RdId.Null, but ${this.rdid}" }
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


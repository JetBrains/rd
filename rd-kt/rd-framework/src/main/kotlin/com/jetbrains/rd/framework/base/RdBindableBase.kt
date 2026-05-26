package com.jetbrains.rd.framework.base

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.util.Sync
import com.jetbrains.rd.util.collections.SynchronizedList
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.reactive.AddRemove
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rd.util.reactive.ViewableList
import com.jetbrains.rd.util.string.IPrintable
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.RName
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


abstract class RdBindableBase : IRdBindable, IPrintable {

    //bound state: main
    override var rdid: RdId = RdId.Null
        internal set

    final override var location: RName = RName("<<not bound>>")
        private set

    var parent: IRdDynamic? = null
        protected set

    private var bindLifetime: Lifetime = Lifetime.Terminated
        private set

    //bound state: inferred

    var bindState = BindState.NotBound
        private set

    val isBound : Boolean  get() = bindState == BindState.Bound

    override val protocol : IProtocol? get() = parent?.protocol

    protected val bindableChildren : MutableList<Pair<String, Any?>> = ViewableList(SynchronizedList())

    override val serializationContext: SerializationCtx? get() = parent?.serializationContext

    val containingExt: RdExtBase?
        get() {
            var cur: IRdDynamic? = this
            while (cur is RdBindableBase) {
                if (cur is RdExtBase) return cur
                cur = cur.parent
            }
            return null
        }

    private fun <T> nb() : T = throw ProtocolNotBoundException(location.toString())



    final override fun preBind(lf: Lifetime, parent: IRdDynamic, name: String) {
        require (this.parent == null) { "Trying to bound already bound $this to ${parent.location}" }
//todo uncomment it
//        require (!rdid.isNull) { "Must be identified first" }

        val proto = parent.protocol ?: return

        lf.bracketIfAlive(
            {
                this.parent = parent
                location = parent.location.sub(name, ".")
                bindLifetime = lf

                assertBindingThread()

                if (proto is Protocol && !rdid.isNull)
                    proto.rdEntitiesRegistrar.register(bindLifetime, rdid, this)

                Signal.priorityAdviseSection {
                    preInit(lf, proto)
                }

                bindState = BindState.PreBound
            },
            {
                unbind()

                location = location.sub("<<unbound>>","::")
                rdid = RdId.Null
                bindState = BindState.NotBound
                this.parent = null
            }
        ) ?: return
    }

    override fun bind() {
        assertBindingThread()

        val bindLifetime = bindLifetime
        val proto = protocol ?: return
        val ctx = serializationContext ?: return

        val bindState = bindState
        bindLifetime.executeIfAlive {
            assert(bindState == BindState.PreBound)

            Signal.priorityAdviseSection {
                init(bindLifetime, proto, ctx)
            }

            this@RdBindableBase.bindState = BindState.Bound
        }
    }

    private val extensionPerNameLocks = ConcurrentHashMap<String, Any>()
    private val extensions = ConcurrentHashMap<String, Any>()

    inline fun <reified T: Any> getOrCreateExtension(name: String, noinline create: () -> T) = getOrCreateExtension(name, T::class, create)
    internal inline fun <reified T: Any> getOrCreateHighPriorityExtension(name: String, noinline create: () -> T) = getOrCreateHighPriorityExtension(name, T::class, create)

    fun <T:Any> getOrCreateExtension(name: String, clazz: KClass<T>, create: () -> T) : T = getOrCreateExtension0(name, clazz, false, create)
    internal fun <T:Any> getOrCreateHighPriorityExtension(name: String, clazz: KClass<T>, create: () -> T) : T = getOrCreateExtension0(name, clazz, true, create)

    private fun <T:Any> getOrCreateExtension0(name: String, clazz: KClass<T>, highPriorityExtension: Boolean = false, create: () -> T) : T {
        val lock = extensionPerNameLocks.getOrPut(name) { Any() }
        Sync.lock(lock) {
            val res = extensions[name] ?: run {
                val newExtension = create()
                extensions[name] = newExtension
                if (newExtension is IRdBindable) {
                    val pair = name to newExtension
                    if (highPriorityExtension) bindableChildren.add(0, pair)
                    else bindableChildren.add(pair)
                    val proto = protocol ?: return newExtension

                    val localBindLifetime = bindLifetime
                    if (localBindLifetime.isAlive) {
                        if (newExtension.rdid == RdId.Null)
                            newExtension.identify(proto.identity, proto.identity.mix(rdid, ".$name"))
                        newExtension.preBind(localBindLifetime, this, name)
                        newExtension.bind()
                    }
                }

                newExtension
            }
            @Suppress("UNCHECKED_CAST")
            return res as? T
                ?: throw error("Wrong class found in extension `$location.$name` : Expected `${clazz.simpleName}` but found `${res::class.simpleName}`. Maybe you already set this extension with another type?")
        }
    }

    protected open fun assertBindingThread() {
        if (AllowBindingCookie.isBindNotAllowed) {
            val proto = protocol ?: return
            if (proto.lifetime.isNotAlive)
                return

            proto.scheduler.assertThread(this)
        }
    }

    //need to implement in subclasses
    protected open fun preInit(lifetime : Lifetime, proto: IProtocol) {
        preInitBindableFields(lifetime)
    }

    //need to implement in subclasses
    protected open fun init(lifetime: Lifetime, proto: IProtocol, ctx: SerializationCtx) {
        initBindableFields(lifetime)
    }

    protected open fun preInitBindableFields(lifetime: Lifetime) {
        for ((name, child) in bindableChildren) {
            child?.preBindPolymorphic(lifetime, this, name)
        }
    }

    protected open fun initBindableFields(lifetime: Lifetime) {
        for ((_, child) in bindableChildren) {
            child?.bindPolymorphic()
        }
    }

    protected open fun unbind() {

    }

    open fun findByRName(rName: RName): RdBindableBase? {
        if (rName == RName.Empty) return this
        val rootName = rName.getNonEmptyRoot()
        val child = bindableChildren
            .asSequence()
            .map { it.second }
            .filterIsInstance<RdBindableBase>()
            .find { it.location.separator == rootName.separator &&
                    it.location.localName == rootName.localName }
            ?: return null
        
        if (rootName == rName)
            return child
        
        return child.findByRName(rName.dropNonEmptyRoot())
    }
    
    override fun identify(identities: IIdentities, id: RdId) {
        require(rdid.isNull) { "Already has RdId: $rdid, entity: $this" }
        require(!id.isNull) { "Assigned RdId mustn't be null, entity: $this" }

        rdid = id
        for ((name, child) in bindableChildren) {
            child?.identifyPolymorphic(identities, identities.mix(id, ".$name"))
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
        val alreadySynchronized = ConcurrentHashMap<String, Unit>().keySet(Unit)


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

enum class BindState {
    NotBound,
    PreBound,
    Bound
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


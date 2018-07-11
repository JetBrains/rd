package com.jetbrains.rider.generator.nova

import com.jetbrains.rider.util.hash.IncrementalHash64
import com.jetbrains.rider.util.string.condstr
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible


//general
interface IType {
    val name : String
}

interface IBindable : IType
interface IScalar   : IType

//other hierarchy
interface INonNullable : IType

interface INonNullableScalar : INonNullable, IScalar
interface INonNullableBindable : INonNullable, IBindable

interface IHasItemType : IType {
    val itemType: IType
}

interface IArray : IHasItemType {
    override val name : String get() = itemType.name + "Array"
}
data class ArrayOfScalars(override val itemType : IScalar) : INonNullableScalar, IArray
data class ArrayOfBindables(override val itemType : IBindable) : INonNullableBindable, IArray

interface IImmutableList : IHasItemType {          
    override val name : String get() = itemType.name + "List"
}
data class ImmutableListOfScalars(override val itemType : IScalar) : INonNullableScalar, IImmutableList
data class ImmutableListOfBindables(override val itemType : IBindable) : INonNullableBindable, IImmutableList


interface INullable : IHasItemType {
    override val itemType: INonNullable
    override val name : String get() = itemType.name + "Nullable"
}
data class NullableScalar(override val itemType : INonNullableScalar) : IScalar, INullable
data class NullableBindable(override val itemType : INonNullableBindable) : IBindable, INullable

data class InternedScalar(val itemType: INonNullableScalar) : INonNullableScalar {
    override val name = itemType.name + "Interned"
}

sealed class PredefinedType : INonNullableScalar {
    override val name : String get() = javaClass.simpleName.capitalize()

    //special type to denote no parameter or return values
    object void : PredefinedType()

    //primitive
    object bool : PredefinedType()

    object byte : PredefinedType()
    object short : PredefinedType()
    object int : PredefinedType()
    object long : PredefinedType()

    object float : PredefinedType()
    object double : PredefinedType()

    object char : PredefinedType()

    //string
    object string : PredefinedType()

    //library types
    object guid : PredefinedType()
    object dateTime : PredefinedType()
    object uri : PredefinedType()
    object secureString : PredefinedType()

    //rd framework special
    object rdId : PredefinedType()
}

@Suppress("UNCHECKED_CAST")
abstract class Declaration(open val pointcut: BindableDeclaration?) : SettingsHolder() {
    abstract val _name : String

    val name : String by lazy {
        if (_name.isNotEmpty()) return@lazy _name.capitalize()


        val parent = pointcut as? Toplevel?: return@lazy ""

        //first try to find in toplevel properties
        val res = parent.javaClass.kotlin.declaredMemberProperties.firstOrNull {
            it.isAccessible = true
            val getterValue = try {it.getter.call()} catch (t: Throwable) {it.get(parent)} //`private val` vs `val`
            getterValue == this
        }?.name
        ?:

        //now search in members
        parent.declaredTypes.flatMap { it.ownMembers }.firstOrNull { when(it) {
            is Member.Field -> it.type == this
            is Member.Reactive -> it.genericParams.size == 1 && it.genericParams[0] == this
            else -> false
        }}?.name

        //default: error will arise at validation
        ?: ""

        return@lazy  res.capitalize()
    }

    val root : Root get () = pointcut?.root ?: this as Root


    @Suppress("UNCHECKED_CAST")
    internal fun <T:Any, S : SettingsHolder> getInheritedSetting(key: ISetting<T, S>) : T? = (settings[key] as? T?) ?: pointcut?.getInheritedSetting(key)

    //for toString purposes
    protected abstract val cl_name: String

    open val base : Declaration? = null
    open val isAbstract : Boolean get() = false

    internal var lazyInitializer: (Declaration.() -> Unit)? = null

    open fun initialize() {
        if (lazyInitializer == null) return
        val castedBody = lazyInitializer as Declaration.() -> Unit
        lazyInitializer = null
        this.castedBody()

        ownMembers.flatMap { it.referencedTypes }.filterIsInstance<Declaration>().forEach {
            it.initialize()
        }
    }

    val ownMembers: ArrayList<Member> = ArrayList()
        get() {
            require(lazyInitializer == null) { "$this: declaration hasn't been initialized" }
            return field
        }
    val membersOfBaseClasses: List<Member> get() = base?.allMembers.orEmpty()
    val allMembers: List<Member> get() = ownMembers + membersOfBaseClasses

    open fun serializationHash(initial: IncrementalHash64) : IncrementalHash64
        = ownMembers.fold(initial.mix(cl_name).mix(name).mix(base?.name)) { acc, member -> member.serializationHash(acc) }

    //todo delete? no recursion into lists, nullable, etc.?
    val referencedTypes: List<IType> get() = allMembers.flatMap { it.referencedTypes }.distinct()

    fun <T : Member> append(member: T) : T {
        ownMembers.add(member)
        member.owner = this@Declaration

        return member
    }

    open fun validate(errors: MutableList<String>) {
        val d = "'$this'"

        if (name.isBlank())
            errors.add("Declaration $d is invalid: empty name. All members are:" + allMembers.joinToString() { "\n->$it" })
        else if (!name[0].isLetter() || !name.all { it.isLetterOrDigit() || it == '_'})
            errors.add("Declaration $d is invalid: must be [A-Za-z][A-Za-z0-9_]*")

        ownMembers.forEach { it.validate(errors) }
    }

    override fun toString() = "$cl_name `$name`" + (pointcut != null).condstr { " :> $pointcut" }
}



abstract class BindableDeclaration(pointcut: BindableDeclaration?) : Declaration(pointcut)

abstract class Toplevel(pointcut: BindableDeclaration?) : BindableDeclaration(pointcut) {
    override val _name : String get() = javaClass.simpleName

    open val isLibrary = false

    val declaredTypes = ArrayList<Declaration>()

    override fun serializationHash(initial: IncrementalHash64) : IncrementalHash64 =
        declaredTypes.fold(super.serializationHash(initial)) {acc, type -> type.serializationHash(acc)}

    @Suppress("UNCHECKED_CAST")
    private fun <T : Declaration> append(typedef: T, typedefBody: T.() -> Unit) : T {
        declaredTypes.add(typedef)
        return typedef.apply { lazyInitializer = typedefBody as Declaration.() -> Unit}
    }



    class Part<T>(val name: String)

    //classes
    private fun baseclass0(name: String, base: Class.Abstract?, body: Class.() -> Unit) = append(Class.Abstract(name, this, base), body)
    fun baseclass(name : String, body: Class.() -> Unit) = baseclass0(name, null, body)
    fun baseclass(body: Class.() -> Unit) = baseclass0("", null, body)
    fun baseclass(name: String) = Part<Class.Abstract>(name)
    val baseclass = baseclass("")
    infix fun Part<Class.Abstract>.extends(p : Pair<Class.Abstract, Class.() -> Unit>) = baseclass0(name, p.first, p.second)

    @Deprecated("Use infix function 'extends'.", ReplaceWith("baseclass(name) extends base (body)"))
    fun baseclass(name : String, base: Class.Abstract?, body: Class.() -> Unit) = baseclass0(name, base, body)


    private fun classdef0(name: String, base: Class.Abstract?, body: Class.() -> Unit) = append(Class.Concrete(name, this, base), body)
    fun classdef(name: String, body: Class.() -> Unit) = classdef0(name, null, body)
    fun classdef(body: Class.() -> Unit) = classdef0("", null, body)
    fun classdef(name: String) = Part<Class.Concrete>(name)
    val classdef = classdef("")
    infix fun Part<Class.Concrete>.extends(p : Pair<Class.Abstract, Class.() -> Unit>) = classdef0(name, p.first, p.second)

    @Deprecated("Use infix function 'extends'.", ReplaceWith("classdef(name) extends base (body)"))
    fun classdef(name: String, base: Class.Abstract?, body: Class.() -> Unit) = classdef0(name, base, body)


    //structs
    private fun basestruct0(name: String, base: Struct.Abstract?, body: Struct.() -> Unit) = append(Struct.Abstract(name, this, base), body)
    fun basestruct(name : String, body: Struct.() -> Unit) = basestruct0(name, null, body)
    fun basestruct(body: Struct.() -> Unit) = basestruct0("", null, body)
    fun basestruct(name: String) = Part<Struct.Abstract>(name)
    val basestruct = basestruct("")
    infix fun Part<Struct.Abstract>.extends(p : Pair<Struct.Abstract, Struct.() -> Unit>) = basestruct0(name, p.first, p.second)

    @Deprecated("Use infix function 'extends'.", ReplaceWith("basestruct(name) extends base (body)"))
    fun basestruct(name : String, base: Struct.Abstract?, body: Struct.() -> Unit) = basestruct0(name, base, body)


    private fun structdef0(name : String, base: Struct.Abstract?, body: Struct.() -> Unit) = append(Struct.Concrete(name, this, base), body)
    fun structdef(name : String, body: Struct.() -> Unit) = structdef0(name, null, body)
    fun structdef(body: Struct.() -> Unit) = structdef0("", null, body)
    fun structdef(name : String) = Part<Struct.Concrete>(name)
    val structdef = structdef("")
    infix fun Part<Struct.Concrete>.extends(p : Pair<Struct.Abstract, Struct.() -> Unit>) = structdef0(name, p.first, p.second)

    @Deprecated("Use infix function 'extends'.", ReplaceWith("structdef(name) extends base (body)"))
    fun structdef(name : String, base: Struct.Abstract?, body: Struct.() -> Unit) = structdef0(name, base, body)

    fun aggregatedef(name: String, body: Aggregate.() -> Unit) = append(Aggregate(name, this), body)

    fun enum(name : String, body: Enum.() -> Unit) = append(Enum(name, this), body)
    fun enum(body: Enum.() -> Unit) = enum("", body)

}



sealed class Struct(override val _name: String, override val pointcut : Toplevel, override val base: Abstract?) : Declaration(pointcut), INonNullableScalar {
    override val cl_name = "${javaClass.simpleName.decapitalize()}_struct"

    class Abstract(name: String, pointcut: Toplevel, base: Abstract?) : Struct(name, pointcut, base) {
        override val isAbstract: Boolean get() = true
        operator fun invoke(body: Struct.() -> Unit)= this to body //for extends

    }
    class Concrete(name: String, pointcut: Toplevel, base: Abstract?) : Struct(name, pointcut, base)
}
operator fun <T : Struct> T.getValue(thisRef: Any?, property: KProperty<*>): T = this

sealed class Class(override val _name: String, override val pointcut : Toplevel, override val base: Abstract?) :
        BindableDeclaration(pointcut), INonNullableBindable, Extensible {
    override val cl_name = "${javaClass.simpleName.decapitalize()}_class"

    var isInternRoot = false
    override val extensions = mutableListOf<Ext>()

    class Abstract (name : String, pointcut : Toplevel, base: Abstract?) : Class(name, pointcut, base) {

        override val isAbstract : Boolean get() = true
        operator fun invoke(body: Class.() -> Unit) = this to body //for extends
    }
    class Concrete (name : String, pointcut : Toplevel, base: Abstract?) : Class(name, pointcut, base)
}
operator fun <T : Class> T.getValue(thisRef: Any?, property: KProperty<*>) : T = this

class Aggregate(override val _name: String, override val pointcut: Toplevel)
    : BindableDeclaration(pointcut), INonNullableBindable {
    override val cl_name = "aggregate"
}

class Enum(override val _name: String, override val pointcut : Toplevel) : Declaration(pointcut), INonNullableScalar {
    override val cl_name = "enum"

    val constants : List<Member.EnumConst> get() = ownMembers.filterIsInstance<Member.EnumConst>()
    operator fun String.unaryPlus() = append(Member.EnumConst(this))
}

interface Extensible {
    val extensions: MutableList<Ext>
}

abstract class Ext(pointcut : BindableDeclaration, val extName: String? = null) : Toplevel(pointcut), Extensible {
    override val extensions: MutableList<Ext> = mutableListOf()

    override val cl_name = "singleton"
    init {
        @Suppress("LeakingThis")
        when(pointcut) {
            is Root -> root.singletons.add(this)
            is Extensible -> {
                root.extensions.add(this)
                pointcut.extensions.add(this)
            }
        }
    }
}

val Declaration.isExtension get() = this is Ext && pointcut !is Root

abstract class Root(vararg val generators: IGenerator) : Toplevel(null) {
    internal val singletons = ArrayList<Ext>()
    internal val extensions = ArrayList<Ext>()

    override val cl_name = "root"

    val toplevels : List<Toplevel> get() = listOf(this) + singletons + extensions

    override fun initialize() {
        super.initialize()

        var initializedToplevelsCount = 0
        while (initializedToplevelsCount < toplevels.count()) {
            val tl = toplevels[initializedToplevelsCount++]

            var initializedTypesCount = 0
            if (tl != this) tl.initialize()

            //collection could grow during initialization
            while (initializedTypesCount < tl.declaredTypes.size) {
                tl.declaredTypes[initializedTypesCount++].initialize()
            }

        }
    }

    override fun validate(errors: MutableList<String>) {
        super.validate(errors)

        val decls = toplevels.flatMap {
            listOf(it) + it.declaredTypes
        }

        decls.forEach {
            if (it !== this)
                it.validate(errors)
        }

        for ((name, lst) in decls.groupBy { it.name }) {
            if (lst.size > 1) errors.add("Duplicated declaration name: '$name'")
        }
    }


}


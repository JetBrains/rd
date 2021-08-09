@file:Suppress("ClassName")

package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.generator.nova.cpp.Cpp17Generator
import com.jetbrains.rd.generator.nova.util.booleanSystemProperty
import com.jetbrains.rd.generator.nova.util.getSourceFileAndLine
import com.jetbrains.rd.util.hash.IncrementalHash64
import com.jetbrains.rd.util.string.condstr
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


interface IAttributedType : IHasItemType {
    val attributes: Map<Lang, List<String>>
}

data class ScalarAttributedType<out T> internal constructor(override val itemType: T, override val attributes: Map<Lang, List<String>>) : IAttributedType, IScalar where T : IScalar {
    override val name: String get() = itemType.name

    // Ignore attributes intentionally
    // Equality on nested type only
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalarAttributedType<*>

        if (itemType != other.itemType) return false

        return true
    }

    override fun hashCode(): Int {
        return itemType.hashCode()
    }

}

interface INullable : IHasItemType {
    override val itemType: INonNullable
    override val name : String get() = itemType.name + "Nullable"
}
data class NullableScalar(override val itemType : INonNullableScalar) : IScalar, INullable
data class NullableBindable(override val itemType : INonNullableBindable) : IBindable, INullable

data class InternedScalar(val itemType: INonNullableScalar, val internKey: InternScope) : INonNullableScalar {
    override val name = itemType.name + "Interned"
}

class InternScope(pointcut: BindableDeclaration?, override val _name: String = ""): Declaration(pointcut) {
    val keyName: String
        get() = name.also { assert(it != "") { "No name specified for intern root and no name can be derived for intern root in $pointcut" } }

    override val cl_name: String
        get() = javaClass.simpleName
}

enum class Modifier{
    Abstract,
    Open,
    Sealed
}

sealed class Context(pointcut: Toplevel, val type: INonNullableScalar): Declaration(pointcut) {
    class Generated(pointcut: Toplevel, override val _name: String = "", type: INonNullableScalar, var isHeavyKey: Boolean = true) : Context(pointcut, type) {
        val keyName: String
            get() = name.also { assert(it != "") { "No name specified for context and no name can be derived for key in $pointcut" } }

        override val cl_name: String
            get() = javaClass.simpleName
    }

    class External(pointcut: Toplevel, val perGeneratorNames: List<Pair<java.lang.Class<in GeneratorBase>, String>>, type: INonNullableScalar) : Context(pointcut, type) {
        override val _name: String
            get() = ""
        override val cl_name: String
            get() = javaClass.simpleName

        fun fqnFor(generator: GeneratorBase): String = perGeneratorNames.first { it.first.isAssignableFrom(generator.javaClass) }.second
    }
}




sealed class PredefinedType : INonNullableScalar {
    override val name : String get() = javaClass.simpleName.capitalize()

    //special type to denote no parameter or return values
    object void : PredefinedType()

    //primitive
    object bool : PredefinedType()


    /**
     * byte, short, int and long that are signed/unsigned based by platform.
     * E.g. byte is signed in java but unsigned in C#
     */
    abstract class NativeIntegral : PredefinedType()

    /**
     * float and double that are single and double precision floating point types respectively.
     * Usually IEEE-754 32 and 64 bit.
     */
    abstract class NativeFloatingPointType : PredefinedType()

    /**
     * Unsigned versions of primitive integral types on all platforms: java, c#, c++
     */
    open class UnsignedIntegral internal constructor(val itemType : NativeIntegral) : PredefinedType() {
        override val name: String get() = "U${itemType.name}"
    }

    /*
     *  No guarantee for being signed. Generates to "byte" in C#, for instance.
     */
    object byte : NativeIntegral()
    object short : NativeIntegral()
    object int : NativeIntegral()
    object long : NativeIntegral()


    object float : NativeFloatingPointType()
    object double : NativeFloatingPointType()

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

    //unsigned
    object ubyte: UnsignedIntegral(byte)
    object ushort: UnsignedIntegral(short)
    object uint: UnsignedIntegral(int)
    object ulong: UnsignedIntegral(long)

    //aliases
    companion object {
        val int8 : NativeIntegral get() = byte
        val uint8 : UnsignedIntegral get() = ubyte

        val int16 : NativeIntegral get() = short
        val uint16 : UnsignedIntegral get() = ushort

        val int32 : NativeIntegral get() = int
        val uint32 : UnsignedIntegral get() = uint

        val int64 : NativeIntegral get() = long
        val uint64 : UnsignedIntegral get() = ulong
    }
}

@Suppress("UNCHECKED_CAST")
abstract class Declaration(open val pointcut: BindableDeclaration?) : SettingsHolder() {
    abstract val _name: String
    var documentation: String? = null
    var sourceFileAndLine: String? = null

    open val name: String by lazy {
        if (_name.isNotEmpty()) return@lazy _name.capitalize()

        val parent = pointcut as? Toplevel ?: return@lazy ""

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
    internal fun <T:Any, S : SettingsHolder> getSettingInHierarchy(key: ISetting<T, S>) : T? = (settings[key] as? T?) ?: pointcut?.getSettingInHierarchy(key)

    //for toString purposes
    protected abstract val cl_name: String

    open val base : Declaration? = null
    open val implements: MutableList<Interface> = mutableListOf()
    open val modifier: Modifier = Modifier.Sealed

    val isAbstract: Boolean get() = modifier == Modifier.Abstract
    val isOpen: Boolean get() = modifier == Modifier.Open
    val isSealed: Boolean get() = modifier == Modifier.Sealed

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

    val constantMembers: ArrayList<Member.Const> = ArrayList()
    val ownMembers: ArrayList<Member> = ArrayList()
        get() {
            require(lazyInitializer == null) { "${this._name}: declaration hasn't been initialized" }
            return field
        }
    val membersOfBaseClasses: List<Member> get() = base?.allMembers.orEmpty()
    val allMembers: List<Member> get() = ownMembers + membersOfBaseClasses

    open fun serializationHash(initial: IncrementalHash64) : IncrementalHash64
        = ownMembers.sortedBy { it.name }.fold(initial.mix(cl_name).mix(name).mix(base?.name)) { acc, member -> member.serializationHash(acc) }

    //todo delete? no recursion into lists, nullable, etc.?
    val referencedTypes: List<IType> get() = allMembers.flatMap { it.referencedTypes }.distinct()

    fun <T : Member> append(member: T) : T {
        ownMembers.add(member)
        member.owner = this@Declaration

        return member
    }

    fun <T : Member.Const> appendConst(member: T) : T {
        constantMembers.add(member)
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
        declaredTypes.sortedBy { it.name }.fold(super.serializationHash(initial)) {acc, type -> type.serializationHash(acc)}

    @Suppress("UNCHECKED_CAST")
    private fun <T : Declaration> append(typedef: T, typedefBody: T.() -> Unit) : T {
        typedef.sourceFileAndLine = getSourceFileAndLine(booleanSystemProperty(SharedGeneratorSettings.LineNumbersInCommentsEnv, true))
        declaredTypes.add(typedef)
        return typedef.apply { lazyInitializer = typedefBody as Declaration.() -> Unit}
    }

    init {
        sourceFileAndLine = getSourceFileAndLine(booleanSystemProperty(SharedGeneratorSettings.LineNumbersInCommentsEnv, true))
    }


    class Part<T>(val name: String)

    //classes
    private fun baseclass0(name: String, base: Class.Abstract?, body: Class.() -> Unit) = append(Class.Abstract(name, this, base), body)
    fun baseclass(name : String, body: Class.() -> Unit) = baseclass0(name, null, body)
    fun baseclass(body: Class.() -> Unit) = baseclass0("", null, body)
    fun baseclass(name: String) = Part<Class.Abstract>(name)
    val baseclass = baseclass("")
    infix fun Part<Class.Abstract>.extends(p : Pair<Class.Abstract, Class.() -> Unit>) = baseclass0(name, p.first, p.second)

    // base classes interface dsl
    infix fun Part<Class.Abstract>.extends(p : Class.Abstract) = baseclass0(name, p){}.toIntermediateClass()
    infix fun Part<Class.Abstract>.implements(i: Interface) = baseclass0(name, null){}.also { it.implements.add(i) }
    infix fun Part<Class.Abstract>.implements(li: List<Interface>) = baseclass0(name, null){}.also { it.implements.addAll(li) }
    infix fun IntermediateClass<Class.Abstract>.implements(i: Interface) = this.clazz.also { it.implements.add(i) }
    infix fun IntermediateClass<Class.Abstract>.implements(li: List<Interface>) = this.clazz.also { it.implements.addAll(li) }

    @Suppress("UNCHECKED_CAST")
    infix fun Class.Abstract.with(body: Class.() -> Unit) = this.also { lazyInitializer = body as Declaration.() -> Unit }

    @Deprecated("Use infix function 'extends'.", ReplaceWith("baseclass(name) extends base (body)"))
    fun baseclass(name : String, base: Class.Abstract?, body: Class.() -> Unit) = baseclass0(name, base, body)


    private fun classdef0(name: String, base: Class?, body: Class.() -> Unit) = append(Class.Concrete(name, this, base), body)
    fun classdef(name: String, body: Class.() -> Unit) = classdef0(name, null, body)
    fun classdef(body: Class.() -> Unit) = classdef0("", null, body)
    fun classdef(name: String) = Part<Class.Concrete>(name)
    val classdef = classdef("")
    infix fun Part<Class.Concrete>.extends(p : Pair<Class, Class.() -> Unit>) = classdef0(name, p.first, p.second)

    // concrete classes interface dsl
    infix fun Part<Class.Concrete>.extends(p : Class.Concrete) = classdef0(name, p){}.toIntermediateClass()
    infix fun Part<Class.Concrete>.implements(i: Interface) = classdef0(name, null){}.also { it.implements.add(i) }
    infix fun Part<Class.Concrete>.implements(li: List<Interface>) = classdef0(name, null){}.also { it.implements.addAll(li) }
    infix fun IntermediateClass<Class.Concrete>.implements(i: Interface) = this.clazz.also { it.implements.add(i) }
    infix fun IntermediateClass<Class.Concrete>.implements(li: List<Interface>) = this.clazz.also { it.implements.addAll(li) }

    @Suppress("UNCHECKED_CAST")
    infix fun Class.Concrete.with(body: Class.() -> Unit) = this.also { lazyInitializer = body as Declaration.() -> Unit }


    private fun openclass0(name: String, base: Class?, body: Class.() -> Unit) = append(Class.Open(name, this, base), body)
    fun openclass(name: String, body: Class.() -> Unit) = openclass0(name, null, body)
    fun openclass(body: Class.() -> Unit) = openclass0("", null, body)
    fun openclass(name: String) = Part<Class.Open>(name)
    val openclass = openclass("")
    infix fun Part<Class.Open>.extends(p : Pair<Class, Class.() -> Unit>) = openclass0(name, p.first, p.second)

    // open classes interface dsl
    infix fun Part<Class.Open>.extends(p : Class) = openclass0(name, p){}.toIntermediateClass()
    infix fun Part<Class.Open>.implements(i: Interface) = openclass0(name, null){}.also { it.implements.add(i) }
    infix fun Part<Class.Open>.implements(li: List<Interface>) = openclass0(name, null){}.also { it.implements.addAll(li) }
    infix fun IntermediateClass<Class.Open>.implements(i: Interface) = this.clazz.also { it.implements.add(i) }
    infix fun IntermediateClass<Class.Open>.implements(li: List<Interface>) = this.clazz.also { it.implements.addAll(li) }

    @Suppress("UNCHECKED_CAST")
    infix fun Class.Open.with(body: Class.() -> Unit) = this.also{ lazyInitializer = body as Declaration.() -> Unit }


    @Deprecated("Use infix function 'extends'.", ReplaceWith("classdef(name) extends base (body)"))
    fun classdef(name: String, base: Class.Abstract?, body: Class.() -> Unit) = classdef0(name, base, body)


    //structs
    private fun basestruct0(name: String, base: Struct.Abstract?, body: Struct.() -> Unit) = append(Struct.Abstract(name, this, base), body)
    fun basestruct(name : String, body: Struct.() -> Unit) = basestruct0(name, null, body)
    fun basestruct(body: Struct.() -> Unit) = basestruct0("", null, body)
    fun basestruct(name: String) = Part<Struct.Abstract>(name)
    val basestruct = basestruct("")
    infix fun Part<Struct.Abstract>.extends(p : Pair<Struct.Abstract, Struct.() -> Unit>) = basestruct0(name, p.first, p.second)

    // base structs interface dsl
    infix fun Part<Struct.Abstract>.extends(p : Struct.Abstract) = basestruct0(name, p){}.toIntermediateClass()
    infix fun Part<Struct.Abstract>.implements(i: Interface) = basestruct0(name, null){}.also { it.implements.add(i) }
    infix fun Part<Struct.Abstract>.implements(li: List<Interface>) = basestruct0(name, null){}.also { it.implements.addAll(li) }
    infix fun IntermediateClass<Struct.Abstract>.implements(i: Interface) = this.clazz.also { it.implements.add(i) }
    infix fun IntermediateClass<Struct.Abstract>.implements(li: List<Interface>) = this.clazz.also { it.implements.addAll(li) }

    @Suppress("UNCHECKED_CAST")
    infix fun Struct.Abstract.with(body: Struct.() -> Unit) = this.also { lazyInitializer = body as Declaration.() -> Unit }

    @Deprecated("Use infix function 'extends'.", ReplaceWith("basestruct(name) extends base (body)"))
    fun basestruct(name : String, base: Struct.Abstract?, body: Struct.() -> Unit) = basestruct0(name, base, body)


    private fun structdef0(name : String, base: Struct?, body: Struct.() -> Unit) = append(Struct.Concrete(name, this, base), body)
    fun structdef(name : String, body: Struct.() -> Unit) = structdef0(name, null, body)
    fun structdef(body: Struct.() -> Unit) = structdef0("", null, body)
    fun structdef(name : String) = Part<Struct.Concrete>(name)
    val structdef = structdef("")
    infix fun Part<Struct.Concrete>.extends(p : Pair<Struct, Struct.() -> Unit>) = structdef0(name, p.first, p.second)

    private fun openstruct0(name: String, base: Struct?, body: Struct.() -> Unit) = append(Struct.Open(name, this, base), body)
    fun openstruct(name: String, body: Struct.() -> Unit) = openstruct0(name, null, body)
    fun openstruct(body: Struct.() -> Unit) = openstruct0("", null, body)
    fun openstruct(name: String) = Part<Struct.Open>(name)
    val openstruct = openstruct("")
    infix fun Part<Struct.Open>.extends(p : Pair<Struct, Struct.() -> Unit>) = openstruct0(name, p.first, p.second)

    // open structs interface dsl
    infix fun Part<Struct.Open>.extends(p : Struct) = openstruct0(name, p){}.toIntermediateClass()
    infix fun Part<Struct.Open>.implements(i: Interface) = openstruct0(name, null){}.also { it.implements.add(i) }
    infix fun Part<Struct.Open>.implements(li: List<Interface>) = openstruct0(name, null){}.also { it.implements.addAll(li) }
    infix fun IntermediateClass<Struct.Open>.implements(i: Interface) = this.clazz.also { it.implements.add(i) }
    infix fun IntermediateClass<Struct.Open>.implements(li: List<Interface>) = this.clazz.also { it.implements.addAll(li) }

    @Suppress("UNCHECKED_CAST")
    infix fun Struct.Open.with(body: Struct.() -> Unit) = this.also { lazyInitializer = body as Declaration.() -> Unit }



    @Deprecated("Use infix function 'extends'.", ReplaceWith("structdef(name) extends base (body)"))
    fun structdef(name : String, base: Struct.Abstract?, body: Struct.() -> Unit) = structdef0(name, base, body)

    fun aggregatedef(name: String, body: Aggregate.() -> Unit) = append(Aggregate(name, this), body)

    fun enum(name : String, body: Enum.() -> Unit) = append(Enum(name, this), body)
    fun enum(body: Enum.() -> Unit) = enum("", body)

    fun flags(name : String, body: Enum.() -> Unit) = enum(name, body).apply { flags = true }
    fun flags(body: Enum.() -> Unit) = enum( body).apply { flags = true }

    fun internScope(name: String = "") = InternScope(this, name)
    fun context(type: INonNullableScalar) = append(Context.Generated(this, "", type)) {}
    fun externalContext(type: INonNullableScalar, perGeneratorNames: List<Pair<java.lang.Class<in GeneratorBase>, String>>) = append(Context.External(this, perGeneratorNames, type)) {}


    private fun interfacedef0(name: String, base: List<Interface>, body: Interface.() -> Unit) = append(Interface(name, this, base), body)
    fun interfacedef(name: String, body: Interface.() -> Unit) = interfacedef0(name, emptyList(), body)
    fun interfacedef(body: Interface.() -> Unit) = interfacedef0("", emptyList(), body)
    fun interfacedef(name: String) = Part<Interface>(name)
    val interfacedef = interfacedef("")

    infix fun Part<Interface>.extends(p: Pair<Interface, Interface.() -> Unit>) = interfacedef0(name, listOf(p.first), p.second)
    fun Part<Interface>.extends(vararg baseInterfaces: Interface, init: Interface.() -> Unit) = interfacedef0(name, baseInterfaces.toList(), init)

    private fun <T : Declaration> T.toIntermediateClass() = IntermediateClass(this)

    class IntermediateClass<T : Declaration>(val clazz: T)
}

class Interface(override val _name: String, pointcut: Toplevel, val baseInterfaces: List<Interface>) : Declaration(pointcut){
    override val cl_name = "${javaClass.simpleName.decapitalize()}_interface"
    operator fun invoke(body: Interface.() -> Unit) = this to body //for extends

    operator fun plus(inter: Interface) = mutableListOf(this, inter)
    operator fun plus(interList: List<Interface>) = interList + this
}

sealed class Struct(override val _name: String, override val pointcut : Toplevel, override val base: Struct?, val isUnknown: Boolean = false) : Declaration(pointcut), INonNullableScalar {
    override val cl_name = "${javaClass.simpleName.decapitalize()}_struct"

    class Abstract(name: String, pointcut: Toplevel, base: Abstract?) : Struct(name, pointcut, base) {
        override val modifier: Modifier = Modifier.Abstract
        operator fun invoke(body: Struct.() -> Unit)= this to body //for extends
    }

    class Concrete(name: String, pointcut: Toplevel, base: Struct?, unknown: Boolean = false) : Struct(name, pointcut, base, unknown)

    class Open (name: String, pointcut : Toplevel, base: Struct?) : Struct(name, pointcut, base) {
        override val modifier: Modifier = Modifier.Open
        operator fun invoke(body: Struct.() -> Unit) = this to body //for extends
    }
}
operator fun <T : Struct> T.getValue(thisRef: Any?, property: KProperty<*>): T = this

sealed class Class(override val _name: String, override val pointcut : Toplevel, override val base: Class?, val isUnknown: Boolean = false) :
        BindableDeclaration(pointcut), INonNullableBindable, Extensible {
    override val cl_name = "${javaClass.simpleName.decapitalize()}_class"

    internal val internRootForScopes = mutableListOf<String>()
    override val extensions = mutableListOf<Ext>()

    class Abstract (name : String, pointcut : Toplevel, base: Abstract?) : Class(name, pointcut, base) {

        override val modifier: Modifier  = Modifier.Abstract
        operator fun invoke(body: Class.() -> Unit) = this to body //for extends
    }

    class Concrete (name : String, pointcut : Toplevel, base: Class?, unknown: Boolean = false) : Class(name, pointcut, base, unknown)

    class Open (name: String, pointcut : Toplevel, base: Class?) : Class(name, pointcut, base) {
        override val modifier: Modifier  = Modifier.Open
        operator fun invoke(body: Class.() -> Unit) = this to body //for extends
    }
}
operator fun <T : Class> T.getValue(thisRef: Any?, property: KProperty<*>) : T = this

class Aggregate(override val _name: String, override val pointcut: Toplevel)
    : BindableDeclaration(pointcut), INonNullableBindable {
    override val cl_name = "aggregate"
}

open class Enum(override val _name: String, override val pointcut : Toplevel) : Declaration(pointcut), INonNullableScalar {
    override val cl_name = "enum"

    var flags = false
    val setOrEmpty : String get() = flags.condstr { "Set" }

    val constants : List<Member.EnumConst> get() = ownMembers.filterIsInstance<Member.EnumConst>()
    operator fun String.unaryPlus() = append(Member.EnumConst(this))

    override fun validate(errors: MutableList<String>) {
        super.validate(errors)

        if (flags && constants.size > 30)
            errors.add("EnumSet $this contains more than 30 constants (does't fit Int)")

        val fields = constants.mapIndexed { index, field ->
            field.getSetting(Cpp17Generator.EnumConstantValue) ?: index
        }
        if (fields.distinct().size != fields.size) {
            errors.add("EnumSet contains duplicated enum constants, values: $fields")
        }
    }
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

abstract class Root(vararg val hardcodedGenerators: IGenerator) : Toplevel(null) {
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
            if (lst.size > 1) {
                errors.add("Root '${this.name}' has duplicated top-level declaration '$name' in files: [${lst.joinToString(separator = ", ") { it.sourceFileAndLine.orEmpty() }}]")
            }
        }
    }
}

fun <T : Declaration> T.doc(value: String) : T{
    documentation = value
    return this
}

val Declaration.isConcrete
    get() = this is Class.Concrete || this is Struct.Concrete || this is Aggregate
val IType.hasEmptyConstructor : Boolean get() = when (this) {
    is Class.Concrete -> allMembers.all { it.hasEmptyConstructor }
    is Aggregate -> true

    else -> false
}

class TypeWithValue internal constructor(val type: IType, val defaultValue: Any)
package com.jetbrains.rd.generator.nova

import com.jetbrains.rd.util.hash.IncrementalHash64
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

enum class FlowKind {
    Both,
    Source,
    Sink
}


sealed class Member(name: String, referencedTypes: List<IType>) : SettingsHolder() {
    open val name: String = name.decapitalize()
    var documentation: String? = null
    lateinit var owner: Declaration
    val referencedTypes : List<IType> = referencedTypes.flatMap { expandItemTypes(it) }.distinct()

    private fun expandItemTypes(type: IType) : List<IType> {
        val res = mutableListOf(type)
        if (type is IHasItemType)
            res.addAll(expandItemTypes(type.itemType))
        return res
    }

    fun serializationHash(initial: IncrementalHash64) = referencedTypes.fold(initial.mix(name)) { acc, type -> acc.mix(type.name) }

    class EnumConst(override val name: String) : Member(name, emptyList()) //no need to decapitalize

    class Field(name : String, val type: IType) : Member(name, listOf(type)) {
        internal var emptyCtorSuppressed = false
        internal var isOptional = false
        internal var defaultValue: Any? = null

        //only for structs
        internal var usedInEquals = true
    }

    class Method(override val name: String, val resultType: IType,  val args: List<Pair<String, IType>>) : Member(name, args.map { it.second } + resultType)

    sealed class Const(name : String, val type: IScalar, val value: String) : Member(name, listOf<IType>(type)){
        class Integral : Const {
            constructor(name : String, type: PredefinedType, value: String) : super(name, type, value)
            constructor(name : String, type: ScalarAttributedType<PredefinedType>, value: String) : super(name, type, value)
        }

        class Enum(name: String, type: com.jetbrains.rd.generator.nova.Enum, value: EnumConst) : Const(name, type, value.name)
    }


    sealed class DelegateType {
        data class Custom(val fqn: String) : DelegateType()
        data class Delegated(val type: IType) : DelegateType()
    }

    data class ExtensionDelegate(
        val klass: KClass<out IGenerator>,
        val flowTransform: FlowTransform?,
        val delegateType: DelegateType,
        val factoryFqn: String? = null
    ) {
        constructor(klass: KClass<out IGenerator>,
                    flowTransform: FlowTransform?,
                    delegateType: IType,
                    factoryFqn: String? = null) : this(klass, flowTransform, DelegateType.Delegated(delegateType), factoryFqn)
        constructor(klass: KClass<out IGenerator>,
                    flowTransform: FlowTransform?,
                    delegateFqn: String,
                    factoryFqn: String? = null) : this(klass, flowTransform, DelegateType.Custom(delegateFqn), factoryFqn)
    }

    sealed class Reactive(name: String, vararg val genericParams: IType) : Member(name, genericParams.toList()) {
        var flow  : FlowKind = FlowKind.Both
        var freeThreaded : Boolean = false
        var context : Context? = null


        class Task  (name : String, paramType : IScalar, resultType: IType) : Reactive(name, paramType, resultType)
        class Signal(name : String, paramType : IScalar) : Reactive(name, paramType)

        sealed class Stateful(name : String, vararg genericParams: IType)  : Reactive(name, *genericParams) {
            class Property  (name: String, valueType: IType, val defaultValue: Any? = null) : Stateful(name, valueType) {
                val isNullable
                    get() = referencedTypes.first().isNullable()

                private fun IType.isNullable() : Boolean = when(this) {
                    is INullable -> true
                    is IAttributedType -> itemType.isNullable()
                    else -> false
                }
            }
            class List      (name : String, itemType : IType) : Stateful(name, itemType)
            class Set       (name : String, itemType : INonNullableScalar) : Stateful(name, itemType)
            class Map       (name : String, keyType : INonNullableScalar, valueType: INonNullable): Stateful(name, keyType, valueType)

            abstract class Extension(name : String, val delegatedBy: IType, vararg _delegates: ExtensionDelegate)
                : Stateful(name) {

                val delegates = _delegates

                fun hasFactoryFqn(generator: IGenerator, flowTransform: FlowTransform) =
                    findDelegate(generator, flowTransform)?.factoryFqn != null

                fun findDelegate(generator: IGenerator, flowTransform: FlowTransform): ExtensionDelegate? {
                    return delegates.firstOrNull {
                        it.klass.safeCast(generator) != null && (it.flowTransform == null || it.flowTransform == flowTransform)
                    }
                }

                fun isSimplyDelegated(generator: IGenerator, flowTransform: FlowTransform) = findDelegate(generator, flowTransform)?.delegateType
                    ?.let { !hasFactoryFqn(generator, flowTransform) && it is DelegateType.Delegated && it.type.unwrapAttributed() == this.delegatedBy.unwrapAttributed() } ?: false

                constructor(name: String, delegatedBy: Class, vararg _delegateFqn: Pair<KClass<out IGenerator>, String>) :
                    this(name, delegatedBy, *_delegateFqn.map { ExtensionDelegate(it.first, null, it.second) }.toTypedArray())
            }
        }
    }

    fun validate(errors: MutableList<String>) {
        val m = "'${owner.name}.$name'"


        if (name.isBlank())
            errors.add("Member $m is invalid: empty name")
        else if (!name[0].isLetter() || !name.all { it.isLetterOrDigit() || it == '_'})
            errors.add("Member $m is invalid: must be [A-Za-z][A-Za-z0-9_]*")
        else if (name.capitalize() == owner.name)
            errors.add("Member $m is invalid: name cannot be the same as its enclosing declaration")
        else if (owner.ownMembers.any { it != this && it.name == name })
            errors.add("Member $m is duplicated")
        else if (owner.membersOfBaseClasses.any { it.name == name })
            errors.add("Member $m is contained by base class")
    }

    override fun toString(): String {
        return "${javaClass.simpleName} $name"
    }


}

fun Member.Field.notUsedInEquals() = apply { usedInEquals = false }

val Member.Field.suppressEmptyCtor get() = apply { emptyCtorSuppressed = true }
val Member.Field.optional get() = apply {
    if (type is INonNullable) throw GeneratorException("Field '$name' can't be optional because it's not nullable, actual type: ${type.name}")
    isOptional = true
}

fun Member.Field.default(value: Long) : Member.Field {
    if (type !== PredefinedType.int && type !== PredefinedType.long) {
        throw GeneratorException("Default value number does not match field type")
    }
    defaultValue = value
    return this
}

fun Member.Field.default(value: String) : Member.Field {
    if (type === PredefinedType.string
        || (type is ScalarAttributedType<*> && type.itemType === PredefinedType.string)
        || type is Enum
    ) {
        defaultValue = value
        return this
    }

    throw GeneratorException("Default value string does not match field type")
}

fun Member.Field.default(value: Boolean) : Member.Field {
    if (type !== PredefinedType.bool) {
        throw GeneratorException("Default value boolean does not match field type")
    }
    defaultValue = value
    return this
}

fun Member.doc(value: String) : Member {
    documentation = value
    return this
}

val <T: Member.Reactive> T.write: T get() = apply { flow = FlowKind.Source }
val <T: Member.Reactive> T.readonly: T get() = apply { flow = FlowKind.Sink }
val <T: Member.Reactive> T.async  : T get() = apply { freeThreaded = true }
val Member.hasEmptyConstructor : Boolean get() = when (this) {
    is Member.Field -> type.hasEmptyConstructor && !emptyCtorSuppressed
    is Member.Reactive -> true

    else -> throw GeneratorException("Unsupported member: $this")
}

fun Member.Reactive.perContext(key: Context) : Member.Reactive {
    require(key !is Context.Generated || key.isHeavyKey) { "Only non-light keys can be used for per-context entities" }
    context = key
    return this
}



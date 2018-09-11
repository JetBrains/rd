package com.jetbrains.rider.generator.nova

import com.jetbrains.rider.util.hash.IncrementalHash64
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

enum class FlowKind {
    Both,
    Source,
    Sink
}


sealed class Member(name: String, val referencedTypes: List<IType>) : SettingsHolder() {
    open val name: String = name.decapitalize()
    var documentation: String? = null
    lateinit var owner: Declaration

    fun serializationHash(initial: IncrementalHash64) = referencedTypes.fold(initial.mix(name)) { acc, type -> acc.mix(type.name) }

    class EnumConst(override val name: String) : Member(name, emptyList()) //no need to decapitalize

    class Field(name : String, val type: IType) : Member(name, listOf(type)) {
        internal var emptyCtorSuppressed = false
        internal var isOptional = false
        internal var defaultValue: Any? = null

        //only for structs
        internal var usedInEquals = true
    }

    data class ExtensionDelegate(
            val klass: KClass<out IGenerator>,
            val flowTransform: FlowTransform?,
            val delegateFqn: String,
            val factoryFqn: String = delegateFqn
    )

    sealed class Reactive(name: String, vararg val genericParams: IType) : Member(name, genericParams.toList()) {
        var flow  : FlowKind = FlowKind.Both
        var freeThreaded : Boolean = false


        class Task  (name : String, paramType : IScalar, resultType: IScalar) : Reactive(name, paramType, resultType)
        class Signal(name : String, paramType : IScalar) : Reactive(name, paramType)

        sealed class Stateful(name : String, vararg genericParams: IType)  : Reactive(name, *genericParams) {
            class Property  (name: String, valueType: IType, val defaultValue: Any? = null) : Stateful(name, valueType) {
                val isNullable
                    get() = referencedTypes.first() is INullable
            }
            class List      (name : String, itemType : IType) : Stateful(name, itemType)
            class Set       (name : String, itemType : INonNullableScalar) : Stateful(name, itemType)
            class Map       (name : String, keyType : INonNullableScalar, valueType: INonNullable): Stateful(name, keyType, valueType)

            abstract class Extension(name : String, val delegatedBy: Class, vararg _delegates: ExtensionDelegate)
                : Stateful(name) {

                val delegates = _delegates

                fun fqn(generator: IGenerator, flowTransform: FlowTransform) : String {
                    return findDelegate(generator, flowTransform)?.delegateFqn ?: javaClass.simpleName
                }

                fun factoryFqn(generator: IGenerator, flowTransform: FlowTransform) : String {
                    return findDelegate(generator, flowTransform)?.factoryFqn ?: javaClass.simpleName
                }

                private fun findDelegate(generator: IGenerator, flowTransform: FlowTransform): ExtensionDelegate? {
                    return delegates.firstOrNull {
                        it.klass.safeCast(generator) != null && (it.flowTransform == null || it.flowTransform == flowTransform)
                    }
                }

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
    if (type !== PredefinedType.string && type !is Enum) {
        throw GeneratorException("Default value string does not match field type")
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

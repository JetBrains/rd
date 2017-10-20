package com.jetbrains.rider.generator.nova

enum class FlowKind {
    Both,
    Source,
    Sink
}

sealed class Member(name: String, val referencedTypes: List<IType>) {
    open val name: String = name.decapitalize()
    lateinit var owner: Declaration

    class EnumConst (override val name: String) : Member(name, emptyList()) //no need to decapitalize

    class Field (name : String, val type: IType): Member(name, listOf(type)) {
        internal var emptyCtorSuppressed = false
        internal var isOptional = false
    }

    sealed class Reactive(name: String, vararg val genericParams: IType) : Member(name, genericParams.toList()) {
        var flow  : FlowKind = FlowKind.Both
        var freeThreaded : Boolean = false


        class Task  (name : String, paramType : IScalar, resultType: IScalar) : Reactive(name, paramType, resultType)
        class Signal(name : String, paramType : IScalar) : Reactive(name, paramType)

        sealed class Stateful(name : String, vararg genericParams: IType)  : Reactive(name, *genericParams) {
            class Property  (name : String, valueType : IType) : Stateful(name, valueType)
            class List      (name : String, itemType : IType) : Stateful(name, itemType)
            class Set       (name : String, itemType : INonNullableScalar) : Stateful(name, itemType)
            class Map       (name : String, keyType : INonNullableScalar, valueType: INonNullable): Stateful(name, keyType, valueType)

            class Text     (name: String) : Stateful(name)
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

val Member.Field.suppressEmptyCtor get() = apply { emptyCtorSuppressed = true }
val Member.Field.optional get() = apply {
    if (type is INonNullable) throw GeneratorException("Field '$name' can't be optional because it's not nullable, actual type: ${type.name}")
    isOptional = true
}

val <T: Member.Reactive> T.write: T get() = apply { flow = FlowKind.Source }
val <T: Member.Reactive> T.readonly: T get() = apply { flow = FlowKind.Sink }
val <T: Member.Reactive> T.async  : T get() = apply { freeThreaded = true }

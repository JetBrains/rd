package com.jetbrains.rd.generator.nova


import com.jetbrains.rd.generator.nova.Member.Field
import com.jetbrains.rd.generator.nova.Member.Reactive.Signal
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.*
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.List
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.Map
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.Set
import com.jetbrains.rd.generator.nova.Member.Reactive.Task
import com.jetbrains.rd.util.PublicApi
import java.lang.IllegalArgumentException

val ProtocolInternScope = InternScope(null, "Protocol")

fun Struct.field(name : String, type : IScalar) = append(Field(name, type))
fun Class.field(name : String, type : IType) = append(Field(name, type))
fun Toplevel.field(name : String, type : Aggregate) = append(Field(name, type))
fun Aggregate.field(name : String, type : Aggregate) = append(Field(name, type))
fun Declaration.const(name: String, type: PredefinedType, value: String) = appendConst(Member.Const.Integral(name, type, value))
fun Declaration.const(name: String, type: Enum, value: Member.EnumConst) = appendConst(Member.Const.Enum(name, type, value))

fun Declaration.const(name: String, type: PredefinedType.bool, value: Boolean) = appendConst(Member.Const.Integral(name, type, value.toString()))
fun Declaration.const(name: String, type: PredefinedType.char, value: Char) = appendConst(Member.Const.Integral(name, type, value.toString()))
fun Declaration.const(name: String, type: PredefinedType.string, value: String) = appendConst(Member.Const.Integral(name, type, value))
fun Declaration.const(name: String, type: Enum, value: Int) = appendConst(Member.Const.Enum(name, type, type.constants[value]))
fun Declaration.const(name: String, type: Enum, value: String) {
    type.constants.find { it.name == value}?.let {
        appendConst(Member.Const.Enum(name, type, it))
    } ?: throw IllegalArgumentException("value:$value is not present in enum:$type")
}

fun Declaration.const(name: String, type: PredefinedType.byte, value: Byte) = appendConst(Member.Const.Integral(name, type, value.toString()))
fun Declaration.const(name: String, type: PredefinedType.short, value: Short) = appendConst(Member.Const.Integral(name, type, value.toString()))
fun Declaration.const(name: String, type: PredefinedType.int, value: Int) = appendConst(Member.Const.Integral(name, type, value.toString()))
fun Declaration.const(name: String, type: PredefinedType.long, value: Long) = appendConst(Member.Const.Integral(name, type, value.toString()))
@ExperimentalUnsignedTypes
fun Declaration.const(name: String, type: PredefinedType.ubyte, value: UByte) = appendConst(Member.Const.Integral(name, type, value.toString()))
@ExperimentalUnsignedTypes
fun Declaration.const(name: String, type: PredefinedType.ushort, value: UShort) = appendConst(Member.Const.Integral(name, type, value.toString()))
@ExperimentalUnsignedTypes
fun Declaration.const(name: String, type: PredefinedType.uint, value: UInt) = appendConst(Member.Const.Integral(name, type, value.toString()))
@ExperimentalUnsignedTypes
fun Declaration.const(name: String, type: PredefinedType.ulong, value: ULong) = appendConst(Member.Const.Integral(name, type, value.toString()))

fun Declaration.const(name: String, type: PredefinedType.float, value: Float) = appendConst(Member.Const.Integral(name, type, value.toString()))
fun Declaration.const(name: String, type: PredefinedType.double, value: Double) = appendConst(Member.Const.Integral(name, type, value.toString()))

fun BindableDeclaration.signal(name : String, valueType : IScalar) = append(Signal(name, valueType))
fun BindableDeclaration.source(name : String, valueType : IScalar) = append(Signal(name, valueType).write)
fun BindableDeclaration.sink(name : String, valueType : IScalar) = append(Signal(name, valueType).readonly)

@PublicApi
@Suppress("unused")
@Deprecated("", ReplaceWith("signal(name, void)"))
fun BindableDeclaration.voidSignal(name : String) = signal(name, PredefinedType.void)
@Deprecated("", ReplaceWith("source(name, void)"))
fun BindableDeclaration.voidSource(name : String) = source(name, PredefinedType.void)
@Deprecated("", ReplaceWith("sink(name, void)"))
fun BindableDeclaration.voidSink(name : String) = sink(name, PredefinedType.void)

fun BindableDeclaration.call(name : String, paramType : IScalar, resultType : IScalar) = append(Task(name, paramType, resultType).write)
fun BindableDeclaration.callback(name : String, paramType : IScalar, resultType : IScalar) = append(Task(name, paramType, resultType).readonly)


fun BindableDeclaration.property(name : String, valueType : IType) = append(Property(name, valueType))
fun BindableDeclaration.property(name: String, defaultValue: Boolean) = append(Property(name, PredefinedType.bool, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: Int) = append(Property(name, PredefinedType.int, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: Double) = append(Property(name, PredefinedType.double, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: String) = append(Property(name, PredefinedType.string, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: Member.Const) = append(Property(name, PredefinedType.string, defaultValue))

fun BindableDeclaration.list(name : String, itemType : IType) = append(List(name, itemType))
fun BindableDeclaration.set(name : String, itemType : INonNullableScalar) = append(Set(name, itemType))
fun BindableDeclaration.map(name : String, keyType : INonNullableScalar, valueType: INonNullable) = append(Map(name, keyType, valueType))

//Following "fake" functions introduced to raise compile-time errors if you add reactive entities into structs.
//Suppose we have struct inside bindable declaration: Ext or Class. Then We must cheat Kotlin resolve
private const val ce_bindable  = "Can't be used inside scalars: structs, enums, etc."
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Struct.field(name : String, type : IBindable) : Nothing = error(ce_bindable)

@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.signal(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.source(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.sink(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.voidSignal(name : String) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.voidSource(name : String) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.voidSink(name : String) : Nothing = error(ce_bindable)


@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.property(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.list(name : String, itemType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.set(name : String, itemType : INonNullableScalar) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.map(name : String, keyType : INonNullableScalar, valueType: INonNullable) : Nothing = error(ce_bindable)


fun array(type: IBindable) = ArrayOfBindables(type)
fun array(type: IScalar)   = ArrayOfScalars(type)

fun immutableList(type: IBindable) = ImmutableListOfBindables(type)
fun immutableList(type: IScalar) = ImmutableListOfScalars(type)

//todo support immutableSets and immutableMaps for consistency

val INonNullableScalar.nullable : NullableScalar get() = NullableScalar(this)
val INonNullableBindable.nullable : NullableBindable get() = NullableBindable(this)

fun INonNullableScalar.interned(key: InternScope) : InternedScalar = InternedScalar(this, key)

fun Class.internRoot(scope: InternScope) {
    internRootForScopes.add(scope.keyName)
}

/**
 * Marks this key a light key. Light keys don't maintain a value set and send values un-interned.
 */
val Context.light: Context
    get() {
        isHeavyKey = false
        return this
    }

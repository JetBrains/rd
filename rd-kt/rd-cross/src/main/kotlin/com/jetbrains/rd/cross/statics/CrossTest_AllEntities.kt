package com.jetbrains.rd.cross.statics

import com.jetbrains.rd.util.reactive.fire
import demo.*
import java.util.*

@Suppress("ClassName")
@ExperimentalUnsignedTypes
object CrossTest_AllEntities {
    internal fun checkConstants() {
        assert(DemoModel.const_toplevel) { "DemoModel.const_toplevel" }
        assert(ConstUtil.const_enum == MyEnum.default) { "ConstUtil.const_enum == MyEnum.default" }
        assert(ConstUtil.const_string == "const_string_value") { "ConstUtil.const_string == \"const_string_value\"" }
        assert(Base.const_base == 'B') { "ConstUtil.const_string == \"const_string_value\"" }
    }

    internal fun fireAll(model: DemoModel, extModel: ExtModel) {
        model.boolean_property.set(false)

        model.boolean_array.set(booleanArrayOf(true, false, false))

        val scalar = MyScalar(false,
            13,
            32000,
            1_000_000_000,
            -2_000_000_000_000_000_000,
            3.14f,
            -123456789.012345678,
            UByte.MAX_VALUE.minus(1u).toUByte(),
            UShort.MAX_VALUE.minus(1u).toUShort(),
            UInt.MAX_VALUE.minus(1u),
            ULong.MAX_VALUE.minus(1u),
            MyEnum.kt,
            EnumSet.of(Flags.anyFlag, Flags.ktFlag),
            MyInitializedEnum.hundred
        )

        model.scalar.set(scalar)

        model.ubyte.set(UByte.MAX_VALUE.minus(13u).toUByte())

        model.ubyte_array.set(ubyteArrayOf(13u.toUByte(), (-1).toUByte()))

        model.list.add(1)
        model.list.add(3)

        model.set.add(13)

        model.mapLongToString[13] = "Kotlin"

        val valA = "Kotlin"
        val valB = "protocol"

        model.interned_string.set(valA)
        model.interned_string.set(valA)
        model.interned_string.set(valB)
        model.interned_string.set(valB)
        model.interned_string.set(valA)

        val derived = Derived("Kotlin instance")
        model.polymorphic.set(derived)

        val openDerived = OpenDerived("Kotlin instance open derived string ", "Kotlin instance open string")
        model.polymorphic_open.set(openDerived)

        val openClass = OpenClass("field")
        openClass.string.set("Kotlin test")

        model.date.set(Date(13_000)) // Thu Jan 01 03:00:13 MSK 1970

        model.enum.set(MyEnum.kt)

        extModel.checker.fire()

        model.struct_with_open_field.set(StructWithOpenStructField(OpenStructInField("", "", 123, "", "")))
    }
}
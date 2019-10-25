@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.jetbrains.rd.cross

import com.jetbrains.rd.cross.base.CrossTestKtServerBase
import com.jetbrains.rd.cross.util.trackAction
import com.jetbrains.rd.util.Date
import com.jetbrains.rd.util.reactive.fire
import demo.*
import java.util.*

@Suppress("unused")
class CrossTestKtServerAllEntities : CrossTestKtServerBase() {
    override fun start(args: Array<String>) {
        trackAction("Checking constant") {
            checkConstants()
        }

        queue {
            val model = trackAction("Creating DemoModel") {
                DemoModel.create(modelLifetime, protocol)
            }

            val extModel = trackAction("Creating ExtModel") {
                model.extModel
            }

            trackAction("Firing started") {
                fireAll(model, extModel)
            }
        }
    }

    private fun checkConstants() {
        assert(DemoModel.const_toplevel) { "DemoModel.const_toplevel" }
        assert(ConstUtil.const_enum == MyEnum.default) { "ConstUtil.const_enum == MyEnum.default" }
        assert(ConstUtil.const_string == "const_string_value") { "ConstUtil.const_string == \"const_string_value\"" }
        assert(Base.const_base == 'B') { "ConstUtil.const_string == \"const_string_value\"" }
    }

    private fun fireAll(model: DemoModel, extModel: ExtModel) {
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
            EnumSet.of(Flags.anyFlag, Flags.ktFlag)
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

        model.date.set(Date(13_000)) // Thu Jan 01 03:00:13 MSK 1970

        model.enum.set(MyEnum.kt)

        extModel.checker.fire()
    }
}

fun main(args: Array<String>) {
    CrossTestKtServerAllEntities().run(args)
}
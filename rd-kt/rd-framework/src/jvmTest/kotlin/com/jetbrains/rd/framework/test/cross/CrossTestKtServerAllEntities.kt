@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.jetbrains.rd.framework.test.cross

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.framework.test.cross.util.portFile
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.Date
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.ISource
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.println
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import demo.*
import java.io.File
import kotlin.test.assertEquals

lateinit var scheduler: IScheduler

fun server(lifetime: Lifetime, port: Int? = null): Protocol {
    scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
    return Protocol(Serializers(), Identities(IdKind.Server), scheduler,
            SocketWire.Server(lifetime, scheduler, port, "DemoServer"), lifetime)
}

@Volatile
var finished = false

const val testName = "CrossTestKtServerAllEntities"

fun main(args : Array<String>) {
    if (args.size != 1) {
        throw IllegalArgumentException("Wrong number of arguments for $testName:${args.size}")
    }
    val outputFileName = args[0]
    val outputFile = File(outputFileName)
    println("Test:$testName started, file=$outputFileName")

    val lifetimeDef = Lifetime.Eternal.createNested()
    val socketLifetimeDef = Lifetime.Eternal.createNested()

    val lifetime = lifetimeDef.lifetime
    val socketLifetime = socketLifetimeDef.lifetime

    val protocol = server(socketLifetime, NetUtils.findFreePort(0))
    portFile.printWriter().use { out ->
        out.println((protocol.wire as SocketWire.Server).port)
    }

    val printer = PrettyPrinter()

    checkConstants()

    scheduler.queue {
        val model = DemoModel.create(lifetime, protocol)
        val extModel = model.extModel

        adviseAll(lifetime, model, extModel, printer)
        fireAll(model, extModel)
    }

    spinUntil(10_000) { finished }
    spinUntil(1_000) { false }

    socketLifetimeDef.terminate()
    lifetimeDef.terminate()
    outputFile.parentFile.mkdirs()
    outputFile.printWriter().use { writer ->
        writer.println(printer)
    }
}

fun checkConstants() {
    assert(DemoModel.const_toplevel)
    assertEquals(ConstUtil.const_enum, MyEnum.default)
    assertEquals(ConstUtil.const_string, "const_string_value")
    assertEquals(Base.const_base, 'B')
}

private fun <T> PrettyPrinter.printIfRemoteChange(entity: ISource<T>, entityName: String, vararg values: Any) {
    if (!entity.isLocalChange) {
        println("***")
        println("$entityName:")
        values.forEach { value -> value.println(this) }
    }
}

private val <T> ISource<T>.isLocalChange
    get() = (this as? RdReactiveBase)?.isLocalChange == true

private fun adviseAll(lifetime: Lifetime, model: DemoModel, extModel: ExtModel, printer: PrettyPrinter) {
    model.boolean_property.advise(lifetime) {
        printer.printIfRemoteChange(model.boolean_property, "boolean_property", it)
    }

    model.boolean_array.advise(lifetime) {
        printer.printIfRemoteChange(model.boolean_array, "boolean_array", it)
    }

    model.scalar.advise(lifetime) {
        printer.printIfRemoteChange(model.scalar, "scalar", it)
    }

    model.ubyte.advise(lifetime) {
        printer.printIfRemoteChange(model.ubyte, "ubyte", it)
    }

    model.ubyte_array.advise(lifetime) {
        printer.printIfRemoteChange(model.ubyte_array, "ubyte_array", it)
    }

    model.list.advise(lifetime) {
        printer.printIfRemoteChange(model.list, "list", it)
    }

    model.set.advise(lifetime) { e, x ->
        printer.printIfRemoteChange(model.set, "set", e, x)
    }

    model.mapLongToString.advise(lifetime) {
        printer.printIfRemoteChange(model.mapLongToString, "mapLongToString", it)
    }

/*
    model.call.set { c ->
        printer.print("RdTask:")
        c.print(printer)

        c.toUpperCase().toString()
    }
*/

    model.interned_string.advise(lifetime) {
        printer.printIfRemoteChange(model.interned_string, "interned_string", it)
    }

    model.polymorphic.advise(lifetime) {
        printer.printIfRemoteChange(model.polymorphic, "polymorphic", it)
    }

    model.date.advise(lifetime) {
        printer.printIfRemoteChange(model.date, "date", it)
    }

    model.enum.advise(lifetime) {
        val entity = model.enum
        printer.printIfRemoteChange(entity, "enum", it)
        if (!entity.isLocalChange) {
            finished = true
        }
    }

    extModel.checker.advise(lifetime) {
        printer.printIfRemoteChange(extModel.checker, "extModel.checker", it)
    }
}

fun fireAll(model: DemoModel, extModel: ExtModel) {
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
            MyEnum.kt
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

//    val sync = model.callback.sync("Unknown")

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

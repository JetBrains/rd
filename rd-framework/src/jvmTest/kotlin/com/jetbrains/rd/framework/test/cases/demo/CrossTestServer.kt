@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
package com.jetbrains.rd.framework.test.cases.demo

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.test.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rd.util.string.PrettyPrinter
import com.jetbrains.rd.util.string.print
import com.jetbrains.rd.util.string.println
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import demo.*
import java.io.File

lateinit var scheduler: IScheduler

fun server(lifetime: Lifetime, port: Int? = null): Protocol {
    scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
    return Protocol(Serializers(), Identities(IdKind.Server), scheduler,
            SocketWire.Server(lifetime, scheduler, port, "DemoServer"), lifetime)
}

fun main() {
    val lifetimeDef = Lifetime.Eternal.createNested()
    val socketLifetimeDef = Lifetime.Eternal.createNested()

    val lifetime = lifetimeDef.lifetime
    val socketLifetime = socketLifetimeDef.lifetime

    val protocol = server(socketLifetime, NetUtils.findFreePort(0))
    val tmpDir = File(System.getProperty("java.io.tmpdir"))
    val file = File(tmpDir, "/rd/port.txt")
    file.parentFile.mkdirs()
    file.printWriter().use { out ->
        out.println((protocol.wire as SocketWire.Server).port)
    }

    val printer = PrettyPrinter()
    scheduler.queue {
        val model = DemoModel.create(lifetime, protocol)
        val extModel = model.extModel

        adviseAll(lifetime, model, extModel, printer)

        val res = fireAll(model, extModel)
        res.print(printer)
    }

    Thread.sleep(10_000)

    socketLifetimeDef.terminate()
    lifetimeDef.terminate()

    println(printer)
}

private fun adviseAll(lifetime: Lifetime, model: DemoModel, extModel: ExtModel, printer: PrettyPrinter) {
    model.boolean_property.advise(lifetime) {
        printer.print("BooleanProperty:")
        it.println(printer)
    }

    model.scalar.advise(lifetime) {
        printer.print("Scalar:")
        it.println(printer)
    }

    model.list.advise(lifetime) {
        printer.print("RdList:")
        it.println(printer)
    }

    model.set.advise(lifetime) { e, x ->
        printer.print("RdSet:")
        e.print(printer)
        x.println(printer)
    }

    model.mapLongToString.advise(lifetime) {
        printer.print("RdMap:")
        it.println(printer)
    }

/*
    model.call.set { c ->
        printer.print("RdTask:")
        c.print(printer)

        c.toUpperCase().toString()
    }
*/

    model.interned_string.advise(lifetime) {
        printer.print("Interned:")
        it.println(printer)
    }

    model.polymorphic.advise(lifetime) {
        printer.print("Polymorphic:")
        it.println(printer)
    }

    extModel.checker.advise(lifetime) {
        printer.print("ExtModel:Checker:")
        it.println(printer)
    }
}

fun fireAll(model: DemoModel, extModel: ExtModel): Int {
    model.boolean_property.set(false)

    val scalar = MyScalar(false,
            13,
            32000,
            1_000_000_000,
            -2_000_000_000_000_000_000,
            3.14f,
            -123456789.012345678,
            UShort.MAX_VALUE.minus(1u).toUShort(),
            UInt.MAX_VALUE.minus(1u),
            ULong.MAX_VALUE.minus(1u)
    )
    model.scalar.set(scalar)

    model.list.add(1)
    model.list.add(3)

    model.set.add(13)

    model.mapLongToString[13] = "Kotlin"

    val valA = "Kotlin"
    val valB = "protocol"

//    val sync = model.callback.sync("Unknown")
    val sync = 0

    model.interned_string.set(valA)
    model.interned_string.set(valA)
    model.interned_string.set(valB)
    model.interned_string.set(valB)
    model.interned_string.set(valA)

    val derived = Derived("Kotlin instance")
    model.polymorphic.set(derived)

    extModel.checker.fire()

    return sync
}

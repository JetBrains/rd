package com.jetbrains.rd.framework.test.cases.demo

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.test.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import org.example.DemoModel
import org.example.extModel
import java.io.File

var scheduler: IScheduler? = null

fun server(lifetime: Lifetime, port: Int? = null): Protocol {
    scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
    return Protocol(Serializers(), Identities(IdKind.Server), scheduler as SingleThreadScheduler,
            SocketWire.Server(lifetime, scheduler as SingleThreadScheduler, port, "DemoServer"), lifetime)
}

fun main() {
    val lifetimeDef = Lifetime.Eternal.createNested()
    val socketLifetimeDef = Lifetime.Eternal.createNested()

    val lifetime = lifetimeDef.lifetime
    val socketLifetime = socketLifetimeDef.lifetime


    val protocol = server(socketLifetime, NetUtils.findFreePort(0))
    File("C:\\temp\\port.txt").printWriter().use { out ->
        out.print((protocol.wire as SocketWire.Server).port)
    }

    scheduler?.queue {
        val model = DemoModel.create(lifetime, protocol);

        model.call.set { c ->
            c.toString()
        }

        model.scalar.advise(lifetime) {
            println(it)
        }

        val extModel = model.extModel

        extModel.checker.advise(lifetime) {
            println("check")
        }
    }
    Thread.sleep(500_000_000)
}
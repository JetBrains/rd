package com.jetbrains.rider.framework.test.cases.demo

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.impl.RdList
import com.jetbrains.rider.framework.test.util.NetUtils
import com.jetbrains.rider.framework.test.util.TestScheduler
//import com.jetbrains.rider.framework.test.util.NetUtils
//import com.jetbrains.rider.framework.test.util.TestScheduler
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.string.PrettyPrinter
import com.jetbrains.rider.util.string.printToString
import com.jetbrains.rider.util.threading.SingleThreadScheduler
import org.example.DemoModel
import org.example.MyList
import java.io.File

fun server(lifetime: Lifetime, port: Int? = null): Protocol {
    return Protocol(Serializers(), Identities(IdKind.Server), SingleThreadScheduler(lifetime, "ProtocolSingleThreadScheduler"),
            SocketWire.Server(lifetime, SingleThreadScheduler(lifetime, "WireSingleThreadScheduler"), port, "DemoServer"))
}

fun main(args: Array<String>) {
    val lifetimeDef = Lifetime.Eternal.createNested()
    val socketLifetimeDef = Lifetime.Eternal.createNested()

    val lifetime = lifetimeDef.lifetime
    val socketLifetime = socketLifetimeDef.lifetime


    val protocol = server(socketLifetime, NetUtils.findFreePort(0))
    File("C:\\temp\\port.txt").printWriter().use { out ->
        out.print((protocol.wire as SocketWire.Server).port)
    }

    val model = DemoModel.create(lifetime, protocol);
    //advise

    val printer = PrettyPrinter()
    model.scalar.advise(lifetime) {
        it.print(printer)
        println(printer.toString())

        val result = model.callCharToString.sync(it.byte_.toChar())
        println("result of sync call is: $result")
    }



    //set

    model.propertyList.set(MyList())

    model.propertyList.valueOrNull?.list?.advise(lifetime) {
        it.printToString()
    }

    model.propertyList.valueOrNull?.list?.add("A")
    model.propertyList.valueOrNull?.list?.add("B")
    model.propertyList.valueOrNull?.list?.add("C")

    /*model.propertyList.valueOrNull?.list?.add("D")
    model.propertyList.valueOrNull?.list?.add("E")
    model.propertyList.valueOrNull?.list?.add("F")*/
    Thread.sleep(500_000_000)
}

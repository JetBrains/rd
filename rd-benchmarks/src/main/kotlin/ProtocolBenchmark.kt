package com.jetbrans.rd.benchmarks

import com.jetbrains.rd.benchmarks.model.BenchmarkRootModel
import com.jetbrains.rider.framework.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.reactive.IScheduler
import java.util.concurrent.CountDownLatch

class ProtocolSide(wireCallback: (Lifetime, IScheduler) -> IWire) {
    val lifetime = LifetimeDefinition()
    private val serializers = Serializers()
    private val identities = Identities()
    private val scheduler = BenchmarkScheduler()
    val wire = wireCallback(lifetime.lifetime, scheduler)
    private val protocol = Protocol(serializers, identities, scheduler, wire)

    val model = BenchmarkRootModel.create(lifetime, protocol)
}

fun main() {
    val server = ProtocolSide { lifetime, scheduler -> SocketWire.Server(lifetime, scheduler, port = null, optId = "Server") }
    val client = ProtocolSide { lifetime, scheduler -> SocketWire.Client(lifetime, scheduler, port = server.wire.serverPort, optId = "Server") }

    val count = 1000
    val latch = CountDownLatch(1)
    client.model.count.view(client.lifetime) { _, value -> if (value == count) latch.countDown() }

    val startTime = System.nanoTime()
    for (i in 1..count) {
        server.model.count.set(i)
    }
    latch.await()
    val endTime = System.nanoTime()
    println("Completed in ${endTime - startTime} ns")
}

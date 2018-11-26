package com.jetbrans.rd.benchmarks

import com.jetbrains.rd.benchmarks.model.BenchmarkRootModel
import com.jetbrains.rider.framework.Identities
import com.jetbrains.rider.framework.Protocol
import com.jetbrains.rider.framework.Serializers
import com.jetbrains.rider.framework.SocketWire
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.threading.SynchronousScheduler
import java.util.concurrent.CountDownLatch

fun main() {
    val serverLifetime = LifetimeDefinition()
    val serverSerializers = Serializers()
    val serverIdentities = Identities()
    val serverScheduler = BenchmarkScheduler()
    val serverWire = SocketWire.Server(serverLifetime.lifetime, serverScheduler, port = null, optId = "Server")
    val serverProtocol = Protocol(serverSerializers, serverIdentities, serverScheduler, serverWire)

    val clientLifetime = LifetimeDefinition()
    val clientSerializers = Serializers()
    val clientIdentities = Identities()
    val clientScheduler = BenchmarkScheduler()
    val clientWire = SocketWire.Client(clientLifetime.lifetime, clientScheduler, serverWire.port, optId = "Client")
    val clientProtocol = Protocol(clientSerializers, clientIdentities, clientScheduler, clientWire)

    val serverModel = BenchmarkRootModel.create(serverLifetime, serverProtocol)
    val clientModel = BenchmarkRootModel.create(clientLifetime, clientProtocol)

    val count = 1000
    val latch = CountDownLatch(1)
    clientModel.count.view(clientLifetime) { _, value -> if (value == count) latch.countDown() }

    val startTime = System.nanoTime()
    for (i in 1..count) {
        serverModel.count.set(i)
    }
    latch.await()
    val endTime = System.nanoTime()
    println("Completed in ${endTime - startTime} ns")
}

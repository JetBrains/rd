package com.jetbrains.rd.cross.base

import com.jetbrains.rd.cross.util.portFile
import com.jetbrains.rd.cross.util.portFileClosed
import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.SpinWait
import java.nio.file.Files

abstract class CrossTestKtClientBase : CrossTestKtBase() {
    private fun client(lifetime: Lifetime, port: Int): IProtocol {
        scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
        return Protocol("DemoServer", Serializers(), Identities(IdKind.Client), scheduler,
            SocketWire.Client(lifetime, scheduler, port, "DemoClient"), lifetime)
    }

    init {
        println("Waiting for port being written in file=${portFile}")

        SpinWait.spinUntil(5_000) {
            Files.exists(portFileClosed.toPath())
        }

        val port =
            portFile.bufferedReader().use { out ->
                out.readLine()!!.toInt()
            }

        println("Port is $port")

        protocol = client(socketLifetime, port)
    }
}

package com.jetbrains.rd.cross.base

import com.jetbrains.rd.cross.util.logWithTime
import com.jetbrains.rd.cross.util.portFile
import com.jetbrains.rd.cross.util.portFileClosed
import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler

abstract class CrossTestKtServerBase : CrossTestKtBase() {
    private fun server(lifetime: Lifetime, port: Int? = null): IProtocol {
        scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
        return Protocol("DemoServer", Serializers(), Identities(IdKind.Server), scheduler,
                SocketWire.Server(lifetime, scheduler, port, "DemoServer"), lifetime)
    }

    init {
        protocol = server(socketLifetime, NetUtils.findFreePort(0))
        val port = (protocol.wire as SocketWire.Server).port

        portFile.printWriter().use { out ->
            out.println(port)
        }

        println("port=$port 's written in file=${portFile.absolutePath}")

        portFileClosed.createNewFile()
    }

    fun queue(action: () -> Unit) {
        scheduler.queue {
            try {
                action()
            } catch (e: Throwable) {
                logWithTime("Async error occurred")
                e.printStackTrace()
            }
        }
    }
}
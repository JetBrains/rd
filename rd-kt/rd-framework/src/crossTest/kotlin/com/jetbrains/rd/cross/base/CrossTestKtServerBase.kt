package com.jetbrains.rd.cross.base

import com.jetbrains.rd.cross.util.portFile
import com.jetbrains.rd.cross.util.portFileClosed
import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler

abstract class CrossTestKtServerBase : CrossTestKtBase() {
    private fun server(lifetime: Lifetime, port: Int? = null): IProtocol {
        scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
        return Protocol(Serializers(), Identities(IdKind.Server), scheduler,
                SocketWire.Server(lifetime, scheduler, port, "DemoServer"), lifetime)
    }

    init {
        protocol = server(socketLifetime, NetUtils.findFreePort(0))

        portFile.printWriter().use { out ->
            out.println((protocol.wire as SocketWire.Server).port)
        }
        portFileClosed.createNewFile()
    }
}
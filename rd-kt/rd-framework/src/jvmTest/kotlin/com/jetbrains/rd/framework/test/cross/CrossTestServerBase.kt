package com.jetbrains.rd.framework.test.cross

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.test.cross.util.portFile
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler

abstract class CrossTestServerBase : CrossTestBase() {
    fun server(lifetime: Lifetime, port: Int? = null): IProtocol {
        scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
        return Protocol(Serializers(), Identities(IdKind.Server), scheduler,
                SocketWire.Server(lifetime, scheduler, port, "DemoServer"), lifetime)
    }

    init {
        protocol = server(socketLifetime, NetUtils.findFreePort(0))

        portFile.printWriter().use { out ->
            out.println((protocol.wire as SocketWire.Server).port)
        }
    }
}
package com.jetbrains.rd.cross.base

import com.jetbrains.rd.cross.util.portFile
import com.jetbrains.rd.cross.util.portFileStamp
import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler

abstract class CrossTest_KtServer_Base : CrossTest_Kt_Base() {
    private fun server(lifetime: Lifetime, port: Int): IProtocol {
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

        portFileStamp.createNewFile()
    }
}
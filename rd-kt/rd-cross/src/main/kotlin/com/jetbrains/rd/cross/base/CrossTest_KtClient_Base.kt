package com.jetbrains.rd.cross.base

import com.jetbrains.rd.cross.util.portFile
import com.jetbrains.rd.cross.util.portFileStamp
import com.jetbrains.rd.framework.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.SpinWait
import java.nio.file.Files
import kotlin.system.exitProcess

@Suppress("ClassName")
abstract class CrossTest_KtClient_Base : CrossTest_Kt_Base() {
    private fun client(lifetime: Lifetime, port: Int): IProtocol {
        scheduler = SingleThreadScheduler(lifetime, "SingleThreadScheduler")
        return Protocol("DemoServer", Serializers(), Identities(IdKind.Client), scheduler,
            SocketWire.Client(lifetime, scheduler, port, "DemoClient"), lifetime)
    }

    init {
        println("Waiting for file created `$portFileStamp` and port written in file `${portFile}`")

        fun stampFileExists() = portFileStamp.exists()

        SpinWait.spinUntil(15_000, ::stampFileExists)

        if (!stampFileExists()) {
            System.err.println("Stamp file `$portFileStamp` wasn't created during timeout")
            exitProcess(1)
        }

        val port =
            portFile.bufferedReader().use { out ->
                out.readLine()!!.toInt()
            }

        println("Port is $port")

        protocol = client(socketLifetime, port)
    }
}

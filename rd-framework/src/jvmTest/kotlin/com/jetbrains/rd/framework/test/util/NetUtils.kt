package com.jetbrains.rd.framework.test.util

import java.net.InetAddress
import java.net.ServerSocket

object NetUtils {
    private fun isPortFree(port: Int?): Boolean {
        if (port == null) return true
        var socket: ServerSocket? = null
        try {
            socket = ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"))
            socket.reuseAddress = true
            return true
        } catch (e: Exception) {
            return false
        } finally {
            socket?.close()
        }
    }

    fun findFreePort(port: Int): Int {
        if (port > 0 && isPortFree(port))
            return port
        val socket1 = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
        val result = socket1.localPort
        socket1.reuseAddress = true;
        socket1.close();
        return result
    }

    internal fun findFreePorts(senderPort: Int, receiverPort: Int): Pair<Int, Int> {
        return Pair(
            findFreePort(senderPort),
            findFreePort(receiverPort)
        )
    }
}


data class PortPair private constructor(val senderPort: Int, val receiverPort: Int) {
    companion object {
        fun getFree(senderPort: Int = 55500, receiverPort: Int = 55501): PortPair {
            val (actualSenderPort, actualReceiverPort) = NetUtils.findFreePorts(
                senderPort,
                receiverPort
            )
            return PortPair(actualSenderPort, actualReceiverPort)
        }
    }
}


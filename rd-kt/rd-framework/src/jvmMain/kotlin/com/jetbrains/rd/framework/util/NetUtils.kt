package com.jetbrains.rd.framework.util

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket

object NetUtils {
    private fun isPortFree(port: Int?): Boolean {
        if (port == null) return true
        var socket: ServerSocket? = null
        try {
            socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress("127.0.0.1", port), 0)
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
        val socket1 = ServerSocket()
        socket1.reuseAddress = true
        socket1.bind(InetSocketAddress("127.0.0.1", 0), 0)
        socket1.close()
        val result = socket1.localPort
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


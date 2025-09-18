package com.jetbrains.rd.framework.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SocketChannel
import java.nio.channels.WritableByteChannel

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

fun SocketChannel.getInputStream(): InputStream {
    val ch = this
    return Channels.newInputStream(object : ReadableByteChannel {
        override fun read(dst: ByteBuffer?): Int {
            return ch.read(dst)
        }

        override fun close() {
            ch.close()
        }

        override fun isOpen(): Boolean {
            return ch.isOpen
        }
    })
}

fun SocketChannel.getOutputStream(): OutputStream {
    val ch = this
    return Channels.newOutputStream(object : WritableByteChannel {
        override fun write(src: ByteBuffer?): Int {
            return ch.write(src)
        }

        override fun close() {
            ch.close()
        }

        override fun isOpen(): Boolean {
            return ch.isOpen
        }
    })
}
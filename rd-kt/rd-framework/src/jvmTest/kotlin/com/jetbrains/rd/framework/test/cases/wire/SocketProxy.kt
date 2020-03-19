package com.jetbrains.rd.framework.test.cases.wire

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.framework.serverPort
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.lifetime.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import kotlin.concurrent.thread


class SocketProxy internal constructor(val id: String, val lifetime: Lifetime, private val serverPort: Int) {
    private val logger: Logger

    private var _port: Int? = null

    val port: Int
        get() = _port ?: throw IllegalStateException("SocketProxy was not started")

    var latency: Duration = Duration.ZERO

    companion object {
        private const val DefaultBufferSize = 16370
    }

    private val serverToClientBuffer = ByteArray(DefaultBufferSize)
    private val clientToServerBuffer = ByteArray(DefaultBufferSize)

    private val serverToClientLifetime: SequentialLifetimes
    private val clientToServerLifetime: SequentialLifetimes
    private lateinit var proxyServer: Socket
    private lateinit var proxyClient: ServerSocket

    init {
        logger = getLogger("SocketProxy.$id")
        serverToClientLifetime = SequentialLifetimes(lifetime).apply { next() }
        clientToServerLifetime = SequentialLifetimes(lifetime).apply { next() }
        lifetime.onTermination {
            _port = null

            stopServerToClientMessaging()
            stopClientToServerMessaging()
        }
    }

    internal constructor(id: String, lifetime: Lifetime, protocol: IProtocol) :
        this(id, lifetime, protocol.wire.serverPort)

    fun start() {
        fun setSocketOptions(acceptedClient: Socket) {
            acceptedClient.tcpNoDelay = true
        }

        try {
            logger.info { "Creating proxies for server and client..." }
            proxyServer = Socket(InetAddress.getLoopbackAddress(), serverPort).also { lifetime.onTermination(it) }
            proxyClient = SocketWire.Server.createServerSocket(lifetime, 0, false).also { lifetime.onTermination(it) }

            setSocketOptions(proxyServer)
            _port = proxyClient.localPort
            logger.info { "Proxies for server on port $serverPort and client on port $port created successfully" }

            val thread = thread {
                try {
                    val acceptedClient = proxyClient.accept()!!
                    lifetime.onTermination(acceptedClient)
                    setSocketOptions(acceptedClient)
                    logger.info { "New client connected on port $port" }

                    connect(proxyServer, acceptedClient)
                } catch (e: Exception) {
                    logger.error("Couldn't accept socket", e)
                }
            }

            lifetime.onTermination { thread.join(500) }
        } catch (e: Exception) {
            logger.error("Failed to create proxies", e)
        }
    }

    private fun connect(proxyServer: Socket, proxyClient: Socket) {
        try {
            logger.info { "Connecting proxies between themselves..." }

            val task1 = thread {
                logger.info { "Server to client messaging started" }
                messaging("Server to client", proxyServer.inputStream, proxyClient.outputStream, serverToClientBuffer, serverToClientLifetime)
            }

            val task2 = thread {
                logger.info { "Client to server messaging started" }
                messaging("Client to server", proxyClient.inputStream, proxyServer.outputStream, clientToServerBuffer, clientToServerLifetime)
            }

            lifetime.onTermination {
                task1.join()
                task2.join()
            }

            logger.info { "Async transferring messages started" }
        } catch (e: Exception) {
            logger.error("Connecting proxies failed", e)
        }
    }

    private fun messaging(id: String, source: InputStream, destination: OutputStream, buffer: ByteArray, lifetimes: SequentialLifetimes) {
        while (lifetime.isAlive) {
            logger.info { "$id: Lifetime is alive, messaging in process..." }
            try {
                val length = source.read(buffer)
                if (length == 0) {
                    logger.info { "${id}: Connection lost" }
                    break
                }
                logger.info { "${id}: Message of length: $length was read" }
                if (!lifetimes.isTerminated) {
                    Thread.sleep(latency.toMillis())
                    destination.write(buffer, 0, length)
                    logger.info { "$id: Message of length: $length was written" }
                } else {
                    logger.info { "$id: Message of length $length was not transferred, because lifetime was terminated" }
                }
            } catch (e: Exception) {
                logger.error("${id}: Messaging failed", e)
            }
        }
    }


    fun stopClientToServerMessaging() {
        clientToServerLifetime.terminateCurrent()
    }

    fun startClientToServerMessaging() {
        clientToServerLifetime.next()
    }

    fun stopServerToClientMessaging() {
        serverToClientLifetime.terminateCurrent()
    }

    fun startServerToClientMessaging() {
        serverToClientLifetime.next()
    }
}


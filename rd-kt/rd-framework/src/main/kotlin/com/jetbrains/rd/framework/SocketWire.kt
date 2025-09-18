package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.WireAddress.Companion.toSocketAddress
import com.jetbrains.rd.framework.base.WireBase
import com.jetbrains.rd.framework.util.getInputStream
import com.jetbrains.rd.framework.util.getOutputStream
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.threading.ByteBufferAsyncProcessor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

private fun InputStream.readByteArray(a : ByteArray): Boolean {
    var pos = 0
    while (pos < a.size) {
        val b = read(a, pos, a.size - pos)
        if (b == -1)
            return false
        pos += b
    }
    return true
}


private fun InputStream.readInt32() : Int? {
    val b1 = read().apply { if (this < 0) return null }
    val b2 = read().apply { if (this < 0) return null }
    val b3 = read().apply { if (this < 0) return null }
    val b4 = read().apply { if (this < 0) return null }
    val res = b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)

    return res
}

private fun InputStream.readInt64() : Long? {
    val b1 = read().toLong().apply { if (this < 0) return null }
    val b2 = read().toLong().apply { if (this < 0) return null }
    val b3 = read().toLong().apply { if (this < 0) return null }
    val b4 = read().toLong().apply { if (this < 0) return null }
    val b5 = read().toLong().apply { if (this < 0) return null }
    val b6 = read().toLong().apply { if (this < 0) return null }
    val b7 = read().toLong().apply { if (this < 0) return null }
    val b8 = read().toLong().apply { if (this < 0) return null }
    val res = b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24) or
            (b5 shl 32) or (b6 shl 40) or (b7 shl 48) or (b8 shl 56)


    return res
}


class SocketWire {
    companion object {
        val timeout: Duration = Duration.ofMillis(500)
        private const val ack_msg_len: Int = -1
        private const val ping_len: Int = -2
        private const val pkg_header_len = 12
        private const val default_max_msg_len = 300_000_000
        const val disconnectedPauseReason = "Socket not connected"
        const val maximumHeartbeatDelay = 3

        private fun connectionEstablished(timeStamp : Int, notionTimestamp: Int) = timeStamp - notionTimestamp <= maximumHeartbeatDelay
    }

    abstract class Base protected constructor(val id: String, private val lifetime: Lifetime, scheduler: IScheduler) : WireBase() {

        protected val logger: Logger = getLogger(this::class)
        val socketProvider = OptProperty<SocketChannel>()

        private lateinit var output : OutputStream
        private lateinit var socketInput : InputStream
        private lateinit var pkgInput : InputStream

        protected val sendBuffer = ByteBufferAsyncProcessor("$id/Sender", processor = ::send0)

        private val threadLocalBufferArray = ThreadLocal.withInitial { UnsafeBuffer(ByteArray(16384)) }

        val acktor: ExecutorService = Executors.newSingleThreadExecutor()
        private fun sendAck(seqn: Long) {
            catchAndDrop { acktor.execute { sendAck0(seqn) } }
        }


        protected val lock = Object()

        @field:Volatile
        var maxMessageLength = default_max_msg_len
            set(value) {
                if (value < default_max_msg_len) {
                    logger.warn { "$value is less than default value ($default_max_msg_len). This is not allowed." }
                } else {
                    field = value
                }
            }

        private var maxReceivedSeqn : Long = 0

        init {
            sendBuffer.pause(disconnectedPauseReason)
            sendBuffer.start()

            connected.advise(lifetime) { heartbeatAlive.value = it }

            socketProvider.advise(lifetime) { socketChannel ->

                logger.debug { "$id : connected" }

                output = socketChannel.getOutputStream()
                socketInput = socketChannel.getInputStream().buffered()
                pkgInput = PkgInputStream(socketInput)

                sendBuffer.reprocessUnacknowledged()
                sendBuffer.resume(disconnectedPauseReason)

                val heartbeatJob = startHeartbeat()

                scheduler.queue { connected.value = true }

                try {
                    receiverProc(socketChannel)
                } finally {
                    scheduler.queue { connected.value = false }
                    heartbeatJob.cancel()
                    sendBuffer.pause(disconnectedPauseReason)
                    catchAndDrop { socketChannel.close() }
                }
            }
        }

        private fun startHeartbeat() = GlobalScope.launch {
            while (true) {
                delay(heartbeatIntervalMs)
                ping()
            }
        }

        private fun ping() {
            try {
                if (!connectionEstablished(currentTimeStamp, counterpartNotionTimestamp)) {
                    if (heartbeatAlive.value) { // only on change
                        logger.trace {
                            "Disconnect detected while sending PING $id: " +
                                "currentTimeStamp: $currentTimeStamp, " +
                                "counterpartTimestamp: $counterpartTimestamp, " +
                                "counterpartNotionTimestamp: $counterpartNotionTimestamp"
                        }
                    }
                    heartbeatAlive.value = false
                }

                synchronized(socketSendLock) {
                    sendPingPkgHeader.reset()
                    sendPingPkgHeader.writeInt(ping_len)
                    sendPingPkgHeader.writeInt(currentTimeStamp)
                    sendPingPkgHeader.writeInt(counterpartTimestamp)
                    output.write(sendPingPkgHeader.getArray())
                }

                ++currentTimeStamp
            } catch (t: Throwable) {
                if (logger.isEnabled(LogLevel.Debug)) {
                    logger.log(LogLevel.Debug, "${id}: ${t.javaClass} raised during PING", t)
                }
            }
        }


        private fun receiverProc(socket: SocketChannel) {
            while (lifetime.isAlive) {
                try {
                    if (!readMsg()) {
                        logger.debug { "$id: Connection was gracefully shutdown" }
                        break
                    }
                } catch (ex: Throwable) {
                    when (ex) {
                        is SocketException, is EOFException, is IOException -> logger.debug {"Exception in SocketWire.Receive:  $id: $ex" }
                        else -> logger.error("$id caught processing", ex)
                    }

                    break
                }
            }
        }

        private fun readMsg() : Boolean {

            val seqnAtStart = maxReceivedSeqn

            val len = pkgInput.readInt32() ?: return false
            require(len > 0) {"len > 0: $len"}
            val tooBigMessage = assertLength(len)

            val data = ByteArray(len)
            if (!pkgInput.readByteArray(data))
                return false

            if (maxReceivedSeqn > seqnAtStart) {
                val responseSeqn = maxReceivedSeqn
                sendAck(responseSeqn)
            }

            val unsafeBuffer = UnsafeBuffer(data)
            val id = RdId.read(unsafeBuffer)
            messageBroker.dispatch(id, unsafeBuffer, tooBigMessage)

            return true
        }


        inner class PkgInputStream(private val stream: InputStream) : InputStream() {
            var pkg: ByteArray = ByteArray(0)
            var pos: Int = 0

            override fun read(): Int {
                if (pos < pkg.size)
                    return pkg[pos++].toInt() and 0xff

                while (true) {
                    val len = stream.readInt32() ?: return -1

                    if (len == ping_len) {
                        val receivedTimestamp = stream.readInt32() ?: return -1
                        val receivedCounterpartTimestamp = stream.readInt32() ?: return -1

                        counterpartTimestamp = receivedTimestamp
                        counterpartNotionTimestamp = receivedCounterpartTimestamp

                        if (connectionEstablished(currentTimeStamp, counterpartNotionTimestamp)) {
                            if (!heartbeatAlive.value) { // only on change
                                logger.trace {
                                    "Connection is alive after receiving PING $id: " +
                                        "receivedTimestamp: $receivedTimestamp, " +
                                        "receivedCounterpartTimestamp: $receivedCounterpartTimestamp" +
                                        "currentTimeStamp: $currentTimeStamp, " +
                                        "counterpartTimestamp: $counterpartTimestamp, " +
                                        "counterpartNotionTimestamp: $counterpartNotionTimestamp"
                                }
                            }
                            heartbeatAlive.value = true
                        }
                        continue
                    }

                    val seqn = stream.readInt64() ?: return -1
                    if (len == ack_msg_len) {
                        sendBuffer.acknowledge(seqn)
                    }
                    else {
                        require(len > 0) {"len > 0: $len"}
                        assertLength(len)

                        pkg = ByteArray(len)
                        pos = 0
                        if (!stream.readByteArray(pkg)) {
                            pkg = ByteArray(0)
                            return -1
                        }

                        if (seqn > maxReceivedSeqn) {
                            maxReceivedSeqn = seqn
                            return pkg[pos++].toInt() and 0xff
                        } else
                            sendAck(seqn)
                    }

                }
            }

        }

        private fun assertLength(len: Int): String? {
            if (len <= maxMessageLength) return null

            val message = "Possible OOM: array_len=$len(0x${len.toString(16)}), allowed_len=$maxMessageLength(0x${maxMessageLength.toString(16)})"
            logger.warn { message }

            return message;
        }

        private fun sendAck0(seqn: Long) {
            try {
                ackPkgHeader.reset()
                ackPkgHeader.writeInt(ack_msg_len)
                ackPkgHeader.writeLong(seqn)

                synchronized(socketSendLock) {
                    output.write(ackPkgHeader.getArray(), 0, pkg_header_len)
                }
            }
            catch(ex: ClosedChannelException) {
                logger.warn { "$id: Exception raised during ACK, seqn = $seqn" }
            }
            catch (ex: SocketException) {
                logger.warn { "$id: Exception raised during ACK, seqn = $seqn" }
            }
        }



        private var sentSeqn = 0L
        private val socketSendLock = Any()
        private val sendPkgHeader = createAbstractBuffer()
        private val ackPkgHeader = createAbstractBuffer()

        /**
         * Timestamp of this wire which increases at intervals of [heartbeatIntervalMs].
         */
        private var currentTimeStamp = 0

        /**
         * Actual notion about counterpart's [currentTimeStamp].
         */
        private var counterpartTimestamp = 0

        /**
         * The latest received counterpart's notion of this wire's [currentTimeStamp].
         */
        private var counterpartNotionTimestamp = 0

        private val sendPingPkgHeader = createAbstractBuffer()

        private fun send0(chunk: ByteBufferAsyncProcessor.Chunk) {
            try {
                if (chunk.isNotProcessed)
                    chunk.seqn = ++sentSeqn


                sendPkgHeader.reset()
                sendPkgHeader.writeInt(chunk.ptr)
                sendPkgHeader.writeLong(chunk.seqn)

                synchronized(socketSendLock) {
                    logger.trace { "Send package with seqn ${chunk.seqn}" }
                    output.write(sendPkgHeader.getArray(), 0, pkg_header_len)
                    output.write(chunk.data, 0, chunk.ptr)
                }
            } catch (ex: SocketException) {
                sendBuffer.pause(disconnectedPauseReason)
            }
            catch (ex: IOException) {
                sendBuffer.pause(disconnectedPauseReason)
            }
        }

        override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
            require(!id.isNull) { "id mustn't be null" }

            val unsafeBuffer = threadLocalBufferArray.get()
            val initialPosition = unsafeBuffer.position
            try {

                unsafeBuffer.writeInt(0) //placeholder for length

                id.write(unsafeBuffer) //write id
                contexts.writeCurrentMessageContext(unsafeBuffer)
                writer(unsafeBuffer) //write rest

                val len = unsafeBuffer.position - initialPosition

                if (len > maxMessageLength) {
                    val entry = messageBroker.tryGetById(id)
                    logger.error { "Too long message: $len. ${entry?.location?.toString() ?: "<NULL>"}" }
                }

                unsafeBuffer.position = initialPosition
                unsafeBuffer.writeInt(len - 4)

                val bytes = unsafeBuffer.getArray()
                sendBuffer.put(bytes, initialPosition, len)
            } finally {
                if (initialPosition == 0)
                    unsafeBuffer.reset() // apply shrinking logic
                else
                    unsafeBuffer.position = initialPosition
            }
        }
    }


    class Client internal constructor(lifetime : Lifetime, scheduler: IScheduler, endpoint: SocketAddress, optId: String? = null) : Base(optId ?:"ClientSocket", lifetime, scheduler) {

        constructor(lifetime : Lifetime, scheduler: IScheduler, wireAddress: WireAddress, optId: String? = null) : this(lifetime, scheduler, wireAddress.toSocketAddress(), optId)

        constructor(
            lifetime: Lifetime,
            scheduler: IScheduler,
            port: Int,
            optId: String? = null,
            hostAddress: InetAddress = InetAddress.getLoopbackAddress()
        ) : this(lifetime, scheduler, InetSocketAddress(hostAddress, port), optId)

        init {

            var socket : SocketChannel? = null
            val thread = thread(name = id, isDaemon = true) {
                try {
                    var lastReportedErrorHash = 0
                    while (lifetime.isAlive) {
                        try {
                            val s = when (endpoint) {
                                is InetSocketAddress -> SocketChannel.open().apply { setOption(StandardSocketOptions.TCP_NODELAY, true) }
                                is UnixDomainSocketAddress -> SocketChannel.open(StandardProtocolFamily.UNIX)
                                else -> throw IllegalArgumentException("Only InetSocketAddress and UnixDomainSocketAddress are supported, got: $endpoint")
                            }

                            // On windows connect will try to send SYN 3 times with interval of 500ms (total time is 1second)
                            // Connect timeout doesn't work if it's more than 1 second. But we don't need it because we can close socket any moment.

                            //https://stackoverflow.com/questions/22417228/prevent-tcp-socket-connection-retries
                            //HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\TcpMaxConnectRetransmissions
                            logger.debug { "$id : connecting to $endpoint" }
                            s.connect(endpoint)

                            synchronized(lock) {
                                if (!lifetime.isAlive) {
                                    logger.debug { "$id : connected, but lifetime is already canceled, closing socket"}
                                    catchAndDrop {s.close()}
                                    return@thread
                                }
                                else
                                    socket = s
                            }

                            socketProvider.set(s)
                            logger.debug { "$id: receiverProc finished " }

                        } catch (e: ConnectException) {
                            val errorSource = Pair(e.message, e.stackTraceToString())
                            val errorHashCode = errorSource.hashCode()
                            if (lastReportedErrorHash != errorHashCode) {
                                lastReportedErrorHash = errorHashCode
                                if (logger.isEnabled(LogLevel.Debug)) {
                                    logger.log(
                                        LogLevel.Debug,
                                        "$id: connection error for endpoint $endpoint.",
                                        e
                                    )
                                }
                            } else {
                                logger.debug { "$id: connection error for endpoint $endpoint (${e.message})." }
                            }

                            val shouldReconnect = synchronized(lock) {
                                if (lifetime.isAlive) {
                                    lock.wait(timeout.toMillis())
                                    lifetime.isAlive
                                } else false

                            }
                            if (shouldReconnect)
                                continue
                            else
                                break
                        }
                    }

                } catch (ex: SocketException) {
                    logger.info {"$id: closed with exception: $ex"}
                }
                catch (ex: ClosedChannelException) {
                    logger.info {"$id: closed with exception: $ex"}
                }
                catch (ex: Throwable) {
                    logger.error("$id: unhandled exception.", ex)
                } finally {
                    logger.debug { "$id: terminated." }
                }
            }


            lifetime += {
                logger.info {"$id: start terminating lifetime"}

                logger.debug {"$id: shutting down ack sending executor"}
                acktor.shutdown()

                val sendBufferStopped = sendBuffer.stop(timeout)
                logger.debug{"$id: send buffer stopped, success: $sendBufferStopped"}

                synchronized(lock) {
                    logger.debug{"$id: closing socket"}
                    catch {socket?.close()}
                    lock.notifyAll()
                }

                logger.debug { "$id: waiting for receiver thread" }
                catch { thread.join(timeout.toMillis()) }
                logger.info { "$id: termination finished" }
            }

        }
    }


    class Server internal constructor(lifetime : Lifetime, scheduler: IScheduler, ss: ServerSocketChannel, optId: String? = null, allowReconnect: Boolean) : Base(optId ?:"ServerSocket", lifetime, scheduler) {
        val wireAddress: WireAddress = WireAddress.fromSocketAddress(ss.localAddress)

        @Deprecated("Use wireAddress instead")
        val port : Int = (wireAddress as? WireAddress.TcpAddress)?.port ?: -1

        companion object {
            internal fun createServerSocket(lifetime: Lifetime, port : Int?, allowRemoteConnections: Boolean) : ServerSocketChannel {
                val address = if (allowRemoteConnections) InetAddress.getByName("0.0.0.0") else InetAddress.getByName("127.0.0.1")
                val portToBind = port ?: 0
                val res = ServerSocketChannel.open().apply { setOption(StandardSocketOptions.SO_REUSEADDR, true) }
                res.bind(InetSocketAddress(address, portToBind), 0)
                lifetime.onTermination {
                    res.close()
                }
                return res
            }

            internal fun createServerSocket(lifetime: Lifetime, endpoint: SocketAddress) : ServerSocketChannel {
                val socketChannel = when (endpoint) {
                    is InetSocketAddress -> ServerSocketChannel.open().apply { setOption(StandardSocketOptions.SO_REUSEADDR, true) }
                    is UnixDomainSocketAddress -> ServerSocketChannel.open(StandardProtocolFamily.UNIX)
                    else -> throw IllegalArgumentException("Only InetSocketAddress and UnixDomainSocketAddress are supported, got: $endpoint")
                }

                socketChannel.bind(endpoint, 0)
                lifetime.onTermination {
                    socketChannel.close()
                }
                return socketChannel
            }
        }

        constructor (lifetime : Lifetime, scheduler: IScheduler, port : Int?, optId: String? = null, allowRemoteConnections: Boolean = false) : this(lifetime, scheduler, createServerSocket(lifetime, port, allowRemoteConnections), optId, allowReconnect = true)
        constructor (lifetime : Lifetime, scheduler: IScheduler, wireAddress: WireAddress, optId: String? = null) : this(lifetime, scheduler, wireAddress.toSocketAddress(), optId)

        internal constructor (lifetime : Lifetime, scheduler: IScheduler, endpoint: SocketAddress, optId: String? = null) : this(lifetime, scheduler, createServerSocket(lifetime, endpoint), optId, allowReconnect = true)

        init {
            var socket : SocketChannel? = null
            val thread = thread(name = id, isDaemon = true) {
                logger.catch {
                    while (lifetime.isAlive) {
                        try {
                            logger.debug { "$id: listening ${wireAddress}" }
                            val s = ss.accept() //could be terminated by close
                            if(s.localAddress is InetSocketAddress)
                                s.setOption(StandardSocketOptions.TCP_NODELAY, true)

                            synchronized(lock) {
                                if (!lifetime.isAlive) {
                                    logger.debug { "$id : connected, but lifetime is already canceled, closing socket" }
                                    catch { s.close() }
                                    return@thread
                                } else
                                    socket = s
                            }

                            socketProvider.set(s)
                        }
                        catch (ex: Exception) {
                            when(ex) {
                                is SocketException, is AsynchronousCloseException, is ClosedChannelException  -> {
                                    logger.debug { "$id closed with exception: $ex" }
                                }
                                else -> logger.error("$id closed with exception", ex)
                            }
                        }

                        if (!allowReconnect) {
                            logger.debug { "$id: finished listening on ${wireAddress}." }
                            break
                        } else {
                            logger.debug { "$id: waiting for reconnection on ${wireAddress}." }
                        }
                    }
                }

                logger.debug { "$id: terminated." }
            }


            lifetime.onTerminationIfAlive {
                logger.info {"$id: start terminating lifetime" }

                logger.debug {"$id: shutting down ack sending executor"}
                acktor.shutdown()

                val sendBufferStopped = sendBuffer.stop(timeout)
                logger.debug {"$id: send buffer stopped, success: $sendBufferStopped"}

                catch {
                    synchronized(lock) {
                        logger.debug {"$id: closing socket"}
                        socket?.close()
                    }
                }

                catch { thread.join(timeout.toMillis()) }
                logger.info{"$id: termination finished"}

            }
        }
    }



    data class WireParameters(val scheduler: IScheduler, val id: String?)
    class ServerFactory private constructor(lifetime : Lifetime, wireParametersFactory: () -> WireParameters, port : Int?, allowRemoteConnections: Boolean, set: ViewableSet<Server>) : IViewableSet<Server> by set {

        constructor(lifetime : Lifetime, wireParametersFactory: () -> WireParameters, port : Int?, allowRemoteConnections: Boolean = false) :
            this(lifetime, wireParametersFactory, port, allowRemoteConnections, ViewableSet<Server>())

        constructor(lifetime : Lifetime, scheduler: IScheduler, port : Int?, allowRemoteConnections: Boolean = false) :
                this(lifetime, { WireParameters(scheduler, null) }, port, allowRemoteConnections, ViewableSet<Server>())


        val localPort: Int

        init {
            val ss = Server.createServerSocket(lifetime, port, allowRemoteConnections)
            localPort = (ss.localAddress as? InetSocketAddress)?.port ?: -1

            fun rec() {
                lifetime.executeIfAlive {
                    val (scheduler, optId) = wireParametersFactory()
                    val s = Server(lifetime, scheduler, ss, optId, allowReconnect = false)
                    s.connected.whenTrue(lifetime) { lt ->
                        set.addUnique(lt, s)
                        rec()
                    }
                }
            }

            rec()
        }
    }
}

sealed class WireAddress {
    data class TcpAddress(val address: InetAddress, val port: Int): WireAddress()
    data class UnixAddress(val path: Path): WireAddress()

    internal companion object {
        fun fromSocketAddress(socketAddress: SocketAddress): WireAddress = when (socketAddress) {
            is InetSocketAddress -> TcpAddress(socketAddress.address, socketAddress.port)
            is UnixDomainSocketAddress -> UnixAddress(socketAddress.path)
            else -> error("Unknown socket address type: $socketAddress")
        }

        fun WireAddress.toSocketAddress() = when (this) {
            is TcpAddress -> InetSocketAddress(address, port)
            is UnixAddress -> UnixDomainSocketAddress.of(path)
        }
    }
}
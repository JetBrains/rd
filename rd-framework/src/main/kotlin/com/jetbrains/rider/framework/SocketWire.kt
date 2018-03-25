package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.AbstractBuffer
import com.jetbrains.rider.framework.base.WireBase
import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.Trigger
import com.jetbrains.rider.util.threading.ByteArraySlice
import com.jetbrains.rider.util.threading.ByteBufferAsyncProcessor
import com.jetbrains.rider.util.threading.write
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.time.Duration
import kotlin.concurrent.thread

private fun InputStream.readByteArray(): ByteArray {
    val b1 = read()
    val b2 = read()
    val b3 = read()
    val b4 = read()
    val len = b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)

    if (len < 0)
        throw SocketException("End of stream was reached")

    if (len > 300_000_000) {
        throw IllegalStateException("Possible OOM: array_len=$len(0x${len.toString(16)})")
    }

    val bytes = ByteArray(len)
    var read = 0
    while(read < bytes.size) {
        val result = read(bytes, read, bytes.size - read)
        if (result < 0)
            throw SocketException("End of stream was reached")
        read += result
    }
    return bytes
}

class SocketWire {
    companion object {
        val timeout: Duration = Duration.ofMillis(500)
    }

    abstract class Base protected constructor(val id: String, private val lifetime: Lifetime, scheduler: IScheduler) : WireBase(scheduler) {

        protected val logger: Logger = getLogger(this::class)
        protected val socketProvider = Trigger<Socket>()

        private lateinit var output : OutputStream
        private lateinit var input : InputStream

        protected val sendBuffer = ByteBufferAsyncProcessor(id+"-AsyncSendProcessor") { send0(it) }

        private val threadLocalSendByteArray = ThreadLocal.withInitial { ByteArray(16384) }

        protected val lock = Object()

        init {
            socketProvider.advise(lifetime) { socket ->

                synchronized(lock) {
                    if (lifetime.isTerminated)
                        return@advise

                    output = socket.outputStream
                    input = socket.inputStream.buffered()

                    sendBuffer.start()
                }

                receiverProc(socket)
            }
        }

        private fun receiverProc(socket: Socket) {
            while (!lifetime.isTerminated) {
                try {
                    if (!socket.isConnected) {
                        logger.debug {"Stop receive messages because socket disconnected" }
                        sendBuffer.terminate()
                        break
                    }

                    val bytes = input.readByteArray()
                    assert(bytes.size >= 4)
                    val unsafeBuffer = UnsafeBuffer(bytes)
                    val id = RdId.read(unsafeBuffer)
                    messageBroker.dispatch(id, unsafeBuffer)

                } catch (ex: Throwable) {
                    when (ex) {
                        is SocketException, is EOFException -> logger.debug {"Exception in SocketWire.Receive:  $id: $ex" }
                        else -> logger.error("$id caught processing", ex)
                    }

                    sendBuffer.terminate()
                    break
                }
            }
        }


        private fun send0(msg: ByteArraySlice) {
            try {
                output.write(msg)
            } catch (ex: SocketException) {
                sendBuffer.terminate()
            }
        }


        override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
            require(!id.isNull) { "id mustn't be null" }

            val unsafeBuffer = UnsafeBuffer(threadLocalSendByteArray.get())
            try {
                unsafeBuffer.writeInt(0) //placeholder for length

                id.write(unsafeBuffer) //write id
                writer(unsafeBuffer) //write rest

                val len = unsafeBuffer.position

                unsafeBuffer.rewind()
                unsafeBuffer.writeInt(len - 4)

                val bytes = unsafeBuffer.getArray()!!
                threadLocalSendByteArray.set(bytes)
                sendBuffer.put(bytes, 0, len)
            } finally {
                unsafeBuffer.close()
            }
        }
    }


    class Client(lifetime : Lifetime, scheduler: IScheduler, port : Int, optId: String? = null) : Base(optId ?:"ClientSocket", lifetime, scheduler) {

        init {

            var socket : Socket? = null
            val thread = thread(name = id, isDaemon = true) {
                try {
                    while (!lifetime.isTerminated) {
                        try {
                            val s = Socket()
                            s.tcpNoDelay = true

                            // On windows connect will try to send SYN 3 times with interval of 500ms (total time is 1second)
                            // Connect timeout doesn't work if it's more than 1 second. But we don't need it because we can close socket any moment.

                            //https://stackoverflow.com/questions/22417228/prevent-tcp-socket-connection-retries
                            //HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters\TcpMaxConnectRetransmissions
                            s.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port))

                            synchronized(lock) {
                                if (lifetime.isTerminated)
                                    catch {s.close()}
                                else
                                    socket = s
                            }

                            socketProvider.set(s)

                        } catch (e: ConnectException) {
                            val shouldReconnect = synchronized(lock) {
                                if (!lifetime.isTerminated) {
                                    lock.wait(timeout.toMillis())
                                    !lifetime.isTerminated
                                } else false

                            }
                            if (shouldReconnect) continue
                        }
                        break
                    }

                } catch (ex: SocketException) {
                    logger.info {"$id: closed with exception: $ex"}
                }
            }


            lifetime += {
                logger.info {"$id: start terminating lifetime"}

                val sendBufferStopped = sendBuffer.stop(timeout)
                logger.debug{"$id: send buffer stopped, success: $sendBufferStopped"}

                synchronized(lock) {
                    logger.debug{"$id: closing socket"}
                    catch {socket?.close()}
                    lock.notifyAll()
                }

                logger.debug{"$id: waiting for receiver thread"}
                thread.join()
                logger.info{"$id: termination finished"}
            }

        }
    }


    class Server(lifetime : Lifetime, scheduler: IScheduler, port : Int?, optId: String? = null) : Base(optId ?:"ServerSocket", lifetime, scheduler) {
        val port : Int

        init {
            val ss = ServerSocket(port?:0, 0, InetAddress.getByName("127.0.0.1"))
            this.port = ss.localPort

            var socket : Socket? = null
            val thread = thread(name = id, isDaemon = true) {
                try {
                    val s = ss.accept() //could be terminated by close
                    s.tcpNoDelay = true

                    synchronized(lock) {
                        if (lifetime.isTerminated)
                            catch {s.close()}
                        else
                            socket = s
                    }

                    socketProvider.set(s)
                } catch (ex: SocketException) {
                    logger.info {"$id closed with exception: $ex"}
                }
            }


            lifetime += {
                logger.info {"$id: start terminating lifetime" }

                val sendBufferStopped = sendBuffer.stop(timeout)
                logger.debug {"$id: send buffer stopped, success: $sendBufferStopped"}

                catch {
                    logger.debug {"$id: closing server socket"}
                    ss.close()
                }
                catch {
                    synchronized(lock) {
                        logger.debug {"$id: closing socket"}
                        socket?.close()
                    }
                }

                logger.debug {"$id: waiting for receiver thread"}
                thread.join()
                logger.info{"$id: termination finished"}

            }
        }
    }

}

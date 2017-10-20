package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.WireBase
import com.jetbrains.rider.util.catch
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IScheduler
import com.jetbrains.rider.util.reactive.Trigger
import com.jetbrains.rider.util.reactive.set
import com.jetbrains.rider.util.threading.*
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.io.*
import java.net.*
import java.time.Duration
import kotlin.concurrent.thread

class SocketWire {
    companion object {
        val timeout: Duration = Duration.ofMillis(500)
    }

    abstract class Base protected constructor(val id: String, private val lifetime: Lifetime, scheduler: IScheduler) : WireBase(scheduler) {

        protected val logger: Log = LogFactory.getLog(javaClass)
        protected val socketProvider = Trigger<Socket>()

        private lateinit var output : OutputStream
        private lateinit var input : InputStream

        protected val sendBuffer = ByteBufferAsyncProcessor(id+"-AsyncSendProcessor") { send0(it) }

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
                        logger.debug("Stop receive messages because socket disconnected")
                        sendBuffer.terminate()
                        break
                    }

                    val bytes = input.readByteArray()
                    assert(bytes.size >= 4)
                    val stream = ByteArrayInputStream(bytes)
                    val id = RdId.read(stream)
                    messageBroker.dispatch(id, stream)

                } catch (ex: Exception) {
                    when (ex) {
                        is SocketException, is EOFException -> logger.debug("Exception in SocketWire.Receive:  $id: $ex")
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


        override fun send(id: RdId, writer: (OutputStream) -> Unit) {

            //todo suboptimal, because:1) creates BAOS each time, 2) expand interal array several times 3) copy BAOS internal array by .toByteArray() 4) BAOS is synchrpnized
            val stream = ByteArrayOutputStream()
            stream.writeInt(0) //placeholder for length

            val ptr = stream.size()

            id.write(stream) //write id
            writer(stream) //write rest

            val len = stream.size() - ptr

            val data = stream.toByteArray()
            len.writeIntoByteArray(data)

            sendBuffer.put(data)
        }
    }


    class Client(lifetime : Lifetime, protocol: Protocol, port : Int, optId: String? = null) : Base(optId ?:"ClientSocket", lifetime, protocol.scheduler) {

        init {

            var socket : Socket? = null
            val thread = thread(name = id, isDaemon = true) {
                try {
                    while (!lifetime.isTerminated) {
                        try {
                            val s = Socket()
                            s.tcpNoDelay = true
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
                    logger.info("$id: closed with exception: $ex")
                }
            }


            lifetime += {
                logger.info("$id: start terminating lifetime")

                val sendBufferStopped = sendBuffer.stop(timeout)
                logger.debug("$id: send buffer stopped, success: $sendBufferStopped")

                synchronized(lock) {
                    logger.debug("$id: closing socket")
                    catch {socket?.close()}
                    lock.notifyAll()
                }

                logger.debug("$id: waiting for receiver thread")
                thread.join()
                logger.info("$id: termination finished")
            }

        }
    }


    class Server(lifetime : Lifetime, protocol: IProtocol, port : Int?, optId: String? = null) : Base(optId ?:"ServerSocket", lifetime, protocol.scheduler) {
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
                    logger.info("$id closed with exception: $ex")
                }
            }

            lifetime += {
                logger.info("$id: start terminating lifetime")

                val sendBufferStopped = sendBuffer.stop(timeout)
                logger.debug("$id: send buffer stopped, success: $sendBufferStopped")

                catch {
                    logger.debug("$id: closing server socket")
                    ss.close()
                }
                catch {
                    synchronized(lock) {
                        logger.debug("$id: closing socket")
                        socket?.close()
                    }
                }

                logger.debug("$id: waiting for receiver thread")
                thread.join()
                logger.info("$id: termination finished")

            }
        }
    }

}

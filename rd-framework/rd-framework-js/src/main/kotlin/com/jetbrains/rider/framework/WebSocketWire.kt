package com.jetbrains.rider.framework

import com.jetbrains.rider.framework.base.WireBase
import com.jetbrains.rider.util.*
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.plusAssign
import com.jetbrains.rider.util.reactive.IScheduler
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

class WebSocketWire {

    class Client(lifetime: Lifetime, scheduler: IScheduler, port: Int, optId: String? = "ClientSocket") : WireBase(scheduler) {
        private val logger: Logger = getLogger(this::class)
        private var socket: WebSocket? = null
        var bytesReceived = 0
        var bytesSend = 0

        init {
            try {
                logger.log(LogLevel.Info, {"creating websocket"})
                val s = WebSocket("ws://127.0.0.1:$port")
                s.binaryType = BinaryType.ARRAYBUFFER
                s.onerror = {
                    logger.log(LogLevel.Error, it, null)
                    Unit
                }
                s.onclose = {
                    logger.log(LogLevel.Info, { "onclose"})
                    socket = null
                    Unit
                }
                s.onopen = {
                    logger.log(LogLevel.Info, { "onopen"})
                    socket = s
                    Unit
                }

                s.onmessage = {
                    try {
                        val messageEvent = it as MessageEvent
                        val data = messageEvent.data as ArrayBuffer
                        bytesReceived += data.byteLength
                        val buffer = JsBuffer(data)
                        val messageLen = buffer.readInt()
                        if (messageLen != data.byteLength - 4)
                            throw Exception("Message is corrupted")
                        val id = RdId.read(buffer)
                        messageBroker.dispatch(id, buffer)
                    } catch (ex: Throwable) {
                        logger.error("$optId caught processing", ex)
                    }
                    Unit
                }

                lifetime += {
                    logger.info { "$optId: start terminating lifetime" }
                    catch { socket?.close() }
                }
            } catch (ex: Throwable) {
                logger.error(ex)
            }
        }

        override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
            if (socket == null)
            {
                logger.log(LogLevel.Warn, {"send is failed, no connection established yet"})
                return
            }

            require(!id.isNull) { "id mustn't be null" }

            val unsafeBuffer = JsBuffer(ArrayBuffer(4096))

            try {
                unsafeBuffer.writeInt(0) //placeholder for length

                id.write(unsafeBuffer) //write id
                writer(unsafeBuffer) //write rest

                val length = unsafeBuffer.position

                unsafeBuffer.rewind()
                unsafeBuffer.writeInt(length - 4)
                socket?.send(unsafeBuffer.getFirstBytes(length))
                bytesSend += length
            } catch (ex: Throwable) {
                logger.error("$id caught processing", ex)
            }
        }
    }
}
package com.jetbrains.rd.framework

import com.jetbrains.rd.framework.base.WireBase
import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.plusAssign
import com.jetbrains.rd.util.reactive.IScheduler
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

class WebSocketWire {

    class Client(lifetime: Lifetime, scheduler: IScheduler, port: Int, private val optId: String? = "ClientSocket")
        : WireBase(scheduler)
    {
        private val logger: Logger = getLogger(this::class)
        private var socket: WebSocket? = null
        private val initialBufferSize = 4096
        var bytesReceived = 0
        var bytesSend = 0

        init {
            try {
                logger.info { "$optId init" }
                val s = WebSocket("ws://127.0.0.1:$port")
                s.binaryType = BinaryType.ARRAYBUFFER
                s.onerror = {
                    logger.error { it }
                    Unit
                }
                s.onclose = {
                    logger.info { "$optId onclose" }
                    socket = null
                    Unit
                }
                s.onopen = {
                    logger.info { "$optId onopen" }
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
                        logger.error("$optId onmessage exception", ex)
                    }
                    Unit
                }

                lifetime += {
                    logger.info { "$optId start terminating lifetime" }
                    catch { socket?.close() }
                }
            } catch (ex: Throwable) {
                logger.error("$optId init exception", ex)
            }
        }

        override fun send(id: RdId, writer: (AbstractBuffer) -> Unit) {
            if (socket == null) {
                logger.warn { "$optId send is failed, no connection established yet" }
                return
            }

            require(!id.isNull) { "id mustn't be null" }

            val unsafeBuffer = JsBuffer(ArrayBuffer(initialBufferSize))

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
                logger.error("$optId send $id exception", ex)
            }
        }
    }
}
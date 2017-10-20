//package com.jetbrains.rider.framework.base
//
//import com.jetbrains.rider.framework.*
//import com.jetbrains.rider.util.getPlatformIndependentHash
//import com.jetbrains.rider.util.lifetime.Lifetime
//import com.jetbrains.rider.util.reactive.Trigger
//import com.jetbrains.rider.util.reactive.adviseOnce
//import com.jetbrains.rider.util.trace
//import java.io.ByteArrayOutputStream
//import java.io.OutputStream
//import java.util.*
//
//abstract class RdExt(lifetime: Lifetime, protocol: IProtocol) : RdReactiveBase() {
//    override val wire : DeferredSendWire by lazy { DeferredSendWire(super.wire as WireBase) } //todo deal with serializationCtx in Wire
//
//    init {
//        //todo !(parent as IRdBindable).isBound
//        id = RdId(IdKind.StaticEntity, javaClass.simpleName.getPlatformIndependentHash())
//        identify(Identities(IdKind.StaticEntity))
//        bind(lifetime, protocol, javaClass.simpleName)
//
//        logSend.trace { "1st isReady: $name" }
//        val def = Lifetime.Eternal.createNestedDef()
//        protocol.wire.send(id) { _, _ ->  }
//        protocol.wire.advise(def.lifetime, id) {_, _ ->
//            logReceived.trace { "isReady: $name" }
//            logSend.trace { "2nd isReady: $name" }
//            protocol.wire.send(id) {_, _ ->}
//            def.terminate()
//
//            wire.counterpartReady.value = Unit
//        }
//    }
//}
//
//
//
//class DeferredSendWire(private val parentWire : WireBase) : IWire by parentWire {
//    val counterpartReady = Trigger<Unit>()
//    private val sendQueue = LinkedList<Pair<RdId, ByteArray>>()
//
//    init {
//        counterpartReady.adviseOnce(Lifetime.Eternal) {
//            synchronized(sendQueue) {
//                for ((id, payload) in sendQueue) {
//                    parentWire.send(id) { _, out -> out.write(payload)}
//                }
//                sendQueue.clear()
//            }
//        }
//    }
//
//    override fun send(id: RdId, writer: (SerializationCtx, OutputStream) -> Unit) {
//        if (counterpartReady.maybe.hasValue) {
//            synchronized(sendQueue) {
//                if (!sendQueue.isEmpty()) return enq(id, writer)
//            }
//            parentWire.send(id, writer)
//        } else {
//            synchronized(sendQueue) {
//                enq(id, writer)
//            }
//        }
//    }
//
//    private fun enq(id: RdId, writer: (SerializationCtx, OutputStream) -> Unit) {
//        val baos = ByteArrayOutputStream() //todo suboptiomal
//        writer(parentWire.serializationCtx, baos)
//        sendQueue.add(id to baos.toByteArray())
//    }
//}
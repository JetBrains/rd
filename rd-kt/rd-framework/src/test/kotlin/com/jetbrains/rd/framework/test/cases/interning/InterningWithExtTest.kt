package com.jetbrains.rd.framework.test.cases.interning

import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.collections.QueueImpl
import com.jetbrains.rd.util.reactive.IOptProperty
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class InterningWithExtTest : RdFrameworkTestBase() {

    override val clientScheduler: IScheduler
        get() = TestSchedulerWithQueueSemantics
    override val serverScheduler: IScheduler
        get() = TestSchedulerWithQueueSemantics

    @Test
    fun testLocalIntern() = doTest(false) { it.internedLocally }

    @Test
    fun testExternalIntern() = doTest(false) { it.internedExternally }

    @Test
    fun testProtocolIntern() = doTest(false) { it.internedInProtocol }


    @Test
    @Disabled
    fun testLocalInternLateConnect() = doTest(true) { it.internedLocally }

    @Test
    @Disabled
    fun testExternalInternLateConnect() = doTest(true) { it.internedExternally }

    @Test
    @Disabled
    fun testProtocolInternLateConnect() = doTest(true) { it.internedInProtocol }

    private fun doTest(delayClientInit: Boolean, propertyGetter: (InterningExtRootModel) -> IOptProperty<String>) {
        val serverProperty = RdOptionalProperty(InterningExtensionHolder).static(1).slave()
        val clientProperty = RdOptionalProperty(InterningExtensionHolder).static(1)

        serverProtocol.bindStatic(serverProperty, "top")
        clientProtocol.bindStatic(clientProperty, "top")

        val serverModel = InterningExtensionHolder()
        serverProperty.set(serverModel)

        serverModel.interningExt

        val clientModel = clientProperty.valueOrThrow
        if (!delayClientInit)
            clientModel.interningExt

        val serverInExtModel = InterningExtRootModel()
        serverModel.interningExt.root.set(serverInExtModel)

        val stringA = "a".repeat(100)
        val stringB = "b".repeat(100)

        val firstSend = measureBytes(serverProtocol) {
            propertyGetter(serverInExtModel).set(stringA)
            if (!delayClientInit)
                assert(propertyGetter(clientModel.interningExt.root.valueOrThrow).valueOrThrow == stringA)
        }

        val secondSend = measureBytes(serverProtocol) {
            propertyGetter(serverInExtModel).set(stringB)
            if (!delayClientInit)
                assert(propertyGetter(clientModel.interningExt.root.valueOrThrow).valueOrThrow == stringB)
        }

        val thirdSend = measureBytes(serverProtocol) {
            propertyGetter(serverInExtModel).set(stringA)
            if (!delayClientInit)
                assert(propertyGetter(clientModel.interningExt.root.valueOrThrow).valueOrThrow == stringA)
        }

        if (delayClientInit) {
            clientModel.interningExt
            clientWire.processAllMessages()
            serverWire.processAllMessages()
            clientWire.processAllMessages()
            assert(propertyGetter(clientModel.interningExt.root.valueOrThrow).valueOrThrow == stringA)
        }

        assert(firstSend == secondSend)
        assert(thirdSend < firstSend)
        println("First: $firstSend, second: $secondSend, third: $thirdSend")
    }

    private fun measureBytes(protocol: IProtocol, action: () -> Unit): Long {
        val pre = (protocol.wire as TestWire).bytesWritten
        action()
        return (protocol.wire as TestWire).bytesWritten - pre
    }
}

object TestSchedulerWithQueueSemantics: IScheduler {
    val queue = QueueImpl<() -> Unit>()

    override fun queue(action: () -> Unit) {
        queue.offer(action)
    }

    override fun invokeOrQueue(action: () -> Unit) {
        action()
        flush()
    }

    override val isActive: Boolean
        get() = true

    override fun flush() {
        while(true) (queue.poll() ?: return).invoke()
    }
}
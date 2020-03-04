package com.jetbrains.rd.framework.test.cases.interning

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.InternRoot
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.framework.test.util.TestWire
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InterningTest: RdFrameworkTestBase() {
    private val simpleTestData: List<Pair<Int, String>>
        get() {
            return listOf(
                0 to "",
                1 to "test",
                2 to "why"
            )
        }

    @Test
    fun testServerToClient() = doTest(false, false)

    @Test
    fun testClientToServer() = doTest(true, true)

    @Test
    fun testServerThenClientMixed() = doTest(false, true)

    @Test
    fun testClientThenServerMixed() = doTest(true, false)

    @Test
    fun testServerThenClientMixedAndReversed() = doTest(false, true, true)

    @Test
    fun testClientThenServerMixedAndReversed() = doTest(true, false, true)



    @Test
    fun testServerToClientProtocolLevel() = testProtocolLevelIntern(false, false)

    @Test
    fun testClientToServerProtocolLevel() = testProtocolLevelIntern(true, true)

    @Test
    fun testServerThenClientMixedProtocolLevel() = testProtocolLevelIntern(false, true)

    @Test
    fun testClientThenServerMixedProtocolLevel() = testProtocolLevelIntern(true, false)

    @Test
    fun testServerThenClientMixedAndReversedProtocolLevel() = testProtocolLevelIntern(false, true, true)

    @Test
    fun testClientThenServerMixedAndReversedProtocolLevel() = testProtocolLevelIntern(true, false, true)



    private fun measureBytes(protocol: IProtocol, action: () -> Unit): Long {
        val pre = (protocol.wire as TestWire).bytesWritten
        action()
        return (protocol.wire as TestWire).bytesWritten - pre
    }

    fun doTest(firstClient: Boolean, secondClient: Boolean, thenSwitchSides: Boolean = false) {
        val serverProperty = RdOptionalProperty(InterningTestModel).static(1).slave()
        val clientProperty = RdOptionalProperty(InterningTestModel).static(1)

        serverProtocol.bindStatic(serverProperty, "top")
        clientProtocol.bindStatic(clientProperty, "top")

        val serverModel = InterningTestModel("")

        serverProperty.set(serverModel)
        val clientModel = clientProperty.valueOrThrow

        val simpleTestData = simpleTestData

        val firstSenderProtocol = if(firstClient) clientProtocol else serverProtocol
        val firstSenderModel = if(firstClient) clientModel else serverModel

        val firstBytesWritten = measureBytes(firstSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                firstSenderModel.issues[k] = WrappedStringModel(v)
            }
        }

        val secondSenderProtocol = if(secondClient) clientProtocol else serverProtocol
        val secondSenderModel = if(secondClient) clientModel else serverModel

        val secondBytesWritten = measureBytes(secondSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                secondSenderModel.issues[k + simpleTestData.size] = WrappedStringModel(v)
            }
        }

        assertTrue(firstBytesWritten - simpleTestData.sumBy { it.second.length } >= secondBytesWritten)

        val firstReceiver = if(firstClient) serverModel else clientModel
        val secondReceiver = if(secondClient) serverModel else clientModel

        simpleTestData.forEach { (k, v) ->
            assertEquals(v, firstReceiver.issues[k]!!.text)
            assertEquals(v, secondReceiver.issues[k + simpleTestData.size]!!.text)
        }

        if (!thenSwitchSides) return

        val extraString = "again"

        val thirdBytesWritten = measureBytes(secondSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                secondSenderModel.issues[k + simpleTestData.size * 2] = WrappedStringModel(v + extraString)
            }
        }


        val fourthBytesWritten = measureBytes(firstSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                firstSenderModel.issues[k + simpleTestData.size * 3] = WrappedStringModel(v + extraString)
            }
        }

        assertTrue(thirdBytesWritten - simpleTestData.sumBy { it.second.length + extraString.length } >= fourthBytesWritten)

        simpleTestData.forEach { (k, v) ->
            assertEquals(v + extraString, secondReceiver.issues[k + simpleTestData.size * 2]!!.text)
            assertEquals(v + extraString, firstReceiver.issues[k + simpleTestData.size * 3]!!.text)
        }
    }

    @Test
    fun testNestedInternedObjects() {
        serverProtocol.serializers.register(InterningNestedTestModel)
        clientProtocol.serializers.register(InterningNestedTestModel)

        val serverPropertyHolder = PropertyHolderWithInternRoot(RdOptionalProperty(InterningNestedTestModel.interned("TestInternScope")).static(1).slave(), serverProtocol.serializationContext)
        val clientPropertyHolder = PropertyHolderWithInternRoot(RdOptionalProperty(InterningNestedTestModel.interned("TestInternScope")).static(1), clientProtocol.serializationContext)

        serverPropertyHolder.mySerializationContext = serverPropertyHolder.mySerializationContext.withInternRootsHere(serverPropertyHolder, "TestInternScope")
        clientPropertyHolder.mySerializationContext = clientPropertyHolder.mySerializationContext.withInternRootsHere(clientPropertyHolder, "TestInternScope")

        val serverProperty = serverPropertyHolder.property
        val clientProperty = clientPropertyHolder.property

        serverProtocol.bindStatic(serverPropertyHolder, "top")
        clientProtocol.bindStatic(clientPropertyHolder, "top")

        val testValue = InterningNestedTestModel("extremelyLongString",
                InterningNestedTestModel("middle",
                        InterningNestedTestModel("bottom", null)
                )
        )

        val firstSendBytes = measureBytes(serverProtocol) {
            serverProperty.set(testValue)
            assertEquals(testValue, clientProperty.valueOrThrow, "Received value should be the same as sent one")
        }

        val secondSendBytes = measureBytes(serverProtocol) {
            serverProperty.set(testValue.inner!!)
            assertEquals(testValue.inner, clientProperty.valueOrThrow, "Received value should be the same as sent one")
        }

        val thirdSendBytes = measureBytes(serverProtocol) {
            serverProperty.set(testValue)
            assertEquals(testValue, clientProperty.valueOrThrow, "Received value should be the same as sent one")
        }

        fun sumLengths(value: InterningNestedTestModel): Int {
            return value.value.length * 2 + 4 + (value.inner?.let { sumLengths(it) } ?: 0)
        }

        assertEquals(secondSendBytes, thirdSendBytes, "Sending a single interned object should take the same amount of bytes")
        assertTrue(thirdSendBytes <= firstSendBytes - sumLengths(testValue))
    }

    private fun testProtocolLevelIntern(firstClient: Boolean, secondClient: Boolean, thenSwitchSides: Boolean = false) {
        val serverProperty = RdOptionalProperty(InterningProtocolLevelModel).static(1).slave()
        val clientProperty = RdOptionalProperty(InterningProtocolLevelModel).static(1)

        serverProtocol.bindStatic(serverProperty, "top")
        clientProtocol.bindStatic(clientProperty, "top")

        val serverModel = InterningProtocolLevelModel("")

        serverProperty.set(serverModel)
        val clientModel = clientProperty.valueOrThrow

        val simpleTestData = simpleTestData

        val firstSenderProtocol = if(firstClient) clientProtocol else serverProtocol
        val firstSenderModel = if(firstClient) clientModel else serverModel

        val firstBytesWritten = measureBytes(firstSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                firstSenderModel.issues[k] = ProtocolWrappedStringModel(v)
            }
        }

        val secondSenderProtocol = if(secondClient) clientProtocol else serverProtocol
        val secondSenderModel = if(secondClient) clientModel else serverModel

        val secondBytesWritten = measureBytes(secondSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                secondSenderModel.issues[k + simpleTestData.size] = ProtocolWrappedStringModel(v)
            }
        }

        assertTrue(firstBytesWritten - simpleTestData.sumBy { it.second.length } >= secondBytesWritten)

        val firstReceiver = if(firstClient) serverModel else clientModel
        val secondReceiver = if(secondClient) serverModel else clientModel

        simpleTestData.forEach { (k, v) ->
            assertEquals(v, firstReceiver.issues[k]!!.text)
            assertEquals(v, secondReceiver.issues[k + simpleTestData.size]!!.text)
        }

        if (!thenSwitchSides) return

        val extraString = "again"

        val thirdBytesWritten = measureBytes(secondSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                secondSenderModel.issues[k + simpleTestData.size * 2] = ProtocolWrappedStringModel(v + extraString)
            }
        }


        val fourthBytesWritten = measureBytes(firstSenderProtocol) {
            simpleTestData.forEach { (k, v) ->
                firstSenderModel.issues[k + simpleTestData.size * 3] = ProtocolWrappedStringModel(v + extraString)
            }
        }

        assertTrue(thirdBytesWritten - simpleTestData.sumBy { it.second.length + extraString.length } >= fourthBytesWritten)

        simpleTestData.forEach { (k, v) ->
            assertEquals(v + extraString, secondReceiver.issues[k + simpleTestData.size * 2]!!.text)
            assertEquals(v + extraString, firstReceiver.issues[k + simpleTestData.size * 3]!!.text)
        }
    }

    @Test
    fun testNestedInternedObjectsOnSameData() {
        serverProtocol.serializers.register(InterningNestedTestModel)
        clientProtocol.serializers.register(InterningNestedTestModel)

        val serverPropertyHolder = PropertyHolderWithInternRoot(RdOptionalProperty(InterningNestedTestStringModel).static(1).slave(), serverProtocol.serializationContext)
        val clientPropertyHolder = PropertyHolderWithInternRoot(RdOptionalProperty(InterningNestedTestStringModel).static(1), clientProtocol.serializationContext)

        serverPropertyHolder.mySerializationContext = serverPropertyHolder.mySerializationContext.withInternRootsHere(serverPropertyHolder, "TestInternScope")
        clientPropertyHolder.mySerializationContext = clientPropertyHolder.mySerializationContext.withInternRootsHere(clientPropertyHolder, "TestInternScope")

        val serverProperty = serverPropertyHolder.property
        val clientProperty = clientPropertyHolder.property

        serverProtocol.bindStatic(serverPropertyHolder, "top")
        clientProtocol.bindStatic(clientPropertyHolder, "top")

        val sameString = "thisStringHasANiceLengthThatWillDominateBytesSentCount"

        val testValue = InterningNestedTestStringModel(sameString,
                InterningNestedTestStringModel(sameString,
                        InterningNestedTestStringModel(sameString, null)
                )
        )

        val firstSendBytes = measureBytes(serverProtocol) {
            serverProperty.set(testValue)
            assertEquals(testValue, clientProperty.valueOrThrow, "Received value should be the same as sent one")
        }

        // expected send: string + 4 bytes length + 4 bytes id + 8+4 bytes polymorphic write, 3 bytes nullability, 3x 4byte ids, 4 bytes property version, 1 byte msg type
        // expected send confirmation: 4 bytes id, 1 byte msg type, 4 bytes id
        val sendTarget = sameString.length * 2 + 4 + 4 + 8 + 4 + 3 + 4 * 3 + 4 + 12
        assert(firstSendBytes <= sendTarget) { "Sent $firstSendBytes, expected $sendTarget" }
    }

    @Test
    fun testLateBindOfObjectWithContent() {
        val serverProperty = RdOptionalProperty(InterningTestModel).slave()
        val clientProperty = RdOptionalProperty(InterningTestModel)

        serverProperty.identify(serverProtocol.identity, RdId(1L))
        clientProperty.identify(clientProtocol.identity, RdId(1L))

        serverProtocol.bindStatic(serverProperty, "top")
        clientProtocol.bindStatic(clientProperty, "top")

        val serverModel = InterningTestModel("")

        val simpleTestData = simpleTestData

        simpleTestData.forEach { (k, v) ->
            serverModel.issues[k] = WrappedStringModel(v)
        }

        serverProperty.set(serverModel)

        val clientModel = clientProperty.valueOrThrow

        simpleTestData.forEach { (k, v) ->
            assertEquals(v, clientModel.issues[k]!!.text)
        }
    }

    @Test
    fun testMonomorphic() {
        val rootServerMono = InternRoot<Long>(FrameworkMarshallers.Long).bindStatic(serverProtocol,"top1")
        val rootServerPoly = InternRoot<Long>().bindStatic(serverProtocol, "top2")

        val monoSentBytes = measureBytes(serverProtocol) { rootServerMono.intern(0) }
        val polySentBytes = measureBytes(serverProtocol) { rootServerPoly.intern(0) }

        assertEquals(2 + 8 + 4, monoSentBytes)
        assertEquals(2 + 8 + 4 + 8 + 4, polySentBytes)
    }


    private fun <T : Any> InternRoot<T>.bindStatic(protocol: IProtocol, id: String) : InternRoot<T> {
        identify(protocol.identity, RdId.Null.mix(id))
        bind(if(protocol === clientProtocol) clientLifetime else serverLifetime, protocol, id)
        return this
    }
}
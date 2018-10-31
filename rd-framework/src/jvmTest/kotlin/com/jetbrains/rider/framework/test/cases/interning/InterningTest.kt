package com.jetbrains.rider.framework.test.cases.interning

import com.jetbrains.rider.framework.IProtocol
import com.jetbrains.rider.framework.base.static
import com.jetbrains.rider.framework.impl.RdOptionalProperty
import com.jetbrains.rider.framework.interned
import com.jetbrains.rider.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rider.framework.test.util.TestWire
import com.jetbrains.rider.framework.withInternRootHere
import com.jetbrains.rider.util.reactive.valueOrThrow
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val serverPropertyHolder = PropertyHolderWithInternRoot(RdOptionalProperty(InterningNestedTestModel.interned()).static(1).slave(), serverProtocol.serializationContext.withInternRootHere(true))
        val clientPropertyHolder = PropertyHolderWithInternRoot(RdOptionalProperty(InterningNestedTestModel.interned()).static(1), clientProtocol.serializationContext.withInternRootHere(false))

        val serverProperty = serverPropertyHolder.property
        val clientProperty = clientPropertyHolder.property

        serverProtocol.bindStatic(serverPropertyHolder, "top")
        clientProtocol.bindStatic(clientPropertyHolder, "top")

        val testValue = InterningNestedTestModel("extremelyLongString", InterningNestedTestModel("middle", InterningNestedTestModel("bottom", null)))

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
}
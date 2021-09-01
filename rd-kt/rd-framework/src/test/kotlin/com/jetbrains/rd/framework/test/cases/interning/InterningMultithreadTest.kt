package com.jetbrains.rd.framework.test.cases.interning

import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.test.util.RdAsyncTestBase
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.CountDownLatch
import java.util.stream.Stream

class InterningMultithreadTest : RdAsyncTestBase() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun doTest_Args(): Stream<Arguments> = Stream.of(
            Arguments.of(1, true),
            Arguments.of(100, true),
            Arguments.of(10_000, true),
            Arguments.of(1, false),
            Arguments.of(100, false),
            Arguments.of(10_000, false)
        )
    }

    @ParameterizedTest
    @MethodSource("doTest_Args")
    fun doTest(typicalStringLength: Int, attemptContention: Boolean) {
        println("Length: $typicalStringLength, contention: $attemptContention")
        val evt0 = CountDownLatch(2)
        val evt1 = CountDownLatch(3)
        val evt2 = CountDownLatch(1)
        val evt3 = CountDownLatch(1)

        val iterationCount = 10_000
        val uniqueValues = 100

        var clientBytesBase = 0L
        val uiStringBase = if(attemptContention) "b" else "u"
        val uniqueValuesMultiplier = if(attemptContention) 1 else 2
        val maxBytesMultiplier = if(attemptContention) 2  else 1

        clientUiScheduler.queue {
            val clientModel = clientProtocol.bindStatic(RdOptionalProperty(InterningMtModel).static(1), "top")

            evt0.countDown()
            evt0.await()

            clientModel.set(InterningMtModel(""))

            evt1.countDown()
            evt1.await()

            clientUiScheduler.queue {
                clientBytesBase = clientWire.bytesWritten
                evt2.await()
                clientBgScheduler.queue {
                    for (i in 0 until iterationCount) {
                        clientModel.valueOrThrow.signaller.fire("b".repeat(typicalStringLength) + i % uniqueValues)
                    }
                }
                for (i in 0 until iterationCount) {
                    clientModel.valueOrThrow.signaller.fire(uiStringBase.repeat(typicalStringLength) + i % uniqueValues)
                }
            }

            evt3.countDown()
        }

        val seenValuesBg = HashSet<String>()
        val seenValuesUi = HashSet<String>()

        serverUiScheduler.queue {
            val serverModel = serverProtocol.bindStatic(RdOptionalProperty(InterningMtModel).static(1), "top")

            evt0.countDown()
            evt0.await()

            serverModel.view(serverLifetime) { plt, m ->
                m.signaller.adviseOn(plt, serverBgScheduler) {
                    seenValuesBg.add(it)
                }
                m.signaller.adviseOn(plt, serverUiScheduler) {
                    seenValuesUi.add(it)
                }
                evt2.countDown()
            }
            evt1.countDown()
            evt1.await()
        }

        evt1.countDown()
        evt1.await()

        evt3.await()

//        serverBgScheduler.

        clientUiScheduler.assertNoExceptions()
        clientBgScheduler.assertNoExceptions()


        serverUiScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()

        assert(seenValuesBg == seenValuesUi) { "Two receiving threads have seen different values, bug?" }
        assert(seenValuesBg.size == uniqueValues * uniqueValuesMultiplier) { "Some values were not received" }

        val uniqueValuesStringLengthSum = (uniqueValues * typicalStringLength + (0 until uniqueValues).sumBy { it.toString().length }) * uniqueValuesMultiplier

        // raw bytes: strings, string headers (4), context headers (2)
        val rawBytesExpected = uniqueValuesStringLengthSum.toLong() * 2 * iterationCount / uniqueValues + 4 * iterationCount * 2 + iterationCount * 2 * 2
        // actual expected value calc:
        // rdid of entity doesn't count
        // per signal firing: 4 bytes interned value, 2 bytes context
        // per unique value: 4 bytes string length + string + 4 bytes id + 8 + 4 bytes polymorphic write + 2 bytes context
        val actualBytesExpected = uniqueValuesStringLengthSum * 2 + uniqueValuesMultiplier * uniqueValues * (4 + 4 + 8 + 4 + 2) + 2 * iterationCount * (4 + 2)

        val interningBytesWritten = clientWire.bytesWritten - clientBytesBase

        assert(interningBytesWritten <= actualBytesExpected * 120 * maxBytesMultiplier / 100) { "Interning should save data, sent ${interningBytesWritten}, expected interned $actualBytesExpected, expected raw $rawBytesExpected" }
        println("Sent ${interningBytesWritten}, expected interned $actualBytesExpected, expected raw $rawBytesExpected")
        println("Interning mt contention: ${interningBytesWritten.toFloat()/actualBytesExpected}")
        println("Interning ratio (more=better): ${rawBytesExpected/interningBytesWritten.toFloat()}")
    }

    @Test
    fun testLateReceiverBind() {
        val evt0 = CountDownLatch(1)
        val evt1 = CountDownLatch(3)
        val evt2 = CountDownLatch(1)
        val evt3 = CountDownLatch(1)
        val evt4 = CountDownLatch(1)

        val iterationCount = 10_000

        clientUiScheduler.queue {
            val clientModel = clientProtocol.bindStatic(RdOptionalProperty(InterningMtModel).static(1), "top")

            evt0.await()
            clientModel.set(InterningMtModel(""))

            evt1.countDown()
            evt1.await()

            clientUiScheduler.queue {
                evt2.await()
                clientBgScheduler.queue {
                    for (i in 0 until iterationCount) {
                        clientModel.valueOrThrow.signaller.fire("b$i")
                    }
                }
                clientBgScheduler.queue {
                    evt3.await()
                    for (i in 0 until iterationCount) {
                        clientModel.valueOrThrow.signaller2.fire("b$i")
                    }
                    evt4.countDown()
                }
                for (i in 0 until iterationCount) {
                    clientModel.valueOrThrow.signaller.fire("u$i")
                }
            }


        }

        val seenValuesBg = HashSet<String>()
        val seenValuesBg2 = HashSet<String>()

        serverUiScheduler.queue {
            evt0.countDown()
            evt1.countDown()
            evt1.await()

            evt2.countDown()

            val serverModel = RdOptionalProperty(InterningMtModel).static(1)

            serverModel.view(serverLifetime) { plt, m ->
                m.signaller.adviseOn(plt, serverBgScheduler) {
                    seenValuesBg.add(it)
                }
                m.signaller2.adviseOn(plt, serverBgScheduler) {
                    seenValuesBg2.add(it)
                }
                evt3.countDown()
                evt4.await()
            }

            Thread.sleep(500)

            serverProtocol.bindStatic(serverModel, "top")
        }

        evt1.countDown()
        evt1.await()

        evt4.await()

        // these also flush the schedulers
        clientUiScheduler.assertNoExceptions()
        clientBgScheduler.assertNoExceptions()


        serverUiScheduler.assertNoExceptions()
        serverBgScheduler.assertNoExceptions()

        assert(seenValuesBg.size == iterationCount * 2) { "Some values were not received" }
        assert(seenValuesBg2.size == iterationCount) { "Some BG values were not received" }
    }
}

package com.jetbrains.rd.framework.test.cases.extensions

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.test.util.RdFrameworkTestBase
import com.jetbrains.rd.util.AtomicInteger
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isNotAlive
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.asRdScheduler
import com.jetbrains.rd.util.threading.coroutines.asCoroutineDispatcher
import com.jetbrains.rd.util.threading.coroutines.launch
import demo.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstantExtTest : RdFrameworkTestBase() {
    override val clientScheduler: IScheduler = createSingleThreadScheduler()
    override val serverScheduler: IScheduler = createSingleThreadScheduler()

    override val clientWireScheduler: IScheduler = createSingleThreadScheduler()
    override val serverWireScheduler: IScheduler = createSingleThreadScheduler()

    private fun createSingleThreadScheduler() = Dispatchers.IO.limitedParallelism(1).asRdScheduler

    @Test
    fun instantExtTest() {
        Lifetime.using { globalTestLifetime ->
            val testLifetimeDef = globalTestLifetime.createNested()
            val testLifetime = testLifetimeDef.lifetime

            var modelCounter = 0
            var baseExtCounter = 0

            val clientProtocol = clientProtocol as Protocol
            clientProtocol.extCreated.advise(testLifetime) {
                if (it.isLocal) return@advise

                // serverWireScheduler because of TestWire
                assertTrue(serverWireScheduler.isActive)

                if (it.info.rName.localName == InstantExtModel::class.simpleName?.decapitalize()) {
                    val parentId = it.info.rdId!!
                    val extensibleModel = clientProtocol.rdEntitiesRegistrar.tryGetDynamic(parentId) as ExtensibleModel
                    val model = extensibleModel.instantExtModel
                    assertTrue(model.connected.value, "Ext is not connected")

                    val bindLifetime = model.bindLifetime!!
                    model.checker.advise(bindLifetime) {

                        try {
                            assertTrue(clientScheduler.isActive)

                            assertEquals(modelCounter, it)
                            // check global order
                            assertEquals(modelCounter, baseExtCounter)
                            modelCounter++
                        } catch (e: Throwable) {
                            Logger.root.error(e)
                            testLifetimeDef.terminate()
                        }
                    }
                }
            }

            val serverInstantHelper = serverProtocol.instantHelperExt
            val clientInstantHelper = clientProtocol.instantHelperExt

            flushAllSchedulers()

            runBlocking(clientScheduler.asCoroutineDispatcher) {
                clientInstantHelper.checker.advise(testLifetime) {
                    try {
                        assertTrue(clientScheduler.isActive)

                        assertEquals(baseExtCounter, it)
                        baseExtCounter++
                        // check global order
                        assertEquals(modelCounter, baseExtCounter)
                    } catch (e: Throwable) {
                        Logger.root.error(e)
                        testLifetimeDef.terminate()
                    }
                }
            }

            val n = 10_000
            runBlocking(serverScheduler.asCoroutineDispatcher) {

                for (i in 0 until n) {
                    if (testLifetime.isNotAlive)
                        return@runBlocking

                    val extensibleModel = ExtensibleModel()
                    serverInstantHelper.value.set(extensibleModel)
                    val model = extensibleModel.instantExtModel
                    assertTrue(model.connected.value, "Ext is not connected")
                    model.checker.fire(i)
                    serverInstantHelper.checker.fire(i)
                }
            }

            flushAllSchedulers()

            assertEquals(n, modelCounter)
            assertEquals(n, baseExtCounter)
        }
    }
}
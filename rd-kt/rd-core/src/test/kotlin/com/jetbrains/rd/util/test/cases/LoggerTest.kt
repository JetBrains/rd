package com.jetbrains.rd.util.test.cases

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.test.framework.RdTestBase
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class LoggerTest : RdTestBase() {
    @Test
    fun manyLoggersStofl() {
        val list = mutableListOf<Logger>()
        for (i in 0..10_001)
            list.add(getLogger("category: $i"))

        list.forEach {
            it.isEnabled(LogLevel.Info)
        }

    }

    @Test
    fun updateFactoryTest() {
        val statics = Statics<ILoggerFactory>()

        val factory = createLoggerFactory()
        val logger = getLogger("")
        assert(logger.getRealLogger() !== factory.LoggerInstance)

        statics.use(factory) {
            assert(logger.getRealLogger() === factory.LoggerInstance)

            runBlocking {

                suspend fun doTest() {
                    val finish = AtomicBoolean(false)
                    val started = AtomicBoolean(false)

                    val tasks = (0 until Runtime.getRuntime().availableProcessors()).map {
                        async(Dispatchers.Default) {
                            started.set(true)

                            while (!finish.get()) {
                                ensureActive()
                                logger.getRealLogger()
                            }

                            // return realLogger when the last factory was set
                            logger.getRealLogger()
                        }
                    }

                    spinUntil { started.get() }

                    Lifetime.using { lifetime ->
                        lifetime.onTermination { finish.set(true) }

                        val n = 100
                        for (i in 0..n) {
                            val newFactory = createLoggerFactory()
                            lifetime.onTermination(statics.push(newFactory))
                            // check that logger.getRealLogger() is correct after pushing newFactory
                            assert(logger.getRealLogger() == newFactory.LoggerInstance)

                            if (i == n) {
                                finish.set(true)
                                tasks.awaitAll().forEach { realLogger ->
                                    assert(realLogger === newFactory.LoggerInstance)
                                }
                            }
                        }
                    }
                }

                for (i in 0..1000) {
                    doTest()
                }
            }
        }
    }

    private fun createLoggerFactory() = object : ILoggerFactory {
        val LoggerInstance = object : Logger {
            override fun log(level: LogLevel, message: Any?, throwable: Throwable?) = Unit
            override fun isEnabled(level: LogLevel) = false
        }

        override fun getLogger(category: String) = LoggerInstance
    }
}
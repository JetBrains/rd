package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.util.asCoroutineDispatcher
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.Statics
import com.jetbrains.rd.util.collections.SynchronizedList
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.threading.CompoundThrowable
import com.jetbrains.rd.util.threading.TestSingleThreadScheduler
import com.jetbrains.rd.util.threading.coroutines.RdCoroutineScope
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class CoroutineTestBase {
    private var def: LifetimeDefinition = LifetimeDefinition.Terminated

    protected val lifetime: Lifetime get() = def.lifetime

    protected val scheduler = TestSingleThreadScheduler("CoroutineTestScheduler")
    protected lateinit var host: TestSingleThreadCoroutineHost

    protected lateinit var logger: Logger

    @BeforeTest
    fun setup() {
        def = LifetimeDefinition()
        val factory = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory)
        def.onTermination { factory.close() }

        logger = getLogger<CoroutineTest>()

        host = TestSingleThreadCoroutineHost(scheduler)

        lifetime.onTermination { scheduler.assertNoExceptions() }
        lifetime.onTermination { host.assertNoExceptions() }

        RdCoroutineScope.override(host)
        def.onTermination {
            runBlocking {
                host.coroutineContext.job.cancelAndJoin()
            }
        }
    }

    @AfterTest
    fun teardown() {
        def.terminate()
        ErrorAccumulatorLoggerFactory.throwAndClear()
    }
}

class TestSingleThreadCoroutineHost(scheduler: TestSingleThreadScheduler) : RdCoroutineScope() {
    override val defaultContext: CoroutineContext = scheduler.asCoroutineDispatcher + SupervisorJob()

    private val exceptions = SynchronizedList<Throwable>()

    override fun onException(throwable: Throwable) {
        exceptions.add(throwable)
    }

    fun getExceptions(clear: Boolean): List<Throwable> {
        val list = exceptions.toList()
        if (clear)
            exceptions.clear()
        return list
    }

    fun assertNoExceptions() {
        val list = getExceptions(true)
        if (list.isNotEmpty()) {
            CompoundThrowable.throwIfNotEmpty(exceptions)
        }
    }
}
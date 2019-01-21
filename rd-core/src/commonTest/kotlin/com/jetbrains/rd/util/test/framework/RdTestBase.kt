package com.jetbrains.rd.util.test.framework

import com.jetbrains.rd.util.*
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.threading.Linearization
import kotlin.test.AfterTest
import kotlin.test.BeforeTest


open class RdTestBase {


    protected val l11n = Linearization()
    private val testLifetimes = SequentialLifetimes(Lifetime.Eternal)

    protected var testLifetime: Lifetime = Lifetime.Eternal
        private set

    private var loggerFactoryCookie : Closeable? = null
    protected val testLogger = getLogger<RdTestBase>()

    @BeforeTest
    fun setup() {
        l11n.reset()
        l11n.enable()

        testLifetime = testLifetimes.next()

        loggerFactoryCookie = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory)
    }

    @AfterTest
    fun teardown() {
        loggerFactoryCookie?.close()
        ErrorAccumulatorLoggerFactory.throwAndClear()

        testLifetimes.terminateCurrent()
    }

    protected inline fun withFailLog(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            testLogger.error(e)
            throw e
        }
    }
}
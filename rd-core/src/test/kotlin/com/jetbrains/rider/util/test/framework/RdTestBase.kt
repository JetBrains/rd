package com.jetbrains.rider.util.test.framework

import com.jetbrains.rider.util.Closeable
import com.jetbrains.rider.util.ILoggerFactory
import com.jetbrains.rider.util.Statics
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.SequentialLifetimes
import com.jetbrains.rider.util.log.ErrorAccumulatorLoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest


open class RdTestBase {

    private val testLifetimes = SequentialLifetimes(Lifetime.Eternal)

    protected var testLifetime: Lifetime = Lifetime.Eternal
        private set

    private var loggerFactoryCookie : Closeable? = null

    @BeforeTest
    fun setup() {
        testLifetime = testLifetimes.next()

        loggerFactoryCookie = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory)
    }

    @AfterTest
    fun teardown() {
        loggerFactoryCookie?.close()
        ErrorAccumulatorLoggerFactory.throwAndClear()

        testLifetimes.terminateCurrent()
    }
}
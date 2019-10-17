package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory.errors
import com.jetbrains.rd.util.spinUntil
import org.junit.Rule
import org.junit.rules.Timeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class TestBase {

    protected lateinit var lifetimeDef : LifetimeDefinition
    protected val lifetime : Lifetime get() = lifetimeDef.lifetime

    protected val timeoutToWaitConditionMs = 1000L
    protected fun wait(condition: () -> Boolean) {
        require (spinUntil(timeoutToWaitConditionMs) {
            SequentialPumpingScheduler.flush()
            condition()
        })
    }

    @JvmField
    @Rule
    var globalTimeout = Timeout.seconds(10)

    @BeforeTest
    fun setupLogger() {
        assert (ErrorAccumulatorLoggerFactory.errors.isEmpty()) {"${ErrorAccumulatorLoggerFactory::class.simpleName}.${::errors.name} are not clear from previous tests"}
        assert (SequentialPumpingScheduler.isEmpty) { "SequentialPumpingScheduler is not empty" }

        lifetimeDef = LifetimeDefinition()
        Logger.set(lifetime, ErrorAccumulatorLoggerFactory)
    }

    @AfterTest
    fun teardownLogger() {
        SequentialPumpingScheduler.flush()
        lifetimeDef.terminate()

        ErrorAccumulatorLoggerFactory.throwAndClear()
    }
}
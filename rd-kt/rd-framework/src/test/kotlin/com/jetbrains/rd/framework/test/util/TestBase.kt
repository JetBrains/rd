package com.jetbrains.rd.framework.test.util

import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rd.util.log.ErrorAccumulatorLoggerFactory.errors
import com.jetbrains.rd.util.spinUntil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 30, unit = TimeUnit.SECONDS)
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

    @BeforeEach
    fun setupLogger() {
        errors.clear()
        SequentialPumpingScheduler.flush()

        lifetimeDef = LifetimeDefinition()
        Logger.set(lifetime, ErrorAccumulatorLoggerFactory)
    }

    @AfterEach
    fun teardownLogger() {
        SequentialPumpingScheduler.flush()
        lifetimeDef.terminate()
        SequentialPumpingScheduler.flush()

        ErrorAccumulatorLoggerFactory.throwAndClear()
    }
}